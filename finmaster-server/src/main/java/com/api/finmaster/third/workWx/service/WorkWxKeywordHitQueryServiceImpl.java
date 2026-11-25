package com.qykj.finmaster.third.workWx.service;

import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qykj.finmaster.third.workWx.entity.WorkWxGroupChat;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessage;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessageKeywordHit;
import com.qykj.finmaster.third.workWx.manager.WorkWxGroupChatManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageKeywordHitManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关键词命中清单查询 + 批量 enrich（工单89 / WXKW-05 重构：从 controller 下沉 service）。
 *
 * <p>零改表：群名/发言人名一律 join {@code work_wx_message}/{@code work_wx_group_chat} 补出。
 * 仿工单82 批量 enrich：按页 msgId/roomId 去重后一次性 in 查询，不逐行查库。</p>
 */
@Service
public class WorkWxKeywordHitQueryServiceImpl {

    @Autowired
    private WorkWxMessageKeywordHitManager hitManager;
    @Autowired
    private WorkWxMessageManager messageManager;
    @Autowired
    private WorkWxGroupChatManager groupChatManager;

    /**
     * 命中清单分页（enrich 群名/发言人名 + chatName 群名筛选）。
     * @param canSeeSensitive 是否可见敏感命中（由 controller 鉴权后传入；false 则 is_sensitive=0 过滤）
     * @return {rows: [...], total: n}
     */
    public Map<String, Object> hits(Long tenantId, boolean canSeeSensitive, String ruleType, String chatName,
                                    String roomId, String start, String end, Integer pageNum, Integer pageSize) {
        Map<String, Object> resp = new LinkedHashMap<>();
        // 群名筛选 → 先按 chat_name LIKE 取 chatId 列表，再 roomId in(...)
        List<String> roomIdsByName = null;
        if (StringUtils.isNotBlank(chatName)) {
            roomIdsByName = groupChatManager.lambdaQuery()
                    .like(WorkWxGroupChat::getChatName, chatName.trim())
                    .list().stream()
                    .map(WorkWxGroupChat::getChatId)
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (roomIdsByName.isEmpty()) {
                resp.put("rows", new ArrayList<>());
                resp.put("total", 0L);
                return resp;
            }
        }

        Page<WorkWxMessageKeywordHit> page = ruleHitWrapper(tenantId, canSeeSensitive, ruleType, roomId, roomIdsByName, start, end)
                .orderByDesc(WorkWxMessageKeywordHit::getHitTime)
                .page(new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 20 : pageSize));
        List<WorkWxMessageKeywordHit> records = page.getRecords();

        // 批量 enrich：收集去重 msgId/roomId，一次性查 work_wx_message 与 work_wx_group_chat 补群名/发言人名
        Set<String> msgIds = new HashSet<>();
        Set<String> roomIds = new HashSet<>();
        for (WorkWxMessageKeywordHit h : records) {
            if (StringUtils.isNotBlank(h.getMsgId())) msgIds.add(h.getMsgId());
            if (StringUtils.isNotBlank(h.getRoomId())) roomIds.add(h.getRoomId());
        }
        Map<String, WorkWxMessage> msgMap = new HashMap<>();
        if (!msgIds.isEmpty()) {
            for (WorkWxMessage m : messageManager.lambdaQuery().in(WorkWxMessage::getMsgId, msgIds).list()) {
                if (m != null && m.getMsgId() != null) msgMap.putIfAbsent(m.getMsgId(), m);
            }
        }
        Map<String, String> groupNameMap = new HashMap<>();
        if (!roomIds.isEmpty()) {
            for (WorkWxGroupChat g : groupChatManager.lambdaQuery().in(WorkWxGroupChat::getChatId, roomIds).list()) {
                if (g != null && g.getChatId() != null) groupNameMap.put(g.getChatId(), g.getChatName());
            }
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (WorkWxMessageKeywordHit h : records) {
            WorkWxMessage m = h.getMsgId() == null ? null : msgMap.get(h.getMsgId());
            // 群名：message.chatName → work_wx_group_chat.chatName → roomId
            String cn = m != null ? m.getChatName() : null;
            if (StringUtils.isBlank(cn)) cn = groupNameMap.get(h.getRoomId());
            if (StringUtils.isBlank(cn)) cn = h.getRoomId();
            String fromName = m != null ? m.getFromName() : null;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", h.getId());
            row.put("msgId", h.getMsgId());
            row.put("roomId", h.getRoomId());
            row.put("chatName", cn);
            row.put("fromName", fromName);
            row.put("fromType", h.getFromType());
            row.put("ruleType", h.getRuleType());
            row.put("hitKeyword", h.getHitKeyword());
            row.put("hitTime", h.getHitTime());
            row.put("sensitive", h.getSensitiveFlag());
            rows.add(row);
        }
        resp.put("rows", rows);
        resp.put("total", page.getTotal());
        return resp;
    }

    private LambdaQueryChainWrapper<WorkWxMessageKeywordHit> ruleHitWrapper(
            Long tenantId, boolean canSeeSensitive, String ruleType, String roomId,
            List<String> roomIdsByName, String start, String end) {
        LambdaQueryChainWrapper<WorkWxMessageKeywordHit> w =
                hitManager.lambdaQuery().eq(WorkWxMessageKeywordHit::getTenantId, tenantId);
        if (StringUtils.isNotBlank(ruleType)) w.eq(WorkWxMessageKeywordHit::getRuleType, ruleType);
        if (StringUtils.isNotBlank(roomId)) w.eq(WorkWxMessageKeywordHit::getRoomId, roomId);
        if (roomIdsByName != null && !roomIdsByName.isEmpty()) w.in(WorkWxMessageKeywordHit::getRoomId, roomIdsByName);
        if (StringUtils.isNotBlank(start)) w.ge(WorkWxMessageKeywordHit::getHitTime, start);
        if (StringUtils.isNotBlank(end)) w.le(WorkWxMessageKeywordHit::getHitTime, end);
        if (!canSeeSensitive) w.eq(WorkWxMessageKeywordHit::getSensitiveFlag, 0); // 敏感命中对非管理员不可见
        return w;
    }
}
