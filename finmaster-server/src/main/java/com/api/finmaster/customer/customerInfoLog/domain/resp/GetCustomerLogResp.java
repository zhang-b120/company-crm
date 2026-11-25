package com.qykj.finmaster.customer.customerInfoLog.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @Author zhb
 * @Date 2025-07-31
 * @Description
 */
@Data
public class GetCustomerLogResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1849936208082298861L;
    /** 日志ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 客户ID */
    private Long customerId;

    /** 客户类型（1私海 2公海） */
    private Integer customerType;

    /** 日志类型（1创建 2跟进 3转化 4编辑 5转入公海 6领取 7分配） */
    private Integer logType;

    /** 日志内容 */
    private String content;

    /** 跟进方式（当log_type=2时使用） */
    private String followType;

    /** 下次跟进时间（当log_type=2时使用） */
    private Date nextFollowTime;
}