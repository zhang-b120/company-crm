package com.qykj.finmaster.customer.company.domain.resp;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-06-03
 * @Description
 */
@Data
public class GetCustomerCompanyListResp implements Serializable {
    private static final long serialVersionUID = -7411491443116637782L;

    /** 主键ID */
    private Long id;

    /** 企业微信openid */
    private String openid;

    /** 公司名称 */
    private String companyName;

    /** 税号 */
    private String taxNumber;

    /** 法人名 */
    private String legalName;

    /** 是否启用 */
    private Integer enabled;

    /**
     * 是否验证税密 0-未验证 1-已验证
     */
    private Integer isVerifyTax;

    /**
     * 账套ID
     */
    private Long accountSetsId;

    /**
     * 当前账期
     */
    private String currentAccountDate;

    /**
     * 启用账期
     */
    private String enableDate;

    /**
     * 公司类型
     */
    private String companyTypeLabel;

    /**
     * 体验账套ID
     */
    private String experienceAccountSetId;
}