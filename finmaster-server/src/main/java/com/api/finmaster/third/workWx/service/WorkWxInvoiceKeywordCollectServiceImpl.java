package com.qykj.finmaster.third.workWx.service;

import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.utils.third.FinancialUtils;
import com.qykj.finmaster.third.workWx.entity.WorkWxGroupChat;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessage;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessageKeywordHit;
import com.qykj.finmaster.third.workWx.entity.WorkWxMsgCursor;
import com.qykj.finmaster.third.workWx.manager.WorkWxGroupChatManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageKeywordHitManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMsgCursorManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 企业微信"发票业务"关键词命中采集（工单89 / WXKW-04 重做：水位线去重 + 窗口修正 + 同群合并）。
 *
 * <p><b>当前仅查询 + 打印，不落业务库（仅游标水位线一行）。</b>
 * <ul>
 *   <li>去重：复用 {@code work_wx_msg_cursor}（cursor_key='invoice_collect'，last_seq 存"已处理到的最大 hitTime 毫秒"）作水位线，
 *       每轮处理 {@code hitTime > 水位线 AND hitTime <= now-10min}，处理后推水位线——零新表、重启不重复、漏跑一轮也不丢。</li>
 *   <li>窗口：只处理 ≥10 分钟前的命中，确保命中后 +10 分钟客户消息已到齐。</li>
 *   <li>合并：本轮命中按 roomId 分组，每群一条（群名 + 关键词 distinct + 时段 [minHit,maxHit] + 客户消息）。</li>
 * </ul>
 *
 * <p>TODO 后续完善：入库 / 通知 / 商机生成。</p>
 */
@Slf4j
@Service
public class WorkWxInvoiceKeywordCollectServiceImpl {

    private static final String RULE_TYPE_INVOICE = "INVOICE";
    /** 采集游标键（水位线 = 已处理到的最大 hitTime 毫秒） */
    private static final String CURSOR_KEY = "invoice_collect";
    /** 滞后窗口（分钟）：只处理 ≥N 分钟前的命中，确保命中后客户消息已到齐 */
    private static final long LAG_MINUTES = 10;
    /** 客户消息取数窗口（命中时段前后各 N 分钟） */
    private static final long CONTEXT_MINUTES = 10;
    private static final long MS_PER_MIN = 60_000L;
    /** 会话取数上限（全发言人，防爆） */
    private static final int CONVERSATION_MSG_LIMIT = 500;
    /** 同群发票推送跨片去重窗口（分钟，用户定 10）：本群命中前 N 分钟内若已有命中（落在上一片、已推过）则跳过本次推送 */
    private static final long DEDUP_MINUTES = 10;
    /** 推财务的会话消息行分隔符（WXKW-11：财务靠 || 切分每条消息；单条 content 内部的 \n 原样保留） */
    private static final String MSG_SEP = "||";

    @Autowired
    private WorkWxMessageKeywordHitManager hitManager;
    @Autowired
    private WorkWxMessageManager messageManager;
    @Autowired
    private WorkWxGroupChatManager groupChatManager;
    @Autowired
    private WorkWxMsgCursorManager cursorManager;

