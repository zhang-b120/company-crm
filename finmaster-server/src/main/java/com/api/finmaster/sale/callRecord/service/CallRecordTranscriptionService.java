package com.qykj.finmaster.sale.callRecord.service;

import com.qykj.finmaster.sale.callRecord.entity.CallRecord;
import com.qykj.finmaster.sale.callRecord.manager.CallRecordManager;
import com.qykj.finmaster.third.baidu.service.BaiduSpeechService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 通话录音转写编排服务
 * 异步调用腾讯云语音识别，更新转写状态和文字；转写完成后自动触发质量分析
 */
@Slf4j
@Service
public class CallRecordTranscriptionService {

    @Autowired
    private CallRecordManager callRecordManager;

    @Autowired
    private BaiduSpeechService baiduSpeechService;

    @Autowired
    private CallRecordQualityService callRecordQualityService;

    @Autowired
    private com.qykj.finmaster.workorder.manager.WorkOrderFieldRecordManager workOrderFieldRecordManager;

    /**
     * 异步触发转写
     */
    @Async
    public void transcribeAsync(Long callRecordId) {
        CallRecord record = callRecordManager.getById(callRecordId);
        if (record == null || record.getRecordingLocalPath() == null || record.getRecordingUrl() == null) {
            log.warn("转写跳过: 记录 {} 不存在或无录音文件", callRecordId);
            return;
        }

        // 标记为转写中
        CallRecord statusUpdate = new CallRecord();
        statusUpdate.setId(callRecordId);
        statusUpdate.setTranscriptionStatus(1);
        callRecordManager.updateById(statusUpdate);

        try {
            String text = baiduSpeechService.recognize(record.getRecordingLocalPath(), record.getRecordingUrl());

            // 转写成功
            CallRecord result = new CallRecord();
            result.setId(callRecordId);
            result.setTranscriptionStatus(2);
            result.setTranscriptionText(text);
            callRecordManager.updateById(result);
            log.info("转写完成: callRecordId={}, textLength={}", callRecordId, text != null ? text.length() : 0);

            // 转写完成后自动触发质量分析
            callRecordQualityService.analyzeAsync(callRecordId);

            // 回填到对应的"电话跟进"记录（如有 work_order_field_record 关联了此 call_record_id）
            try {
                java.util.List<com.qykj.finmaster.workorder.domain.WorkOrderFieldRecord> linked =
                        workOrderFieldRecordManager.lambdaQuery()
                                .eq(com.qykj.finmaster.workorder.domain.WorkOrderFieldRecord::getCallRecordId, callRecordId)
                                .list();
                for (var r : linked) {
                    com.qykj.finmaster.workorder.domain.WorkOrderFieldRecord upd =
                            new com.qykj.finmaster.workorder.domain.WorkOrderFieldRecord();
                    upd.setId(r.getId());
                    upd.setContent("【电话转写】\n" + (text != null ? text : "(转写为空)"));
                    workOrderFieldRecordManager.updateById(upd);
                }
            } catch (Exception ignore) {
                log.warn("回填电话转写到 field_record 失败: callRecordId={}", callRecordId, ignore);
            }

        } catch (Exception e) {
            log.error("转写失败: callRecordId={}", callRecordId, e);
            CallRecord fail = new CallRecord();
            fail.setId(callRecordId);
            fail.setTranscriptionStatus(3);
            fail.setTranscriptionText("转写失败: " + e.getMessage());
            callRecordManager.updateById(fail);
        }
    }
}
