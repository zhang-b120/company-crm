package com.qykj.finmaster.third.workWx.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.third.workWx.entity.WorkWxGroupChat;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessage;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessageKeywordHit;
import com.qykj.finmaster.third.workWx.entity.WorkWxResponseMetric;
import com.qykj.finmaster.third.workWx.manager.WorkWxGroupChatManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageKeywordHitManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxResponseMetricManager;
import com.qykj.finmaster.third.workWx.mapper.WorkWxResponseMetricMapper;
import com.qykj.finmaster.system.bizConfig.service.SysBizConfigService;
import org.apache.commons.lang3.StringUtils;
import com.qykj.finmaster.workbench.util.WorkingHoursCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.locks.ReentrantLock;
import java.util.*;

/**
 * 企业微信群维度回复/超时计算引擎（工单 76 / WXRESP-01）。
 *
 * <p>纯只读 {@code work_wx_message}，按 room_id 分组、msg_time 升序遍历做回合切分：
 * 客户(from_type=2)首条发言开回合，员工(from_type=1)首条回复关闭回合（客户连发只算一个回合、从首条起算，
 * 员工无未回合时主动发言不计）。首响时长用工作时段(WorkingHoursCalculator)判级；未回复回合 >24h 标高风险。
 * 按唯一键(room_id, first_customer_msg_id) 幂等 upsert。不关联客户、不碰业务模块。</p>
 *
 * <p>口径（原文档 §8.3）：≤5min 优秀 / ≤30 正常 / ≤120 超时 / >120 严重超时。
 * TODO：阈值/工作日历预留 sys_biz_config（参照 LeadAutoInvalidateTask 用 SysBizConfigService），当前用默认常量。</p>
 */
@Slf4j
@Service
public class WorkWxResponseMetricServiceImpl {

    // 默认阈值（工作时段分钟）
    private static final long EXCELLENT_MIN = 5;
    /** 首响超时阈值默认值（工作时段分钟，工单84/WXRESP-09：≤5 优秀 / ≤15 正常 / >15 超时）。可由配置覆盖。 */
    private static final int DEFAULT_TIMEOUT_MIN = 15;
    /** 未回复高风险阈值（自然小时） */
    private static final long UNREPLIED_HIGH_RISK_HOURS = 24;
    /** 首响超时阈值配置键（sys_biz_config，module=wecom）。 */
    private static final String CFG_TIMEOUT_MIN = "wecom.msgaudit.response_timeout_minutes";

    /** 重算锁（工单84/WXRESP-10）：入库触发的 runAsync 重算与 5 分钟定时兜底重算串行、防重入。 */
    private final ReentrantLock recomputeLock = new ReentrantLock();

    @Autowired
    private WorkWxMessageManager messageManager;
    @Autowired
    private SysBizConfigService bizConfigService;
    @Autowired
    private WorkWxResponseMetricManager metricManager;
    @Autowired
    private WorkWxResponseMetricMapper metricMapper;
    @Autowired
    private WorkWxMessageKeywordHitManager keywordHitManager;
    @Autowired
    private WorkWxGroupChatManager groupChatManager;
    /** 客服超时弹窗告警（WXRESP-12）：重算后触发。@Lazy 防潜在循环依赖。 */
    @Lazy
    @Autowired
    private WorkWxCsOvertimeService csOvertimeService;

