package com.qykj.finmaster.third.tianyancha.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 天眼查企业基础信息缓存 tyc_company_info
 */
@Data
@TableName("tyc_company_info")
public class TycCompanyInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tyc_id")
    private Long tycId;

    private String name;
    private String alias;

    @TableField("credit_code")
    private String creditCode;

    @TableField("tax_number")
    private String taxNumber;

    @TableField("reg_number")
    private String regNumber;

    @TableField("org_number")
    private String orgNumber;

    @TableField("legal_person_name")
    private String legalPersonName;

    @TableField("legal_person_type")
    private Integer legalPersonType;

    @TableField("reg_status")
    private String regStatus;

    @TableField("reg_capital")
    private String regCapital;

    @TableField("reg_capital_currency")
    private String regCapitalCurrency;

    @TableField("actual_capital")
    private String actualCapital;

    @TableField("actual_capital_currency")
    private String actualCapitalCurrency;

    @TableField("company_org_type")
    private String companyOrgType;

    @TableField("comp_form")
    private Integer compForm;

    private String industry;
    private String category;

    @TableField("category_big")
    private String categoryBig;

    @TableField("category_middle")
    private String categoryMiddle;

    @TableField("category_small")
    private String categorySmall;

    @TableField("category_code_first")
    private String categoryCodeFirst;

    @TableField("category_code_second")
    private String categoryCodeSecond;

    @TableField("category_code_third")
    private String categoryCodeThird;

    @TableField("category_code_fourth")
    private String categoryCodeFourth;

    @TableField("reg_location")
    private String regLocation;

    @TableField("reg_location_half_width")
    private String regLocationHalfWidth;

    private String base;
    private String city;
    private String district;

    @TableField("district_code")
    private String districtCode;

    @TableField("economic_function_zone1")
    private String economicFunctionZone1;

    @TableField("economic_function_zone2")
    private String economicFunctionZone2;

    @TableField("reg_institute")
    private String regInstitute;

    @TableField("estiblish_time")
    private Date estiblishTime;

    @TableField("approved_time")
    private Date approvedTime;

    @TableField("from_time")
    private Date fromTime;

    @TableField("to_time")
    private Date toTime;

    @TableField("revoke_date")
    private Date revokeDate;

    @TableField("revoke_reason")
    private String revokeReason;

    @TableField("cancel_date")
    private Date cancelDate;

    @TableField("cancel_reason")
    private String cancelReason;

    @TableField("staff_num_range")
    private String staffNumRange;

    @TableField("social_staff_num")
    private Integer socialStaffNum;

    @TableField("is_micro_ent")
    private Integer isMicroEnt;

    @TableField("above_scale")
    private String aboveScale;

    @TableField("percentile_score")
    private Integer percentileScore;

    @TableField("bond_name")
    private String bondName;

    @TableField("used_bond_name")
    private String usedBondName;

    @TableField("bond_num")
    private String bondNum;

    @TableField("bond_type")
    private String bondType;

    @TableField("business_scope")
    private String businessScope;

    private String tags;

    @TableField("english_name")
    private String englishName;

    @TableField("history_names")
    private String historyNames;

    @TableField("history_name_list")
    private String historyNameList;

    @TableField("tyc_update_time")
    private Date tycUpdateTime;

    @TableField("raw_json")
    private String rawJson;

    @TableField("create_time")
    private Date createTime;

    @TableField("update_time")
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
