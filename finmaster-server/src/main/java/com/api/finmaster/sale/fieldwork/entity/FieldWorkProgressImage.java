package com.qykj.finmaster.sale.fieldwork.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("field_work_progress_image")
public class FieldWorkProgressImage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("progress_id")
    private Long progressId;

    @TableField("task_id")
    private Long taskId;

    @TableField("image_url")
    private String imageUrl;

    @TableField("image_name")
    private String imageName;

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
