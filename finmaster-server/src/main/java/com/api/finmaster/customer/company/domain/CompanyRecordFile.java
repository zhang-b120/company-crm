package com.qykj.finmaster.customer.company.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.time.LocalDateTime;

/**
 * 客户公司信息对象 customer_company
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_record_file")
public class CompanyRecordFile extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 3045313589231313373L;
    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 维护记录id
     */
    @TableField("company_record_id")
    private Long companyRecordId;

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
} 