    /**
     * 重算自 sinceTime 起的群回复指标（幂等 upsert）。
     * @return 处理汇总
     */
    public Map<String, Object> recompute(Date sinceTime) {
        Map<String, Object> result = new LinkedHashMap<>();
        // WXRESP-10：tryLock 防重入（入库 runAsync 重算 与 定时兜底重算 串行）
        if (!recomputeLock.tryLock()) {
            result.put("ok", true);
            result.put("stage", "SKIPPED_RUNNING");
            result.put("message", "已有重算正在执行，跳过本次");
            return result;
        }
        try {
        Date now = new Date();
        int timeoutMin = resolveTimeoutMinutes();

        List<WorkWxMessage> msgs = messageManager.lambdaQuery()
                .isNotNull(WorkWxMessage::getRoomId)
                .ne(WorkWxMessage::getRoomId, "")
                .ge(sinceTime != null, WorkWxMessage::getMsgTime, sinceTime)
                .in(WorkWxMessage::getFromType, Arrays.asList(1, 2))
                .isNotNull(WorkWxMessage::getMsgTime)
                .orderByAsc(WorkWxMessage::getRoomId)
                .orderByAsc(WorkWxMessage::getMsgTime)
                .list();

        int rounds = 0, closed = 0, open = 0;
        String curRoom = null;
        WorkWxMessage openCustomer = null; // 当前未关闭回合的首条客户消息

        for (WorkWxMessage m : msgs) {
            if (!Objects.equals(curRoom, m.getRoomId())) {
                // 切换群：上一个群若有未关闭回合，落未回复行
                if (openCustomer != null) {
                    upsert(buildUnreplied(openCustomer, now, timeoutMin));
                    open++;
                    rounds++;
                }
                curRoom = m.getRoomId();
                openCustomer = null;
            }
            Integer ft = m.getFromType();
            if (ft != null && ft == 2) {
                if (openCustomer == null) {
                    openCustomer = m; // 开回合
                }
                // 已开回合则忽略（客户连发并入同一回合）
            } else if (ft != null && ft == 1) {
                if (openCustomer != null) {
                    upsert(buildClosed(openCustomer, m, timeoutMin));
                    closed++;
                    rounds++;
                    openCustomer = null; // 关闭回合
                }
                // 员工无未回合时主动发言不计
            }
        }
        // 末尾群未关闭回合
        if (openCustomer != null) {
            upsert(buildUnreplied(openCustomer, now, timeoutMin));
            open++;
            rounds++;
        }

        result.put("ok", true);
        result.put("sinceTime", sinceTime);
        result.put("timeoutMin", timeoutMin);
        result.put("scanned", msgs.size());
        result.put("rounds", rounds);
        result.put("closed", closed);
        result.put("openUnreplied", open);

        // WXRESP-12：重算完毕后，对新达到超时且未提醒过的回合给责任客服推弹窗（仅工作时段、每回合一次）。
        // 异常只 warn、不影响重算返回。
        try {
            csOvertimeService.alertNewOverdue();
        } catch (Throwable e) {
            log.warn("[wxresp] 触发客服超时弹窗告警失败：{}", e.getMessage());
        }
        return result;
        } finally {
            recomputeLock.unlock();
        }
    }

    /** 重算最近 7 天（工单84/WXRESP-10）：入库后触发 + 定时兜底共用。 */
    public Map<String, Object> recomputeRecent() {
        return recompute(new Date(System.currentTimeMillis() - 7L * 24 * 3600 * 1000));
    }

    /** 读首响超时阈值（工作分钟）；异常/缺省回退 15。 */
    private int resolveTimeoutMinutes() {
        try {
            Integer v = bizConfigService.getInt(CFG_TIMEOUT_MIN, DEFAULT_TIMEOUT_MIN);
            return (v != null && v > 0) ? v : DEFAULT_TIMEOUT_MIN;
        } catch (Throwable e) {
            return DEFAULT_TIMEOUT_MIN;
        }
    }

    private WorkWxResponseMetric buildClosed(WorkWxMessage cust, WorkWxMessage reply, int timeoutMin) {
        WorkWxResponseMetric m = baseRow(cust);
        m.setFirstReplyMsgId(reply.getMsgId());
        m.setFirstReplyUser(reply.getFromUser());
        m.setFirstReplyName(reply.getFromName());
        m.setFirstReplyTime(reply.getMsgTime());
        long naturalSec = Math.max(0, (reply.getMsgTime().getTime() - cust.getMsgTime().getTime()) / 1000);
        long workingMin = WorkingHoursCalculator.minutesBetween(toLdt(cust.getMsgTime()), toLdt(reply.getMsgTime()));
        m.setResponseSeconds(naturalSec);
        m.setWorkingSeconds(workingMin * 60);
        m.setReplied(1);
        m.setResponseStatus(judge(workingMin, timeoutMin));
        return m;
    }

