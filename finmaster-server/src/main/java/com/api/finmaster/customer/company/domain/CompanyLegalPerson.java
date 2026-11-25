package com.qykj.finmaster.customer.company.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 公司法人/股东信息 company_legal_person
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company_legal_person")
public class CompanyLegalPerson extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司ID */
    @TableField("company_id")
    private Long companyId;

    /** 姓名 */
    @TableField("name")
    private String name;

    /** 身份证号 */
    @TableField("id_card")
    private String idCard;

    /** 证件类型 0身份证 1非身份证（非身份证免身份证二要素校验，工单95/IDV-05，工单99/IDV-09 改名） */
    @TableField("id_type")
    private Integer idType;

    /** 手机号 */
    @TableField("phone")
    private String phone;

    /** 持股比例（百分比，如 30.00 表示 30%） */
    @TableField("shareholding_ratio")
    private BigDecimal shareholdingRatio;

    /** 个人所得税密码 */
    @TableField("individual_tax_password")
    private String individualTaxPassword;

    /** 是否下载个人所得税App 0-否 1-是 */
    @TableField("individual_tax_app_downloaded")
    private Integer individualTaxAppDownloaded;

    /** 备注 */
    @TableField("remark")
    private String remark;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
