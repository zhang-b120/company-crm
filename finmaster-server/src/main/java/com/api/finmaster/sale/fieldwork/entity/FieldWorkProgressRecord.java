package com.qykj.finmaster.sale.fieldwork.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("field_work_progress_record")
public class FieldWorkProgressRecord extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("task_id")
    private Long taskId;

    @TableField("content")
    private String content;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("submit_time")
    private Date submitTime;

    @TableField("submitter_id")
    private Long submitterId;

    @TableField("submitter_name")
    private String submitterName;

    @TableField("tenant_id")
    private Long tenantId;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    /** 图片列表（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private List<FieldWorkProgressImage> images;
}