    /** 采集水位线之后、≥10 分钟前的 INVOICE 命中，按群合并打印，并推进水位线。 */
    public void collectRecent() {
        WorkWxMsgCursor cursor = cursorManager.lambdaQuery()
                .eq(WorkWxMsgCursor::getCursorKey, CURSOR_KEY).last("limit 1").one();
        // WXKW-07：水位线改用 last_hit_time（时间列），不再用 last_seq 存毫秒
        Date watermark = (cursor != null && cursor.getLastHitTime() != null) ? cursor.getLastHitTime() : new Date(0);
        Date cutoff = new Date(System.currentTimeMillis() - LAG_MINUTES * MS_PER_MIN);
        if (!cutoff.after(watermark)) {
            return; // 无新的"已到齐"命中区间
        }

        List<WorkWxMessageKeywordHit> hits = hitManager.lambdaQuery()
                .eq(WorkWxMessageKeywordHit::getRuleType, RULE_TYPE_INVOICE)
                .gt(WorkWxMessageKeywordHit::getHitTime, watermark)
                .le(WorkWxMessageKeywordHit::getHitTime, cutoff)
                .orderByAsc(WorkWxMessageKeywordHit::getHitTime)
                .list();
        if (hits == null || hits.isEmpty()) {
            return; // 本区间无命中；水位线不动（下轮 cutoff 前移自然覆盖）
        }

        // 按 roomId 分组（保序），并记录全局最大 hitTime 用于推进水位线
        Map<String, List<WorkWxMessageKeywordHit>> byRoom = new LinkedHashMap<>();
        long watermarkMs = watermark.getTime();
        long maxHitMs = watermarkMs;
        for (WorkWxMessageKeywordHit h : hits) {
            byRoom.computeIfAbsent(h.getRoomId(), k -> new ArrayList<>()).add(h);
            if (h.getHitTime() != null) maxHitMs = Math.max(maxHitMs, h.getHitTime().getTime());
        }

        log.info("[invoice-collect-task] 本轮发票业务命中 {} 条、涉群 {} 个（水位线 {} → {}）",
                hits.size(), byRoom.size(), watermark, new Date(maxHitMs));
        for (Map.Entry<String, List<WorkWxMessageKeywordHit>> e : byRoom.entrySet()) {
            String roomId = e.getKey();
            List<WorkWxMessageKeywordHit> groupHits = e.getValue();
            // 命中时段 [minHit, maxHit] + 关键词 distinct
            long minMs = Long.MAX_VALUE, maxMs = Long.MIN_VALUE;
            LinkedHashSet<String> keywords = new LinkedHashSet<>();
            for (WorkWxMessageKeywordHit h : groupHits) {
                if (h.getHitTime() != null) {
                    minMs = Math.min(minMs, h.getHitTime().getTime());
                    maxMs = Math.max(maxMs, h.getHitTime().getTime());
                }
                if (StringUtils.isNotBlank(h.getHitKeyword())) keywords.add(h.getHitKeyword());
            }
            Date minHit = new Date(minMs);
            Date maxHit = new Date(maxMs);
            String chatName = resolveChatName(roomId);
            String conversation = loadConversation(roomId, minMs, maxMs);
            log.info("[invoice-collect-task] 群名={} 关键词={} 命中时段={}~{} 会话={}",
                    chatName, String.join(",", keywords), minHit, maxHit, conversation);

            // WXKW-06：会话非空时推送财务系统入库（复用现有 FinancialUtils）；失败只 log（不回退水位线，at-most-once）
            if (StringUtils.isNotBlank(conversation) && !"无".equals(conversation)) {
                // WXKW-09：跨片去重——本群命中前 DEDUP_MINUTES 内若已有命中（落在上一片、已推过，即 hitTime<=本轮起始水位线），跳过
                boolean recentlyPushed = hitManager.lambdaQuery()
                        .eq(WorkWxMessageKeywordHit::getRuleType, RULE_TYPE_INVOICE)
                        .eq(WorkWxMessageKeywordHit::getRoomId, roomId)
                        .ge(WorkWxMessageKeywordHit::getHitTime, new Date(minMs - DEDUP_MINUTES * MS_PER_MIN))
                        .le(WorkWxMessageKeywordHit::getHitTime, watermark)
                        .count() > 0;
                if (recentlyPushed) {
                    log.info("[invoice-collect-task] 同群近 {} 分钟已推送，跳过重复 群名={}", DEDUP_MINUTES, chatName);
                } else {
                    JSONObject r = FinancialUtils.collectWxGroupChat(chatName, conversation);
                    if (r == null || !Boolean.TRUE.equals(r.getBoolean("success"))) {
                        log.error("[invoice-collect-task] 推送财务系统失败 群名={} msg={}",
                                chatName, r == null ? "调用异常" : r.getString("msg"));
                    }
                }
            }
        }

        // 推进水位线（saveOrUpdate；仅游标一行，不落业务库）
        if (maxHitMs > watermarkMs) {
            if (cursor == null) {
                cursor = new WorkWxMsgCursor();
                cursor.setCursorKey(CURSOR_KEY);
            }
            cursor.setLastHitTime(new Date(maxHitMs));
            cursor.setUpdateTime(new Date());
            cursorManager.saveOrUpdate(cursor);
        }
    }

    /**
     * 一次性补传（工单92 / WXKW-08）：查 [from,to] 内 INVOICE 命中按 roomId 分组，每群取完整会话推送财务。
     * <b>全程不读写采集水位线</b>，与定时采集互不影响。dryRun=true 只统计不推（预览）。
     * @return {from,to,dryRun,groups,pushed,failed,skippedEmpty}
     */
    public Map<String, Object> backfill(Date from, Date to, boolean dryRun) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("from", from);
        result.put("to", to);
        result.put("dryRun", dryRun);
        int pushed = 0, failed = 0, skippedEmpty = 0;

        List<WorkWxMessageKeywordHit> hits = (from == null || to == null) ? null : hitManager.lambdaQuery()
                .eq(WorkWxMessageKeywordHit::getRuleType, RULE_TYPE_INVOICE)
                .ge(WorkWxMessageKeywordHit::getHitTime, from)
                .le(WorkWxMessageKeywordHit::getHitTime, to)
                .orderByAsc(WorkWxMessageKeywordHit::getHitTime)
                .list();

