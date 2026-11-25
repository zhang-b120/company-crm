package com.qykj.finmaster.sale.callRecord.service;

import com.qykj.finmaster.sale.callRecord.entity.CallRecord;
import com.qykj.finmaster.sale.callRecord.manager.CallRecordManager;
import com.qykj.finmaster.third.deepseek.service.DeepSeekService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 通话质量分析服务
 * 转写完成后异步调用 DeepSeek，对通话内容进行销售质量评估
 */
@Slf4j
@Service
public class CallRecordQualityService {

    @Autowired
    private CallRecordManager callRecordManager;

    @Autowired
    private DeepSeekService deepSeekService;

    /**
     * 异步触发质量分析
     */
    @Async
    public void analyzeAsync(Long callRecordId) {
        CallRecord record = callRecordManager.getById(callRecordId);
        if (record == null) {
            log.warn("质量分析跳过: 记录 {} 不存在", callRecordId);
            return;
        }
        if (record.getTranscriptionStatus() == null || record.getTranscriptionStatus() != 2) {
            log.warn("质量分析跳过: 记录 {} 转写未完成", callRecordId);
            return;
        }
        String text = record.getTranscriptionText();
        if (text == null || text.isBlank()) {
            log.warn("质量分析跳过: 记录 {} 转写文本为空", callRecordId);
            return;
        }
        // 文本过短（不足20字），说明通话内容不完整，跳过
        if (text.replaceAll("【说话人\\d+】", "").trim().length() < 20) {
            log.info("质量分析跳过: 记录 {} 有效文本过短", callRecordId);
            return;
        }

        // 标记为分析中
        CallRecord statusUpdate = new CallRecord();
        statusUpdate.setId(callRecordId);
        statusUpdate.setQualityStatus(1);
        callRecordManager.updateById(statusUpdate);

        try {
            String analysisJson = deepSeekService.analyzeCallQuality(text);

            // 从 JSON 中提取 overall_score
            int score = extractOverallScore(analysisJson);

            CallRecord result = new CallRecord();
            result.setId(callRecordId);
            result.setQualityStatus(2);
            result.setQualityScore(score);
            result.setQualityAnalysis(analysisJson);
            callRecordManager.updateById(result);

            log.info("质量分析完成: callRecordId={}, score={}", callRecordId, score);

        } catch (Exception e) {
            log.error("质量分析失败: callRecordId={}", callRecordId, e);
            CallRecord fail = new CallRecord();
            fail.setId(callRecordId);
            fail.setQualityStatus(3);
            callRecordManager.updateById(fail);
        }
    }

    private int extractOverallScore(String json) {
        try {
            // 简单提取 overall_score 字段，避免引入额外依赖
            int idx = json.indexOf("\"overall_score\"");
            if (idx < 0) return 0;
            int colon = json.indexOf(":", idx);
            int comma = json.indexOf(",", colon);
            int brace = json.indexOf("}", colon);
            int end = (comma > 0 && comma < brace) ? comma : brace;
            String numStr = json.substring(colon + 1, end).trim();
            return Integer.parseInt(numStr);
        } catch (Exception e) {
            log.warn("解析 overall_score 失败, json={}", json);
            return 0;
        }
    }
}
