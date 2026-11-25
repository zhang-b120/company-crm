package com.qykj.finmaster.third.workWx.service;

import com.qykj.finmaster.third.workWx.entity.WorkWxKeywordRule;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessage;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessageKeywordHit;
import com.qykj.finmaster.third.workWx.manager.WorkWxKeywordRuleManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageKeywordHitManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 企业微信关键词增量抓取（工单 77 / WXKW-01）。
 *
 * <p>只读 {@code work_wx_message.content} 跑规则，命中写 {@code work_wx_message_keyword_hit} 并标 keyword_hit=1，
 * 扫过置 keyword_scanned=1 避免重复。<b>不自动生成工单/商机、不关联客户。</b></p>
 */
@Slf4j
@Service
public class WorkWxKeywordScanServiceImpl {

    /** 每轮扫描消息上限 */
    private static final int BATCH = 500;

    @Autowired
    private WorkWxMessageManager messageManager;
    @Autowired
    private WorkWxKeywordRuleManager ruleManager;
    @Autowired
    private WorkWxMessageKeywordHitManager hitManager;

    /**
     * 扫描未扫过的 text 消息（增量）。
     * @return 处理汇总
     */
    public Map<String, Object> scanNew() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 启用规则 + 预拆关键词
        List<WorkWxKeywordRule> rules = ruleManager.lambdaQuery()
                .eq(WorkWxKeywordRule::getEnabled, 1)
                .list();
        List<RuleWords> compiled = new ArrayList<>();
        if (rules != null) {
            for (WorkWxKeywordRule r : rules) {
                List<String> words = splitKeywords(r.getKeywords());
                if (!words.isEmpty()) compiled.add(new RuleWords(r, words));
            }
        }

        // 取未扫描的 text 消息一批
        List<WorkWxMessage> msgs = messageManager.lambdaQuery()
                .eq(WorkWxMessage::getMsgType, "text")
                .and(w -> w.eq(WorkWxMessage::getKeywordScanned, 0).or().isNull(WorkWxMessage::getKeywordScanned))
                .isNotNull(WorkWxMessage::getContent)
                .ne(WorkWxMessage::getContent, "")
                .last("limit " + BATCH)
                .list();

        int scanned = 0, hits = 0, hitMsgs = 0;
        if (msgs != null) {
            for (WorkWxMessage m : msgs) {
                String content = m.getContent();
                boolean anyHit = false;
                if (StringUtils.isNotBlank(content) && !compiled.isEmpty()) {
                    for (RuleWords rw : compiled) {
                        for (String kw : rw.words) {
                            if (content.contains(kw)) {
                                hitManager.save(buildHit(m, rw.rule, kw));
                                hits++;
                                anyHit = true;
                            }
                        }
                    }
                }
                // 标记水位 + 命中
                m.setKeywordScanned(1);
                m.setKeywordHit(anyHit ? 1 : 0);
                messageManager.updateById(m);
                scanned++;
                if (anyHit) hitMsgs++;
            }
        }

        result.put("ok", true);
        result.put("rules", compiled.size());
        result.put("scanned", scanned);
        result.put("hitMessages", hitMsgs);
        result.put("hits", hits);
        return result;
    }

    private WorkWxMessageKeywordHit buildHit(WorkWxMessage m, WorkWxKeywordRule rule, String kw) {
        WorkWxMessageKeywordHit hit = new WorkWxMessageKeywordHit();
        hit.setMessageId(m.getId());
        hit.setMsgId(m.getMsgId());
        hit.setRuleId(rule.getId());
        hit.setRuleType(rule.getRuleType());
        hit.setHitKeyword(kw);
        hit.setRoomId(m.getRoomId());
        hit.setFromType(m.getFromType());
        hit.setSensitiveFlag(rule.getSensitiveFlag() != null ? rule.getSensitiveFlag() : 0);
        hit.setSource("RULE");
        hit.setHitTime(m.getMsgTime());
        hit.setTenantId(m.getTenantId());
        return hit;
    }

    private List<String> splitKeywords(String keywords) {
        List<String> out = new ArrayList<>();
        if (StringUtils.isBlank(keywords)) return out;
        // 支持中英文逗号分隔
        for (String s : keywords.split("[,，]")) {
            String t = s.trim();
            if (!t.isEmpty()) out.add(t);
        }
        return out;
    }

    private static class RuleWords {
        final WorkWxKeywordRule rule;
        final List<String> words;
        RuleWords(WorkWxKeywordRule rule, List<String> words) { this.rule = rule; this.words = words; }
    }
}
