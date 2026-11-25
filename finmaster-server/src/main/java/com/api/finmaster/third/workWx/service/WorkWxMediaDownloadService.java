package com.qykj.finmaster.third.workWx.service;

import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.core.domain.constansts.FileConstants;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessage;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessageMedia;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageMediaManager;
import com.qykj.finmaster.third.workWx.sdk.WeWorkFinanceSdk;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

/**
 * 企业微信会话存档媒体分片下载（工单 75 / WXMSG-03）。
 *
 * <p>按 sdkfileid 用 {@code GetMediaData} 分片下载（用上次 outindexbuf 作下次 indexbuf，直到 is_finish=1），
 * 落盘后只把路径写入 {@code work_wx_message_media}。下载失败不影响文本消息入库（由调用方 try/catch）。</p>
 */
@Slf4j
@Service
public class WorkWxMediaDownloadService {

    /** 媒体落盘目录（参照 FileConstants 现有附件落盘范式） */
    private static final String MEDIA_DIR = "E:\\qykj\\file\\wecomMedia";
    /** 访问 URL 前缀 */
    private static final String MEDIA_URL = FileConstants.BASE_URL + "common/file/wecomMedia/";
    /** 单分片下载超时(秒) */
    private static final long TIMEOUT = 30L;
    /** 防御：单文件最大累计字节（50MB），超过则中止避免异常超大流 */
    private static final long MAX_BYTES = 50L * 1024 * 1024;

    @Autowired
    private WorkWxMessageMediaManager mediaManager;

    /**
     * 下载某条媒体消息的文件并落库（已下载过则跳过）。
     * @param entity 已入库的消息
     * @param msg    解密明文 JSON
     * @param sdk    已 Init 的 SDK 句柄（由同步流程传入，复用同一会话）
     */
    public void downloadForMessage(WorkWxMessage entity, JSONObject msg, long sdk) {
        String type = msg.getString("msgtype");
        if (StringUtils.isBlank(type)) return;
        JSONObject mo = msg.getJSONObject(type);
        if (mo == null) return;
        String sdkFileId = mo.getString("sdkfileid");
        if (StringUtils.isBlank(sdkFileId)) return;

        // 去重：同 msgId + type 已记录则跳过
        WorkWxMessageMedia exist = mediaManager.lambdaQuery()
                .eq(WorkWxMessageMedia::getMsgId, entity.getMsgId())
                .eq(WorkWxMessageMedia::getMediaType, type)
                .last("limit 1").one();
        if (exist != null && StringUtils.isNotBlank(exist.getStoragePath())) return;

        String fileName = resolveFileName(type, mo, entity.getMsgId());
        byte[] bytes;
        try {
            bytes = downloadAll(sdk, sdkFileId);
        } catch (Throwable e) {
            log.warn("[msgaudit-media] 分片下载失败 msgId={} type={}：{}", entity.getMsgId(), type, e.getMessage());
            return;
        }
        if (bytes == null || bytes.length == 0) return;

        String storagePath;
        try {
            Path dir = Paths.get(MEDIA_DIR);
            Files.createDirectories(dir);
            Path target = dir.resolve(fileName);
            Files.write(target, bytes);
            storagePath = MEDIA_URL + fileName;
        } catch (Throwable e) {
            log.warn("[msgaudit-media] 落盘失败 msgId={}：{}", entity.getMsgId(), e.getMessage());
            return;
        }

        WorkWxMessageMedia media = exist != null ? exist : new WorkWxMessageMedia();
        media.setMessageId(entity.getId());
        media.setMsgId(entity.getMsgId());
        media.setMediaType(type);
        media.setSdkFileId(sdkFileId);
        media.setFileName(fileName);
        media.setFileSize((long) bytes.length);
        media.setStoragePath(storagePath);
        media.setTenantId(entity.getTenantId());
        if (media.getCreateTime() == null) media.setCreateTime(new Date());
        mediaManager.saveOrUpdate(media);
    }

    /** 分片循环：用上次 outindexbuf 作下次 indexbuf，直到 is_finish==1。 */
    private byte[] downloadAll(long sdk, String sdkFileId) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        String indexbuf = "";
        int guard = 0;
        while (true) {
            if (++guard > 100000) throw new IllegalStateException("分片次数异常，中止");
            long media = WeWorkFinanceSdk.newMediaData();
            try {
                int ret = WeWorkFinanceSdk.getMediaData(sdk, indexbuf, sdkFileId, "", "", TIMEOUT, media);
                if (ret != 0) throw new IllegalStateException("GetMediaData 返回码=" + ret);
                byte[] chunk = WeWorkFinanceSdk.getData(media);
                if (chunk != null && chunk.length > 0) {
                    bos.write(chunk, 0, chunk.length);
                    if (bos.size() > MAX_BYTES) throw new IllegalStateException("媒体超过大小上限");
                }
                int finish = WeWorkFinanceSdk.isMediaDataFinish(media);
                String outIndex = WeWorkFinanceSdk.getOutIndexBuf(media);
                if (finish == 1) break;
                indexbuf = outIndex;
            } finally {
                try { WeWorkFinanceSdk.freeMediaData(media); } catch (Throwable ignore) { }
            }
        }
        return bos.toByteArray();
    }

    private String resolveFileName(String type, JSONObject mo, String msgId) {
        String safeMsgId = msgId == null ? "unknown" : msgId.replaceAll("[^A-Za-z0-9_-]", "_");
        if ("file".equals(type)) {
            String fn = mo.getString("filename");
            if (StringUtils.isNotBlank(fn)) {
                return safeMsgId + "_" + fn.replaceAll("[\\\\/:*?\"<>|]", "_");
            }
            return safeMsgId + "_file";
        }
        switch (type) {
            case "image": return safeMsgId + ".jpg";
            case "voice": return safeMsgId + ".amr";
            case "video": return safeMsgId + ".mp4";
            default: return safeMsgId + ".bin";
        }
    }
}
