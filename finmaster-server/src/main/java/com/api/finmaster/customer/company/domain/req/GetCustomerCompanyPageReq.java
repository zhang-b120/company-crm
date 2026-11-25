package com.qykj.finmaster.customer.company.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * @Author zhb
 * @Date 2025-06-07
 * @Description
 */
@Data
public class GetCustomerCompanyPageReq extends PageEntity {

    @Serial
    private static final long serialVersionUID = -8125577257758436520L;
    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 税号
     */
    private String taxNumber;

    /**
     * 客户姓名
     */
    private String customerName;

    /**
     * 客户手机号
     */
    private String phone;

    /**
     * 是否验证税密 0-未验证 1-已验证
     */
    private Integer isVerifyTax;

    /**
     * 是否开通发票服务 0-未验证 1-已验证
     */
    private Integer isInvoicing;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 公司状态：COOPERATING/CANCELLED/DEREGISTERED
     */
    private String companyStatus;

    /**
     * 工商状态：在业/存续/吊销/注销（企查查 status）
     */
    private String regStatus;

}