        Map<String, List<WorkWxMessageKeywordHit>> byRoom = new LinkedHashMap<>();
        if (hits != null) {
            for (WorkWxMessageKeywordHit h : hits) {
                byRoom.computeIfAbsent(h.getRoomId(), k -> new ArrayList<>()).add(h);
            }
        }

        for (Map.Entry<String, List<WorkWxMessageKeywordHit>> e : byRoom.entrySet()) {
            String roomId = e.getKey();
            long minMs = Long.MAX_VALUE, maxMs = Long.MIN_VALUE;
            for (WorkWxMessageKeywordHit h : e.getValue()) {
                if (h.getHitTime() != null) {
                    minMs = Math.min(minMs, h.getHitTime().getTime());
                    maxMs = Math.max(maxMs, h.getHitTime().getTime());
                }
            }
            String chatName = resolveChatName(roomId);
            String conversation = loadConversation(roomId, minMs, maxMs);
            if (StringUtils.isBlank(conversation) || "无".equals(conversation)) {
                skippedEmpty++;
                continue;
            }
            if (dryRun) {
                continue; // 预览：只统计、不推送
            }
            JSONObject r = FinancialUtils.collectWxGroupChat(chatName, conversation);
            if (r != null && Boolean.TRUE.equals(r.getBoolean("success"))) {
                pushed++;
            } else {
                failed++;
                log.error("[invoice-backfill] 补传财务系统失败 群名={} msg={}",
                        chatName, r == null ? "调用异常" : r.getString("msg"));
            }
        }

        result.put("groups", byRoom.size());
        result.put("pushed", pushed);
        result.put("failed", failed);
        result.put("skippedEmpty", skippedEmpty);
        log.info("[invoice-backfill] from={} to={} dryRun={} groups={} pushed={} failed={} skippedEmpty={}",
                from, to, dryRun, byRoom.size(), pushed, failed, skippedEmpty);
        return result;
    }

    /** 群名：work_wx_group_chat(chatId=roomId).chatName → message.chatName → roomId。 */
    private String resolveChatName(String roomId) {
        if (StringUtils.isBlank(roomId)) return "(无群)";
        WorkWxGroupChat g = groupChatManager.lambdaQuery()
                .eq(WorkWxGroupChat::getChatId, roomId).last("limit 1").one();
        if (g != null && StringUtils.isNotBlank(g.getChatName())) return g.getChatName();
        WorkWxMessage m = messageManager.lambdaQuery()
                .eq(WorkWxMessage::getRoomId, roomId)
                .isNotNull(WorkWxMessage::getChatName).ne(WorkWxMessage::getChatName, "")
                .last("limit 1").one();
        if (m != null && StringUtils.isNotBlank(m.getChatName())) return m.getChatName();
        return roomId;
    }

    /**
     * 该群在 [minHit-10min, maxHit+10min] 内**全发言人**消息（不再仅 from_type=2，含 type=0 未解析/type=1 员工），
     * 升序，每条 {@code 发言人：内容}（发言人空退回 fromUser），行间用 {@link #MSG_SEP}(||) 连接；跳过空 content；无则"无"。
     * 单条 content 内部原有的 \n 原样保留（财务靠 || 切分每条消息）。
     * （WXKW-04 修复：原仅取 from_type=2 丢失约 70% 消息，财务反馈"消息不全"；WXKW-11：分隔符 \n→||）
     */
    private String loadConversation(String roomId, long minMs, long maxMs) {
        Date from = new Date(minMs - CONTEXT_MINUTES * MS_PER_MIN);
        Date to = new Date(maxMs + CONTEXT_MINUTES * MS_PER_MIN);
        List<WorkWxMessage> msgs = messageManager.lambdaQuery()
                .eq(WorkWxMessage::getRoomId, roomId)
                .ge(WorkWxMessage::getMsgTime, from)
                .le(WorkWxMessage::getMsgTime, to)
                .orderByAsc(WorkWxMessage::getMsgTime)
                .last("limit " + CONVERSATION_MSG_LIMIT)
                .list();
        if (msgs == null || msgs.isEmpty()) return "无";
        String joined = msgs.stream()
                .filter(m -> StringUtils.isNotBlank(m.getContent()))
                .map(m -> (StringUtils.isNotBlank(m.getFromName()) ? m.getFromName() : m.getFromUser())
                        + "：" + m.getContent())
                .collect(Collectors.joining(MSG_SEP));
        return StringUtils.isBlank(joined) ? "无" : joined;
    }
}
