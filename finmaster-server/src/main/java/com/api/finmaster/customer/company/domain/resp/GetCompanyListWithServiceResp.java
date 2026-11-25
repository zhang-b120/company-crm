package com.qykj.finmaster.customer.company.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @Author zhb
 * @Date 2025-06-07
 * @Description
 */
@Data
public class GetCompanyListWithServiceResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 5254941262630028022L;
    /**
     * 公司ID
     */
    private Long id;

    /** 企业微信openid */
    private String openid;

    /** 公司名称 */
    private String companyName;

    /** 税号 */
    private String taxNumber;

    /** 所属客户 */
    private String customerName;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 销售负责人
     */
    private String saleOwnerUserName;

    /**
     * 管家负责人
     */
    private String butlerOwnerUserName;

    /**
     * 公司类型
     */
    private String companyType;

    /**
     * 公司类型名称
     */
    private String companyTypeName;

    /**
     * 是否记账户 0-否 1-是
     */
    private Integer isBookkeeping;

    /**
     * 双免个体 0-否 1-是
     */
    private Integer isDoubleExemption;

    /**
     * 是否创建账套 0-未创建 1-已创建
     */
    private Integer isHavaAccountSet;

    /** 是否验证税密 0-未验证 1-已验证 */
    private Integer isVerifyTax;

    /**
     * 服务数量
     */
    private Integer serviceSum;

    /**
     * 消费金额
     */
    private BigDecimal payAmountSum;

    /**
     * 最新评论
     */
    private String newFollowContent;

    /**
     * 银行流水是否已上传 0-未上传 1-已上传
     */
    private Integer bankStatementUploaded;

    /**
     * 业务类型名称（聚合 company_service 上的 sys_business.business_name，多业务用「，」分隔）
     */
    private String businessNames;
}