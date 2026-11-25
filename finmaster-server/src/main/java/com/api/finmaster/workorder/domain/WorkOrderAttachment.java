package com.qykj.finmaster.workorder.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 任务单附件 work_order_attachment
 */
@Data
@TableName("work_order_attachment")
public class WorkOrderAttachment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("work_order_id")
    private Long workOrderId;

    @TableField("step_id")
    private Long stepId;

    /** OLD_ACCOUNT / FIELD_PHOTO / FIELD_AUDIO / CONTRACT_SCAN / OTHER_RESULT / OTHER */
    @TableField("file_type")
    private String fileType;

    @TableField("file_url")
    private String fileUrl;

    @TableField("file_name")
    private String fileName;

    @TableField("file_size")
    private Long fileSize;

    @TableField("mime_type")
    private String mimeType;

    @TableField("upload_user")
    private String uploadUser;

    @TableField("upload_user_name")
    private String uploadUserName;

    @TableField("upload_time")
    private Date uploadTime;

    @TableLogic
    private Integer deleted;
}