    private WorkWxResponseMetric buildUnreplied(WorkWxMessage cust, Date now, int timeoutMin) {
        WorkWxResponseMetric m = baseRow(cust);
        m.setReplied(0);
        long waitedHours = (now.getTime() - cust.getMsgTime().getTime()) / 3600000L;
        // WXRESP-09：未回复也按工作分钟判超时——自然≥24h 高风险；否则工作分钟>阈值即 timeout（进红表/KPI）；否则 pending。
        // 非工作时间消息由 minutesBetween 天然从次日 08:15 起算，无需特判。
        long workingMin = WorkingHoursCalculator.minutesBetween(toLdt(cust.getMsgTime()), toLdt(now));
        if (waitedHours >= UNREPLIED_HIGH_RISK_HOURS) {
            m.setResponseStatus("unreplied_high_risk");
        } else if (workingMin > timeoutMin) {
            m.setResponseStatus("timeout");
        } else {
            m.setResponseStatus("pending");
        }
        return m;
    }

    private WorkWxResponseMetric baseRow(WorkWxMessage cust) {
        WorkWxResponseMetric m = new WorkWxResponseMetric();
        m.setRoomId(cust.getRoomId());
        m.setChatName(cust.getChatName());
        m.setFirstCustomerMsgId(cust.getMsgId());
        m.setFirstCustomerUser(cust.getFromUser());
        m.setFirstCustomerTime(cust.getMsgTime());
        m.setTenantId(cust.getTenantId());
        return m;
    }

    /** 工作时段分钟判级（工单84/WXRESP-09）：≤5 优秀 / ≤timeoutMin 正常 / >timeoutMin 超时。 */
    private String judge(long workingMin, int timeoutMin) {
        if (workingMin <= EXCELLENT_MIN) return "excellent";
        if (workingMin <= timeoutMin) return "normal";
        return "timeout";
    }

    /** 按唯一键(room_id, first_customer_msg_id) 幂等 upsert */
    private void upsert(WorkWxResponseMetric row) {
        WorkWxResponseMetric exist = metricManager.lambdaQuery()
                .eq(WorkWxResponseMetric::getRoomId, row.getRoomId())
                .eq(WorkWxResponseMetric::getFirstCustomerMsgId, row.getFirstCustomerMsgId())
                .last("limit 1").one();
        if (exist != null) {
            row.setId(exist.getId());
        }
        metricManager.saveOrUpdate(row);
    }

    // ================= 查询（供 Controller） =================

    /** 超时/未回复清单：仅未回复(replied=0) 且 status in (timeout/serious_timeout/unreplied_high_risk)，
     *  限定 since 之后，按首客时间倒序。WXRESP-13：加 replied=0 + 时间窗，剔除迟到已回复行，与客服监控口径一致；
     *  可选 roomId（指定群）与 statuses（状态子集，须落在超时集内）筛选。
     *  工单82/WXRESP-08 补充：每行附 groupMembers（群内企业成员），与回合明细同款。 */
    public List<Map<String, Object>> unreplied(Long tenantId, Date since, String roomId,
                                               List<String> statuses, int limit) {
        List<String> st = (statuses == null || statuses.isEmpty())
                ? Arrays.asList("timeout", "serious_timeout", "unreplied_high_risk")
                : statuses;
        List<WorkWxResponseMetric> list = metricManager.lambdaQuery()
                .eq(WorkWxResponseMetric::getTenantId, tenantId)
                .eq(WorkWxResponseMetric::getReplied, 0)
                .in(WorkWxResponseMetric::getResponseStatus, st)
                .eq(StringUtils.isNotBlank(roomId), WorkWxResponseMetric::getRoomId, roomId)
                .ge(since != null, WorkWxResponseMetric::getFirstCustomerTime, since)
                .orderByDesc(WorkWxResponseMetric::getFirstCustomerTime)
                .last("limit " + Math.max(1, limit))
                .list();
        List<Map<String, Object>> out = new ArrayList<>();
        if (list == null || list.isEmpty()) return out;
        Set<String> roomIds = new HashSet<>();
        for (WorkWxResponseMetric r : list) {
            if (r.getRoomId() != null && !r.getRoomId().isEmpty()) roomIds.add(r.getRoomId());
        }
        Map<String, List<String>> groupMembersByRoom = resolveGroupMembers(roomIds);
        for (WorkWxResponseMetric r : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", r.getId());
            m.put("roomId", r.getRoomId());
            m.put("chatName", r.getChatName());
            m.put("firstCustomerMsgId", r.getFirstCustomerMsgId());
            m.put("firstCustomerUser", r.getFirstCustomerUser());
            m.put("firstCustomerTime", r.getFirstCustomerTime());
            m.put("firstReplyName", r.getFirstReplyName());
            m.put("firstReplyUser", r.getFirstReplyUser());
            m.put("responseStatus", r.getResponseStatus());
            m.put("responseSeconds", r.getResponseSeconds());
            m.put("workingSeconds", r.getWorkingSeconds());
            m.put("replied", r.getReplied());
            m.put("groupMembers", groupMembersByRoom.getOrDefault(r.getRoomId(), Collections.emptyList()));
            out.add(m);
        }
        return out;
    }

