package com.qykj.finmaster.oa.payment.domain.req;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 新增付款申请请求
 */
@Data
public class AddPaymentReq {

    /** 收款人名称 */
    private String payeeName;

    /** 收款人银行 */
    private String payeeBank;

    /** 收款人账号 */
    private String payeeAccount;

    /** 付款金额 */
    private BigDecimal amount;

    /** 付款用途 */
    private String purpose;

    /** 附件 */
    private String attachments;
}
