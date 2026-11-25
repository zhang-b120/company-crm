package com.qykj.finmaster.third.baidu.service;

import com.google.gson.Gson;
import com.tencentcloudapi.asr.v20190614.AsrClient;
import com.tencentcloudapi.asr.v20190614.models.CreateRecTaskRequest;
import com.tencentcloudapi.asr.v20190614.models.CreateRecTaskResponse;
import com.tencentcloudapi.asr.v20190614.models.DescribeTaskStatusRequest;
import com.tencentcloudapi.asr.v20190614.models.DescribeTaskStatusResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语音识别服务（腾讯云录音文件识别标准版）
 * 异步提交 + 轮询结果，支持说话人分离
 */
@Slf4j
@Service
public class BaiduSpeechService {

    @Value("${third.tencent.asr.secret-id:}")
    private String secretId;

    @Value("${third.tencent.asr.secret-key:}")
    private String secretKey;

    /** 轮询间隔（毫秒） */
    private static final long POLL_INTERVAL_MS = 3000;
    /** 最大等待时间（毫秒），30 分钟，覆盖长录音场景 */
    private static final long MAX_WAIT_MS = 1_800_000;

    private final Gson gson = new Gson();

    private AsrClient createClient() {
        if (secretId == null || secretId.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("未配置腾讯云语音识别密钥，请在 application-local.yml 或环境变量 THIRD_TENCENT_ASR_SECRET_ID / THIRD_TENCENT_ASR_SECRET_KEY 中设置");
        }
        Credential credential = new Credential(secretId, secretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("asr.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        return new AsrClient(credential, "", clientProfile);
    }

    /**
     * 语音识别（标准版，支持说话人分离）
     * 使用 URL 模式提交，腾讯云自行下载音频，无文件大小限制
     *
     * @param localFilePath  本地音频文件路径（用于存在性校验）
     * @param recordingUrl   公网可访问的录音 URL
     * @return 识别出的文字（按说话人分段）
     */
    public String recognize(String localFilePath, String recordingUrl) throws Exception {
        File file = new File(localFilePath);
        if (!file.exists()) {
            throw new RuntimeException("录音文件不存在: " + localFilePath);
        }

        log.info("开始语音识别(标准版,URL模式): url={}, size={}bytes", recordingUrl, file.length());

        // 1. 提交识别任务
        Long taskId = createTask(recordingUrl);
        log.info("识别任务已提交: taskId={}", taskId);

        // 2. 轮询等待结果
        String resultJson = pollTaskResult(taskId);

        // 3. 解析结果，按说话人拼接
        return parseResult(resultJson);
    }

    /**
     * 提交录音文件识别任务（URL 模式，无大小限制）
     */
    private Long createTask(String recordingUrl) throws Exception {
        AsrClient client = createClient();

        CreateRecTaskRequest req = new CreateRecTaskRequest();
        req.setEngineModelType("16k_zh");
        req.setChannelNum(1L);
        req.setSourceType(0L); // 0 = URL 模式，腾讯云自行下载，支持任意大小
        req.setUrl(recordingUrl);
        req.setSpeakerDiarization(1L); // 开启说话人分离
        req.setSpeakerNumber(2L);      // 通话场景固定 2 人
        req.setResTextFormat(0L); // 0 = 普通文本
        req.setConvertNumMode(1L);

        CreateRecTaskResponse resp = client.CreateRecTask(req);
        if (resp.getData() == null || resp.getData().getTaskId() == null) {
            throw new RuntimeException("创建识别任务失败: " + gson.toJson(resp));
        }
        return resp.getData().getTaskId().longValue();
    }

    /**
     * 轮询任务状态直到完成
     */
    private String pollTaskResult(Long taskId) throws Exception {
        AsrClient client = createClient();
        long startTime = System.currentTimeMillis();

        while (true) {
            if (System.currentTimeMillis() - startTime > MAX_WAIT_MS) {
                throw new RuntimeException("语音识别超时（超过30分钟），taskId=" + taskId);
            }

            Thread.sleep(POLL_INTERVAL_MS);

            DescribeTaskStatusRequest req = new DescribeTaskStatusRequest();
            req.setTaskId(taskId);

            DescribeTaskStatusResponse resp = client.DescribeTaskStatus(req);
            if (resp.getData() == null) {
                continue;
            }

            Long status = resp.getData().getStatus();
            String statusStr = resp.getData().getStatusStr();
            log.debug("轮询任务状态: taskId={}, status={}, statusStr={}", taskId, status, statusStr);

            if ("success".equals(statusStr)) {
                log.info("识别任务完成: taskId={}", taskId);
                return resp.getData().getResult();
            } else if ("failed".equals(statusStr)) {
                throw new RuntimeException("语音识别失败: taskId=" + taskId + ", errorMsg=" + resp.getData().getErrorMsg());
            }
            // waiting / doing 继续轮询
        }
    }

    /**
     * 解析标准版识别结果（ResTextFormat=0 纯文本格式）
     * 每句格式: [start_time,end_time,speakerId] 文字
     * 例: [0:0.890,0:1.290,0] 喂。
     *
     * 合并同一说话人连续的句子，输出格式:
     * 【说话人1】喂。你好。可以听到是吗？
     * 【说话人2】喂，你好。
     */
    private String parseResult(String resultText) {
        if (resultText == null || resultText.isBlank()) {
            return "";
        }

        log.debug("识别结果原文: {}", resultText);

        // 匹配 [任意时间信息,speakerId] 文字
        Pattern pattern = Pattern.compile("\\[[^\\]]*,(\\d+)]\\s*(.+?)(?=\\s*\\[|$)");
        Matcher matcher = pattern.matcher(resultText.trim());

        StringBuilder sb = new StringBuilder();
        int lastSpeakerId = -1;

        while (matcher.find()) {
            int speakerId = Integer.parseInt(matcher.group(1));
            String text = matcher.group(2).trim();
            if (text.isEmpty()) continue;

            if (speakerId != lastSpeakerId) {
                // 切换说话人，换行并加标签
                if (sb.length() > 0) sb.append("\n");
                sb.append("【说话人").append(speakerId + 1).append("】");
                lastSpeakerId = speakerId;
            }
            sb.append(text);
        }

        if (sb.length() == 0) {
            // 正则未匹配到，降级返回原文
            return resultText;
        }

        return sb.toString();
    }

}
