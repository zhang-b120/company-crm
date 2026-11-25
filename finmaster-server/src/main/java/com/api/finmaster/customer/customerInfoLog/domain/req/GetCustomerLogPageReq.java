package com.qykj.finmaster.customer.customerInfoLog.domain.req;

import com.baomidou.mybatisplus.annotation.TableField;
import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * @Author zhb
 * @Date 2025-07-31
 * @Description
 */
@Data
public class GetCustomerLogPageReq extends PageEntity {

    @Serial
    private static final long serialVersionUID = -8888536653689041383L;

    /** 日志ID */
    private Long id;

    /** 客户ID */
    @TableField("customer_id")
    private Long customerId;

    /** 客户类型（1私海 2公海） */
    private Integer customerType;

    /** 日志类型（1创建 2跟进 3转化 4编辑 5转入公海 6领取 7分配） */
    private Integer logType;
}