package com.qykj.finmaster.customer.customerInfo.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 客户信息对象 customer_info
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GetCustomerInfoReq extends PageEntity {

    @Serial
    private static final long serialVersionUID = 993654691824094877L;
    /**
     * 客户名称
     */
    private String customerName;

    /** 客户类型（1私海 2公海） */
    private Integer customerType;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 状态（1未跟进 2跟进中 3已转化 4已失效）
     */
    private Integer status;

    /**
     * 负责人姓名
     */
    private String ownerUserName;

    /**
     * 是否成交
     */
    private Boolean isTransaction;

    /**
     * 沟通次数
     */
    private Integer followCount;
}
