package com.qykj.finmaster.customer.company.domain.resp;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 按税号查询公司信息（小程序 / PC 共用）—— 仅查本地数据库
 * 命中：返回本对象（含 公司基本信息 + 工商信息 + 股东信息）
 * 未命中：data 直接返回 null
 */
@Data
public class CompanyLookupResp implements Serializable {
    private static final long serialVersionUID = 1L;

    // ============ 公司基本信息 ============
    /** 公司名称 */
    private String companyName;
    /** 统一社会信用代码 / 税号 */
    private String taxNumber;
    /** 公司类型（字典值，如 small-scale-tax / general-tax） */
    private String companyType;

    // ============ 工商信息 ============
    /** 法人姓名 */
    private String legalName;
    /** 法人证件号 */
    private String legalIdCard;
    /** 法人手机号 */
    private String legalPhone;
    /** 注册地址 */
    private String registeredAddress;
    /** 成立日期 yyyy-MM-dd */
    private String establishedDate;
    /** 工商状态 */
    private String regStatus;
    /** 省份 */
    private String province;
    /** 税局所在地 */
    private String taxDistrict;
    /** 是否开票 0/1 */
    private Integer isInvoicing;
    /** 是否个体工商户 0/1 */
    private Integer isIndividual;
    /** 公司状态 */
    private String companyStatus;

    // ============ 股东信息 ============
    private List<Shareholder> shareholders;

    @Data
    public static class Shareholder implements Serializable {
        private static final long serialVersionUID = 1L;
        private Long id;
        /** 姓名 */
        private String name;
        /** 证件号 */
        private String idCard;
        /** 手机号 */
        private String phone;
        /** 持股比例（%） */
        private BigDecimal shareholdingRatio;
        /** 备注 */
        private String remark;
    }
}