    /** 按去重 room_id 批量查群档，解析 member_list 取 type==1 成员名 → roomId→[员工名] 映射。
     *  turns 与 unreplied 共用（工单82/WXRESP-08）。 */
    private Map<String, List<String>> resolveGroupMembers(Collection<String> roomIds) {
        Map<String, List<String>> map = new HashMap<>();
        if (roomIds == null || roomIds.isEmpty()) return map;
        List<WorkWxGroupChat> groups = groupChatManager.lambdaQuery()
                .in(WorkWxGroupChat::getChatId, roomIds).list();
        if (groups != null) {
            for (WorkWxGroupChat g : groups) {
                if (g != null && g.getChatId() != null) {
                    map.put(g.getChatId(), parseEmployeeNames(g.getChatDetail()));
                }
            }
        }
        return map;
    }

    public List<Map<String, Object>> statsByGroup(Long tenantId, Date since) {
        return metricMapper.statsByGroup(tenantId, since);
    }

    public List<Map<String, Object>> statsByMember(Long tenantId, Date since) {
        return metricMapper.statsByMember(tenantId, since);
    }

    /** KPI（近 N 天，跟随选择器）：客户消息 / 员工回复 / 超时未回复数(replied=0) / 平均首响(秒)。
     *  WXRESP-13：四项统一按 since 时间窗，避免"今日"在数据不活跃/快照环境恒为 0、且与本页选择器口径一致。 */
    public Map<String, Object> kpi(Long tenantId, Date since) {
        Map<String, Object> kpi = new LinkedHashMap<>();

        long custMsg = messageManager.lambdaQuery()
                .eq(WorkWxMessage::getTenantId, tenantId)
                .isNotNull(WorkWxMessage::getRoomId).ne(WorkWxMessage::getRoomId, "")
                .eq(WorkWxMessage::getFromType, 2)
                .ge(since != null, WorkWxMessage::getMsgTime, since)
                .count();
        long replyMsg = messageManager.lambdaQuery()
                .eq(WorkWxMessage::getTenantId, tenantId)
                .isNotNull(WorkWxMessage::getRoomId).ne(WorkWxMessage::getRoomId, "")
                .eq(WorkWxMessage::getFromType, 1)
                .ge(since != null, WorkWxMessage::getMsgTime, since)
                .count();
        long overdueUnreplied = metricManager.lambdaQuery()
                .eq(WorkWxResponseMetric::getTenantId, tenantId)
                .eq(WorkWxResponseMetric::getReplied, 0)
                .in(WorkWxResponseMetric::getResponseStatus, Arrays.asList("timeout", "serious_timeout", "unreplied_high_risk"))
                .ge(since != null, WorkWxResponseMetric::getFirstCustomerTime, since)
                .count();
        // 窗口内已回复回合的平均首响（秒）
        List<WorkWxResponseMetric> todayReplied = metricManager.lambdaQuery()
                .eq(WorkWxResponseMetric::getTenantId, tenantId)
                .eq(WorkWxResponseMetric::getReplied, 1)
                .ge(since != null, WorkWxResponseMetric::getFirstReplyTime, since)
                .list();
        long avgResp = 0;
        if (todayReplied != null && !todayReplied.isEmpty()) {
            long sum = 0; int n = 0;
            for (WorkWxResponseMetric r : todayReplied) {
                if (r.getResponseSeconds() != null) { sum += r.getResponseSeconds(); n++; }
            }
            avgResp = n > 0 ? sum / n : 0;
        }
        kpi.put("todayCustomerMsg", custMsg);
        kpi.put("todayReply", replyMsg);
        kpi.put("overdueUnreplied", overdueUnreplied);
        kpi.put("avgFirstResponseSeconds", avgResp);
        return kpi;
    }

