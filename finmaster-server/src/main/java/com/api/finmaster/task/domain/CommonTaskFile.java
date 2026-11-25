package com.qykj.finmaster.task.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-08-09
 * @Description
 */
@Data
@TableName("common_task_file")
@EqualsAndHashCode(callSuper=false)
public class CommonTaskFile extends BaseEntity {
    @Serial
    private static final long serialVersionUID = 2623688466712991091L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 公司ID
     */
    @TableField("company_id")
    private Long companyId;

    /**
     * 文件名
     */
    @TableField("file_name")
    private String fileName;

    /**
     * 源文件名称
     */
    @TableField("original_file_name")
    private String originalFileName;

    /**
     * 文件路径
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 文件类型
     */
    @TableField("file_suffix")
    private String fileSuffix;

    /**
     * 上传类型
     */
    @TableField("upload_type")
    private String uploadType;

    /**
     * 是否删除（0否 1是）
     */
    @TableLogic
    private Integer deleted;
    /**
     * 公司税号
     */
    @TableField("tax_number")
    private String taxNumber;
}