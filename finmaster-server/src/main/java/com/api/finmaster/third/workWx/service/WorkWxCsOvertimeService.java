package com.qykj.finmaster.third.workWx.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.third.workWx.entity.WorkWxGroupChat;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessage;
import com.qykj.finmaster.third.workWx.entity.WorkWxResponseMetric;
import com.qykj.finmaster.third.workWx.manager.WorkWxGroupChatManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxResponseMetricManager;
import com.qykj.finmaster.third.workWx.service.WorkWxCsAgentResolver.CsAgent;
import com.qykj.finmaster.workbench.util.WorkingHoursCalculator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 客服部超时未处理监控 + 超时弹窗告警（WXRESP-12）。
 *
 * <p><b>归因</b>：一条"超时未处理回合"（{@code work_wx_response_metric} 中 replied=0 且状态属于超时集）发生在某群，
 * 责任人 = 该群 member_list 中 type=1、且 userid 能对应到本系统<b>客服部员工</b>（{@link WorkWxCsAgentResolver}）的成员。
 * 一个群有多个客服 → 该回合同时计入每个客服、弹窗也推给每个客服（"群内所有客服都算"）。</p>
 *
 * <p><b>标签数据</b>：{@link #summary} 每个客服一行（待处理超时数/高风险数/最久等待/已回复量/平均首响）；
 * {@link #detail} 某客服名下所有超时未处理回合明细。</p>
 *
 * <p><b>弹窗</b>：{@link #alertNewOverdue} 由回复指标重算后触发，对<b>新达到超时且未提醒过</b>（alerted=0）的回合，
 * 按群聚合、给责任客服推 STOMP 弹窗，然后置 alerted=1 去重（每回合仅弹一次）。仅工作时段推送；
 * 无法归因（群内无已绑定客服）的回合保持 alerted=0，待补录 wecom_userid 后自动补弹。</p>
 */
@Slf4j
@Service
public class WorkWxCsOvertimeService {

    /** "超时未处理"状态集：均为 replied=0 的待办红状态。 */
    private static final List<String> OVERDUE_STATUSES =
            Arrays.asList("timeout", "serious_timeout", "unreplied_high_risk");
    /** 告警扫描回看窗口（天）：覆盖 24h 高风险，避免全表扫描。 */
    private static final int ALERT_LOOKBACK_DAYS = 3;
    /** 弹窗 payload 携带的本批回合明细上限（防首轮积压把 STOMP 消息撑爆）。 */
    private static final int ALERT_ROUNDS_LIMIT = 50;

    /** 告警锁：防重算并发触发的重入。 */
    private final ReentrantLock alertLock = new ReentrantLock();

    @Autowired
    private WorkWxResponseMetricManager metricManager;
    @Autowired
    private WorkWxGroupChatManager groupChatManager;
    @Autowired
    private WorkWxMessageManager messageManager;
    @Autowired
    private WorkWxCsAgentResolver csAgentResolver;
    @Autowired
    private WorkWxOvertimeNotificationService overtimeNotificationService;

    // ==================== 标签：汇总（管理者视角，每个客服一行） ====================

    /**
     * 客服超时汇总：以客服部全体员工为行（含 0 超时者），叠加超时未处理归因与已回复业绩。
     * 返回 {agents:[每客服一行], distinctUnhandled:去重超时回合数, unattributedCount:未归因数, totalOverdue:总数}。
     * 注意：多客服群同一回合计入每位客服 → agents 各行相加可大于 distinctUnhandled；
     * distinctUnhandled + unattributedCount = totalOverdue（与总览 KPI 同口径，账目闭环）。
     */
    public Map<String, Object> summary(Long tenantId, int days) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agents", Collections.emptyList());
        result.put("distinctUnhandled", 0L);
        result.put("unattributedCount", 0L);
        result.put("totalOverdue", 0L);
        if (tenantId == null) return result;
        Date since = new Date(System.currentTimeMillis() - Math.max(1, days) * 24L * 3600 * 1000);
        Date now = new Date();

        List<CsAgent> agents = csAgentResolver.listCsAgents(tenantId);
        Map<String, CsAgent> byWecom = new HashMap<>();
        for (CsAgent a : agents) byWecom.put(a.wecomUserid, a);

        // 每个客服的累加器（按 wecomUserid）
        Map<String, Acc> accByWecom = new LinkedHashMap<>();
        for (CsAgent a : agents) accByWecom.put(a.wecomUserid, new Acc(a));

        // 1) 超时未处理回合 → 按群归因给客服；同时统计去重回合数与未归因数（与总览 KPI 对账）
        List<WorkWxResponseMetric> overdue = loadOverdue(tenantId, since);
        Map<String, List<String>> roomEmployees = resolveRoomEmployeeUserids(collectRoomIds(overdue));
        long distinctUnhandled = 0, unattributed = 0;
        for (WorkWxResponseMetric m : overdue) {
            List<String> emps = roomEmployees.getOrDefault(m.getRoomId(), Collections.emptyList());
            long waitedMin = m.getFirstCustomerTime() == null ? 0
                    : Math.max(0, (now.getTime() - m.getFirstCustomerTime().getTime()) / 60000L);
            boolean attributed = false;
            for (String uid : emps) {
                Acc acc = accByWecom.get(uid);
                if (acc == null) continue; // 该员工不是客服部（或未绑定）→ 不计
                attributed = true;
                acc.unhandled++;
                if ("unreplied_high_risk".equals(m.getResponseStatus())) acc.highRisk++;
                if (m.getRoomId() != null) acc.rooms.add(m.getRoomId());
                if (waitedMin > acc.maxWaitedMin) {
                    acc.maxWaitedMin = waitedMin;
                    acc.oldestCustomerTime = m.getFirstCustomerTime();
                }
            }
            if (attributed) distinctUnhandled++; else unattributed++;
        }

        // 2) 已回复业绩（首响员工 = 客服 wecom_userid）：reply_count + 平均首响
        if (!byWecom.isEmpty()) {
            List<WorkWxResponseMetric> replied = metricManager.lambdaQuery()
                    .eq(WorkWxResponseMetric::getTenantId, tenantId)
                    .eq(WorkWxResponseMetric::getReplied, 1)
                    .ge(WorkWxResponseMetric::getFirstReplyTime, since)
                    .in(WorkWxResponseMetric::getFirstReplyUser, byWecom.keySet())
                    .list();
            for (WorkWxResponseMetric m : replied) {
                Acc acc = accByWecom.get(m.getFirstReplyUser());
                if (acc == null) continue;
                acc.replyCount++;
                if (m.getResponseSeconds() != null) { acc.respSecSum += m.getResponseSeconds(); acc.respSecN++; }
                if (Arrays.asList("timeout", "serious_timeout").contains(m.getResponseStatus())) acc.repliedTimeout++;
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Acc acc : accByWecom.values()) out.add(acc.toMap());
        // 待处理超时多的排前；其次高风险多的
        out.sort((x, y) -> {
            int c = Long.compare(num(y.get("unhandledCount")), num(x.get("unhandledCount")));
            if (c != 0) return c;
            return Long.compare(num(y.get("highRiskCount")), num(x.get("highRiskCount")));
        });
        result.put("agents", out);
        result.put("distinctUnhandled", distinctUnhandled);
        result.put("unattributedCount", unattributed);
        result.put("totalOverdue", (long) overdue.size());
        return result;
    }

    // ==================== 标签：明细（某客服名下的超时未处理回合） ====================

    public List<Map<String, Object>> detail(Long tenantId, String wecomUserid, int days) {
        if (tenantId == null || StringUtils.isBlank(wecomUserid)) return Collections.emptyList();
        Date since = new Date(System.currentTimeMillis() - Math.max(1, days) * 24L * 3600 * 1000);
        Date now = new Date();

        List<WorkWxResponseMetric> overdue = loadOverdue(tenantId, since);
        Map<String, List<String>> roomEmployees = resolveRoomEmployeeUserids(collectRoomIds(overdue));

        // 仅保留该客服所在群的回合
        List<WorkWxResponseMetric> mine = new ArrayList<>();
        for (WorkWxResponseMetric m : overdue) {
            if (roomEmployees.getOrDefault(m.getRoomId(), Collections.emptyList()).contains(wecomUserid)) {
                mine.add(m);
            }
        }
        // 批量取首条客户消息内容
        Map<String, WorkWxMessage> custMsgById = loadMessagesByMsgId(collectCustomerMsgIds(mine));

        // 最久等待优先（oldest first）
        mine.sort(Comparator.comparing(m -> m.getFirstCustomerTime() == null ? new Date(0) : m.getFirstCustomerTime()));

        List<Map<String, Object>> out = new ArrayList<>();
        for (WorkWxResponseMetric m : mine) {
            WorkWxMessage cm = custMsgById.get(m.getFirstCustomerMsgId());
            long waitedMin = m.getFirstCustomerTime() == null ? 0
                    : Math.max(0, (now.getTime() - m.getFirstCustomerTime().getTime()) / 60000L);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("roomId", m.getRoomId());
            r.put("chatName", m.getChatName());
            r.put("firstCustomerUser", m.getFirstCustomerUser());
            r.put("customerName", cm != null ? cm.getFromName() : null);
            r.put("customerContent", cm != null ? cm.getContent() : null);
            r.put("customerMsgType", cm != null ? cm.getMsgType() : null);
            r.put("firstCustomerMsgId", m.getFirstCustomerMsgId());
            r.put("firstCustomerTime", m.getFirstCustomerTime());
            r.put("waitedMinutes", waitedMin);
            r.put("responseStatus", m.getResponseStatus());
            out.add(r);
        }
        return out;
    }

    // ==================== 弹窗告警（重算后触发） ====================

    /**
     * 对新达到超时且未提醒过的回合，按群给责任客服推弹窗，然后置 alerted=1 去重。仅工作时段执行。
     */
    public void alertNewOverdue() {
        if (!WorkingHoursCalculator.isWorkingTime(LocalDateTime.now())) {
            return; // 非工作时段不打扰；仍未处理的回合等工作时段恢复后再弹（alerted 仍为 0）
        }
        if (!alertLock.tryLock()) {
            return;
        }
        try {
            Date since = new Date(System.currentTimeMillis() - ALERT_LOOKBACK_DAYS * 24L * 3600 * 1000);
            Date now = new Date();
            // 未提醒过的超时未处理回合
            List<WorkWxResponseMetric> rows = metricManager.lambdaQuery()
                    .eq(WorkWxResponseMetric::getReplied, 0)
                    .in(WorkWxResponseMetric::getResponseStatus, OVERDUE_STATUSES)
                    .ge(WorkWxResponseMetric::getFirstCustomerTime, since)
                    .and(w -> w.eq(WorkWxResponseMetric::getAlerted, 0).or().isNull(WorkWxResponseMetric::getAlerted))
                    .list();
            if (rows == null || rows.isEmpty()) return;

            // 按租户分组（多租户安全；当前实际单租户）
            Map<Long, List<WorkWxResponseMetric>> byTenant = new LinkedHashMap<>();
            for (WorkWxResponseMetric m : rows) {
                byTenant.computeIfAbsent(m.getTenantId(), k -> new ArrayList<>()).add(m);
            }

            List<Long> alertedIds = new ArrayList<>();
            for (Map.Entry<Long, List<WorkWxResponseMetric>> te : byTenant.entrySet()) {
                Long tenantId = te.getKey();
                List<WorkWxResponseMetric> tRows = te.getValue();
                Map<String, CsAgent> agentByWecom = csAgentResolver.mapByWecomUserid(tenantId);
                if (agentByWecom.isEmpty()) continue; // 无已绑定客服 → 无法归因，保持 alerted=0 待补录后补弹
                Map<String, List<String>> roomEmployees = resolveRoomEmployeeUserids(collectRoomIds(tRows));

                // 按客服聚合：一个客服一周期只弹一条（跨群汇总），避免积压时弹窗刷屏。
                // 一条回合可归属多个客服（群内所有客服都算）→ 分别计入各自列表。
                Map<String, List<WorkWxResponseMetric>> byAgent = new LinkedHashMap<>();
                Set<Long> attributedIds = new LinkedHashSet<>();
                for (WorkWxResponseMetric m : tRows) {
                    for (String uid : roomEmployees.getOrDefault(m.getRoomId(), Collections.emptyList())) {
                        if (agentByWecom.containsKey(uid)) {
                            byAgent.computeIfAbsent(uid, k -> new ArrayList<>()).add(m);
                            attributedIds.add(m.getId());
                        }
                    }
                }
                // 批量取本批回合的客户消息内容（弹窗明细展示用）
                Map<String, WorkWxMessage> custMsgById = loadMessagesByMsgId(collectCustomerMsgIds(tRows));
                for (Map.Entry<String, List<WorkWxResponseMetric>> ae : byAgent.entrySet()) {
                    CsAgent agent = agentByWecom.get(ae.getKey());
                    List<WorkWxResponseMetric> list = ae.getValue();
                    // 最久等待优先排序；首条即代表回合
                    list.sort(Comparator.comparing(m -> m.getFirstCustomerTime() == null ? new Date(0) : m.getFirstCustomerTime()));
                    WorkWxResponseMetric rep = list.get(0);
                    Set<String> rooms = new HashSet<>();
                    for (WorkWxResponseMetric m : list) if (m.getRoomId() != null) rooms.add(m.getRoomId());
                    WorkWxMessage cm = custMsgById.get(rep.getFirstCustomerMsgId());
                    String custMsg = cm != null ? cm.getContent() : null;
                    long waitedMin = rep.getFirstCustomerTime() == null ? 0
                            : Math.max(0, (now.getTime() - rep.getFirstCustomerTime().getTime()) / 60000L);
                    Long centerMs = rep.getFirstCustomerTime() != null ? rep.getFirstCustomerTime().getTime() : null;
                    // 本批回合明细（≤50 条）：弹窗"查看明细"只展示本次提醒对应的回合，而非全量积压
                    List<Map<String, Object>> rounds = new ArrayList<>();
                    for (int i = 0; i < Math.min(list.size(), ALERT_ROUNDS_LIMIT); i++) {
                        WorkWxResponseMetric m = list.get(i);
                        WorkWxMessage mm = custMsgById.get(m.getFirstCustomerMsgId());
                        Map<String, Object> r = new LinkedHashMap<>();
                        r.put("roomId", m.getRoomId());
                        r.put("chatName", m.getChatName());
                        r.put("customerUser", m.getFirstCustomerUser());
                        r.put("customerName", mm != null ? mm.getFromName() : null);
                        r.put("customerMsg", mm != null ? mm.getContent() : null);
                        r.put("msgType", mm != null ? mm.getMsgType() : null);
                        r.put("firstCustomerTimeMs", m.getFirstCustomerTime() != null ? m.getFirstCustomerTime().getTime() : null);
                        r.put("waitedMinutes", m.getFirstCustomerTime() == null ? 0
                                : Math.max(0, (now.getTime() - m.getFirstCustomerTime().getTime()) / 60000L));
                        r.put("status", m.getResponseStatus());
                        rounds.add(r);
                    }
                    overtimeNotificationService.notifyOvertime(agent.username, agent.wecomUserid, rep.getRoomId(),
                            rep.getChatName(), rep.getFirstCustomerUser(), custMsg, centerMs, waitedMin,
                            rep.getResponseStatus(), list.size(), rooms.size(), rounds);
                }
                alertedIds.addAll(attributedIds);
            }

            // 置 alerted=1 去重（每回合仅弹一次）
            if (!alertedIds.isEmpty()) {
                metricManager.lambdaUpdate()
                        .in(WorkWxResponseMetric::getId, alertedIds)
                        .set(WorkWxResponseMetric::getAlerted, 1)
                        .set(WorkWxResponseMetric::getAlertedTime, now)
                        .update();
                log.info("[wx-overtime] 超时弹窗：提醒 {} 个回合", alertedIds.size());
            }
        } catch (Throwable e) {
            log.warn("[wx-overtime] 超时弹窗告警异常：{}", e.getMessage(), e);
        } finally {
            alertLock.unlock();
        }
    }

    // ==================== 本人自查权限辅助（WXRESP-14） ====================

    /** 按登录名解析该用户对应的客服档案（客服部 + 已绑 wecom_userid）；非客服/未绑定返回 null。 */
    public WorkWxCsAgentResolver.CsAgent resolveAgentByUsername(Long tenantId, String username) {
        if (tenantId == null || StringUtils.isBlank(username)) return null;
        for (CsAgent a : csAgentResolver.listCsAgents(tenantId)) {
            if (username.equals(a.username)) return a;
        }
        return null;
    }

    /** 登录用户是否为指定 wecomUserid 的客服本人（客服自查明细放行用）。 */
    public boolean isSelfAgent(Long tenantId, String username, String wecomUserid) {
        if (StringUtils.isBlank(wecomUserid)) return false;
        CsAgent a = resolveAgentByUsername(tenantId, username);
        return a != null && wecomUserid.equals(a.wecomUserid);
    }

    /** 登录用户（客服）是否为某群成员（客服自查会话放行用）：按其 wecom_userid 匹配群 member_list。 */
    public boolean isRoomMember(Long tenantId, String username, String roomId) {
        if (StringUtils.isBlank(roomId)) return false;
        CsAgent a = resolveAgentByUsername(tenantId, username);
        if (a == null || StringUtils.isBlank(a.wecomUserid)) return false;
        WorkWxGroupChat g = groupChatManager.lambdaQuery()
                .eq(WorkWxGroupChat::getChatId, roomId).last("limit 1").one();
        return g != null && StringUtils.isNotBlank(g.getChatDetail())
                && g.getChatDetail().contains("\"userid\":\"" + a.wecomUserid + "\"");
    }

    // ==================== 内部工具 ====================

    private List<WorkWxResponseMetric> loadOverdue(Long tenantId, Date since) {
        return metricManager.lambdaQuery()
                .eq(WorkWxResponseMetric::getTenantId, tenantId)
                .eq(WorkWxResponseMetric::getReplied, 0)
                .in(WorkWxResponseMetric::getResponseStatus, OVERDUE_STATUSES)
                .ge(WorkWxResponseMetric::getFirstCustomerTime, since)
                .list();
    }

    private Set<String> collectRoomIds(List<WorkWxResponseMetric> rows) {
        Set<String> s = new HashSet<>();
        for (WorkWxResponseMetric m : rows) {
            if (StringUtils.isNotBlank(m.getRoomId())) s.add(m.getRoomId());
        }
        return s;
    }

    private Set<String> collectCustomerMsgIds(List<WorkWxResponseMetric> rows) {
        Set<String> s = new HashSet<>();
        for (WorkWxResponseMetric m : rows) {
            if (StringUtils.isNotBlank(m.getFirstCustomerMsgId())) s.add(m.getFirstCustomerMsgId());
        }
        return s;
    }

    /** 批量取群的企业成员(type=1) userid 列表：roomId → [userid...]。 */
    private Map<String, List<String>> resolveRoomEmployeeUserids(Set<String> roomIds) {
        Map<String, List<String>> map = new HashMap<>();
        if (roomIds == null || roomIds.isEmpty()) return map;
        List<WorkWxGroupChat> groups = groupChatManager.lambdaQuery()
                .in(WorkWxGroupChat::getChatId, roomIds).list();
        if (groups != null) {
            for (WorkWxGroupChat g : groups) {
                if (g != null && g.getChatId() != null) {
                    map.put(g.getChatId(), parseEmployeeUserids(g.getChatDetail()));
                }
            }
        }
        return map;
    }

    /** 解析 member_list，取 type==1（企业成员）的 userid 列表（去重保序）。 */
    private List<String> parseEmployeeUserids(String chatDetail) {
        List<String> ids = new ArrayList<>();
        if (StringUtils.isBlank(chatDetail)) return ids;
        try {
            String s = chatDetail.trim();
            JSONArray members = s.startsWith("[") ? JSON.parseArray(s)
                    : (JSON.parseObject(s) != null ? JSON.parseObject(s).getJSONArray("member_list") : null);
            if (members == null) return ids;
            Set<String> seen = new LinkedHashSet<>();
            for (int i = 0; i < members.size(); i++) {
                JSONObject mb = members.getJSONObject(i);
                if (mb == null) continue;
                Integer type = mb.getInteger("type");
                String uid = mb.getString("userid");
                if (type != null && type == 1 && StringUtils.isNotBlank(uid)) seen.add(uid);
            }
            ids.addAll(seen);
        } catch (Exception e) {
            // 解析失败返回空，不影响主流程
        }
        return ids;
    }

    private Map<String, WorkWxMessage> loadMessagesByMsgId(Set<String> msgIds) {
        Map<String, WorkWxMessage> map = new HashMap<>();
        if (msgIds == null || msgIds.isEmpty()) return map;
        List<WorkWxMessage> list = messageManager.lambdaQuery()
                .in(WorkWxMessage::getMsgId, msgIds).list();
        if (list != null) {
            for (WorkWxMessage m : list) {
                if (m.getMsgId() != null && !map.containsKey(m.getMsgId())) map.put(m.getMsgId(), m);
            }
        }
        return map;
    }

    private WorkWxMessage loadOneMessage(String msgId) {
        if (StringUtils.isBlank(msgId)) return null;
        return messageManager.lambdaQuery().eq(WorkWxMessage::getMsgId, msgId).last("limit 1").one();
    }

    private static long num(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        try { return Long.parseLong(String.valueOf(o)); } catch (Exception e) { return 0; }
    }

    /** 单客服累加器。 */
    private static class Acc {
        final CsAgent agent;
        long unhandled = 0;
        long highRisk = 0;
        long maxWaitedMin = 0;
        Date oldestCustomerTime = null;
        final Set<String> rooms = new HashSet<>();
        long replyCount = 0;
        long repliedTimeout = 0;
        long respSecSum = 0;
        long respSecN = 0;

        Acc(CsAgent agent) { this.agent = agent; }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username", agent.username);
            m.put("realName", agent.realName);
            m.put("wecomUserid", agent.wecomUserid);
            m.put("unhandledCount", unhandled);
            m.put("highRiskCount", highRisk);
            m.put("groupCount", (long) rooms.size());
            m.put("maxWaitedMinutes", maxWaitedMin);
            m.put("oldestCustomerTime", oldestCustomerTime);
            m.put("replyCount", replyCount);
            m.put("repliedTimeoutCount", repliedTimeout);
            m.put("avgRespSeconds", respSecN > 0 ? respSecSum / respSecN : 0);
            return m;
        }
    }
}