    /**
     * 会话回合明细（工单 82 / WXRESP-03）：分页 join 客户消息/员工回复内容，并批量附关键词标签。
     * @return {rows: [...], total: n}
     */
    public Map<String, Object> turns(Long tenantId, Date startTime, Date endTime, String roomId,
                                     String memberUser, String customerUser, List<String> statuses,
                                     String keyword, String orderBy, Integer pageNum, Integer pageSize) {
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("rows", new ArrayList<>());
        resp.put("total", 0L);
        if (tenantId == null) return resp;

        int pn = (pageNum == null || pageNum <= 0) ? 1 : pageNum;
        int ps = (pageSize == null || pageSize <= 0) ? 20 : pageSize;
        int offset = (pn - 1) * ps;
        List<String> st = (statuses != null && !statuses.isEmpty()) ? statuses : null;

        Long total = metricMapper.turnsCount(tenantId, startTime, endTime, roomId, memberUser, customerUser, st, keyword);
        List<Map<String, Object>> rows = metricMapper.turnsList(tenantId, startTime, endTime, roomId, memberUser,
                customerUser, st, keyword, orderBy, offset, ps);

        // 批量取本页客户消息的关键词命中标签（避免逐行查库）
        Set<String> custMsgIds = new HashSet<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                String mid = asString(r.get("first_customer_msg_id"));
                if (mid != null) custMsgIds.add(mid);
            }
        }
        Map<String, List<Map<String, Object>>> tagsByMsgId = new HashMap<>();
        if (!custMsgIds.isEmpty()) {
            List<WorkWxMessageKeywordHit> hits = keywordHitManager.lambdaQuery()
                    .in(WorkWxMessageKeywordHit::getMsgId, custMsgIds).list();
            if (hits != null) {
                for (WorkWxMessageKeywordHit h : hits) {
                    Map<String, Object> tag = new LinkedHashMap<>();
                    tag.put("ruleType", h.getRuleType());
                    tag.put("hitKeyword", h.getHitKeyword());
                    tag.put("sensitive", h.getSensitiveFlag());
                    tagsByMsgId.computeIfAbsent(h.getMsgId(), k -> new ArrayList<>()).add(tag);
                }
            }
        }

        // WXRESP-08：批量取本页各群的"群内员工"（member_list 中 type==1 的成员名），一次性查库（与红表共用 resolveGroupMembers）
        Set<String> roomIds = new HashSet<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                String rid = asString(r.get("room_id"));
                if (rid != null && !rid.isEmpty()) roomIds.add(rid);
            }
        }
        Map<String, List<String>> groupMembersByRoom = resolveGroupMembers(roomIds);

        List<Map<String, Object>> out = new ArrayList<>();
        if (rows != null) {
            for (Map<String, Object> r : rows) {
                String custMsgId = asString(r.get("first_customer_msg_id"));
                String roomKey = asString(r.get("room_id"));
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", r.get("id"));
                m.put("roomId", r.get("room_id"));
                m.put("chatName", r.get("chat_name"));
                m.put("firstCustomerMsgId", custMsgId);
                m.put("firstCustomerUser", r.get("first_customer_user"));
                m.put("customerName", r.get("customer_name"));
                m.put("firstCustomerTime", r.get("first_customer_time"));
                m.put("firstReplyMsgId", r.get("first_reply_msg_id"));
                m.put("firstReplyUser", r.get("first_reply_user"));
                m.put("firstReplyName", r.get("first_reply_name"));
                m.put("firstReplyTime", r.get("first_reply_time"));
                m.put("responseSeconds", r.get("response_seconds"));
                m.put("workingSeconds", r.get("working_seconds"));
                m.put("responseStatus", r.get("response_status"));
                m.put("replied", r.get("replied"));
                m.put("customerContent", r.get("customer_content"));
                m.put("customerMsgType", r.get("customer_msg_type"));
                m.put("replyContent", r.get("reply_content"));
                m.put("replyMsgType", r.get("reply_msg_type"));
                m.put("keywordTags", tagsByMsgId.getOrDefault(custMsgId, Collections.emptyList()));
                m.put("groupMembers", groupMembersByRoom.getOrDefault(roomKey, Collections.emptyList()));
                out.add(m);
            }
        }
        resp.put("rows", out);
        resp.put("total", total != null ? total : 0L);
        return resp;
    }

    /**
     * 会话上下文（工单 82 / WXRESP-04）：按 roomId + 时间窗口升序返回该群消息，供抽屉聊天时间线。
     */
    public List<Map<String, Object>> conversation(Long tenantId, String roomId, Date centerTime,
                                                  Integer beforeMinutes, Integer afterMinutes) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (tenantId == null || roomId == null || roomId.isEmpty() || centerTime == null) return out;
        int before = (beforeMinutes == null || beforeMinutes < 0) ? 5 : beforeMinutes;
        int after = (afterMinutes == null || afterMinutes < 0) ? 120 : afterMinutes;
        Date from = new Date(centerTime.getTime() - before * 60_000L);
        Date to = new Date(centerTime.getTime() + after * 60_000L);

        List<WorkWxMessage> msgs = messageManager.lambdaQuery()
                .eq(WorkWxMessage::getTenantId, tenantId)
                .eq(WorkWxMessage::getRoomId, roomId)
                .ge(WorkWxMessage::getMsgTime, from)
                .le(WorkWxMessage::getMsgTime, to)
                .orderByAsc(WorkWxMessage::getMsgTime)
                .last("limit 500")
                .list();
        if (msgs != null) {
            for (WorkWxMessage m : msgs) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("msgId", m.getMsgId());
                r.put("fromUser", m.getFromUser());
                r.put("fromName", m.getFromName());
                r.put("fromType", m.getFromType());
                r.put("msgType", m.getMsgType());
                r.put("content", m.getContent());
                r.put("hasMedia", m.getHasMedia());
                r.put("msgTime", m.getMsgTime());
                out.add(r);
            }
        }
        return out;
    }

    /** 解析群 member_list，取 type==1（企业成员）的名字列表（去重、保序）。解析失败返回空列表。 */
    private List<String> parseEmployeeNames(String chatDetail) {
        List<String> names = new ArrayList<>();
        if (StringUtils.isBlank(chatDetail)) return names;
        try {
            String s = chatDetail.trim();
            JSONArray members = s.startsWith("[") ? JSON.parseArray(s)
                    : (JSON.parseObject(s) != null ? JSON.parseObject(s).getJSONArray("member_list") : null);
            if (members == null) return names;
            Set<String> seen = new LinkedHashSet<>();
            for (int i = 0; i < members.size(); i++) {
                JSONObject mb = members.getJSONObject(i);
                if (mb == null) continue;
                Integer type = mb.getInteger("type");
                if (type != null && type == 1) {
                    String name = mb.getString("name");
                    if (StringUtils.isBlank(name)) name = mb.getString("userid");
                    if (StringUtils.isNotBlank(name)) seen.add(name);
                }
            }
            names.addAll(seen);
        } catch (Exception e) {
            // 解析失败保持空列表，不影响主流程
        }
        return names;
    }

    private static String asString(Object o) {
        return o == null ? null : o.toString();
    }

    private static LocalDateTime toLdt(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
