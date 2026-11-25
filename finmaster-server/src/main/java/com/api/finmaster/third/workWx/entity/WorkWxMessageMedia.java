package com.qykj.finmaster.third.workWx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 企业微信会话存档媒体文件（工单 75 / WXMSG-03）。库存路径，不存二进制。
 */
@Data
@TableName("work_wx_message_media")
public class WorkWxMessageMedia {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联 work_wx_message.id */
    @TableField("message_id")
    private Long messageId;

    @TableField("msg_id")
    private String msgId;

    /** image/voice/video/file */
    @TableField("media_type")
    private String mediaType;

    @TableField("sdk_file_id")
    private String sdkFileId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_size")
    private Long fileSize;

    /** 落盘路径 / 访问 URL */
    @TableField("storage_path")
    private String storagePath;

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
}
