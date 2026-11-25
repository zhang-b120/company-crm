package com.qykj.finmaster.customer.company.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.annotation.Excel;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 客户公司操作日志对象 customer_company_log
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("customer_company_log")
public class CustomerCompanyLog extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 客户ID */
    @Excel(title = "客户ID")
    @TableField("customer_id")
    private Long customerId;

    /** 日志类型（1创建 2修改 3删除） */
    @Excel(title = "操作类型", readConverterExp = "1=创建,2=修改,3=删除")
    @TableField("log_type")
    private Integer logType;

    /** 操作内容 */
    @Excel(title = "操作内容")
    @TableField("content")
    private String content;

    /** 操作人ID */
    @Excel(title = "操作人ID")
    @TableField("operator_id")
    private Long operatorId;

    /** 操作人姓名 */
    @Excel(title = "操作人姓名")
    @TableField("operator_name")
    private String operatorName;

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("customerId", getCustomerId())
            .append("logType", getLogType())
            .append("content", getContent())
            .append("operatorId", getOperatorId())
            .append("operatorName", getOperatorName())
            .append("createTime", getCreateTime())
            .toString();
    }
} 