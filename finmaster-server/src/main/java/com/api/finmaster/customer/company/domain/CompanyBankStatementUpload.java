package com.qykj.finmaster.customer.company.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

/**
 * 公司银行流水上传统计
 */
@Data
@TableName("company_bank_statement_upload")
public class CompanyBankStatementUpload {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司ID */
    @TableField("company_id")
    private Long companyId;

    /** 账期 yyyy-MM-dd */
    @TableField("period")
    private String period;

    /** 上传文件数量 */
    @TableField("upload_count")
    private Integer uploadCount;

    /** 是否本期无银行流水（0-否 1-是；勾选后视为本期已完成） */
    @TableField("no_statement")
    private Integer noStatement;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;
}
