package com.qykj.finmaster.contract.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 合同对象 contract
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("contract")
public class Contract extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 合同ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 合同编码
     */
    @TableField("contract_no")
    private String contractNo;

    /**
     * 公司ID
     */
    @TableField("company_id")
    private Long companyId;

    /**
     * 业务类型ID
     */
    @TableField("business_id")
    private Long businessId;

    /**
     * 合同类型 新增合同/续费合同
     */
    @TableField("contract_type")
    private String contractType;

    /**
     * 合同开始时间（仅日期）
     */
    @TableField("contract_start_time")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date contractStartTime;

    /**
     * 合同结束时间（仅日期）
     */
    @TableField("contract_end_time")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date contractEndTime;

    /**
     * 合同价格
     */
    @TableField("contract_price")
    private BigDecimal contractPrice;

    /**
     * 是否存在订单 0-否 1-是
     */
    @TableField("have_order")
    private Integer haveOrder;

    /**
     * 是否存在地址 0-否 1-是
     */
    @TableField("have_address")
    private Integer haveAddress;

    /**
     * 合同状态 DRAFT-草稿 COMPLETED-已完成 TERMINATED-已终止 DISUSED-已废弃
     */
    @TableField("status")
    private String status;

    /**
     * 任务单标识 0-否 1-是（仅新增合同可转任务单）
     */
    @TableField("transfer_task")
    private Integer transferTask;

    /**
     * 已生成合同文件 URL（NULL 表示未生成；非空时禁止重复生成）
     */
    @TableField("contract_file_url")
    private String contractFileUrl;

    /**
     * 是否删除（0-否 1-是）
     */
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
