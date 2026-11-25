package com.qykj.finmaster.sale.fieldwork.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 外勤任务图片实体，映射 field_work_task_image 表
 */
@Data
@TableName("field_work_task_image")
public class FieldWorkTaskImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联外勤任务ID */
    @TableField("task_id")
    private Long taskId;

    /** 图片访问URL */
    @TableField("image_url")
    private String imageUrl;

    /** 图片文件名 */
    @TableField("image_name")
    private String imageName;

    /** 上传人ID */
    @TableField("uploader_id")
    private Long uploaderId;

    /** 上传人名称 */
    @TableField("uploader_name")
    private String uploaderName;

    /** 上传时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("upload_time")
    private Date uploadTime;

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
