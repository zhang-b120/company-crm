package com.qykj.finmaster.common.utils.third.req;

import com.beust.jcommander.internal.Maps;
import lombok.Data;
import org.springframework.cglib.beans.BeanMap;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-07-12
 * @Description
 */
@Data
public class CreateAccountSetReq implements Serializable {
    private static final long serialVersionUID = -3653110382422251156L;

    /**
     * 公司名称
     */
    private String companyName;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 会计准则 默认小企业会计准则
     */
    private String accountStandard = "SMALL_BUSINESS";

    /**
     * 账套模板ID
     */
    private Long astId;

    /**
     * 账套启用日期
     */
    private LocalDate enableDate;

    /**
     * 账套截止日期（财务系统字段名 effectiveDate，与 updateAccountSetEffectiveDate 保持一致）
     */
    private LocalDate effectiveDate;

    /**
     * 增值税种类
     *  小规模纳税人、 一般纳税人
     */
    private String vatType;

    /**
     * 电税密码（电子税务局登录密码）
     */
    private String electricityTaxCode;

    /**
     * 个税密码（个人所得税申报用，与 electricityTaxCode 区分）
     */
    private String individualTaxCode;

    /**
     * 省份
     */
    private String province;

    /**
     * 电税手机号
     */
    private String electricityTaxPhone;

    /**
     * 是否零申报
     */
    private Boolean zeroDeclarations;

    /**
     * 账套创建人
     */
    private Long creatorId;

    /**
     * 凭证是否需要审核
     */
    private Boolean voucherReviewed;

    /**
     * 是否个体工商户（财务系统字段：individual_business / individualBusiness，Boolean）
     */
    private Boolean individualBusiness;

    /**
     * 国标 GB/T 4754《国民经济行业分类》代码，如 "C13"、"I65"、"A0123"。
     * 数据源：天眼查 categoryCodeFourth → categoryCodeThird → categoryCodeSecond → categoryCodeFirst（取最细一级非空）
     */
    private String industryCode;

    /**
     * 行业中文名称，与 industryCode 一一对应。数据源：天眼查 industry 字段。
     */
    private String industryName;

    /**
     * 经营范围（财务系统字段：business_scope，TEXT；从天眼查 businessScope 字段填充）
     */
    private String businessScope;

    /**
     * 企业类型 / 组织形式（如"有限责任公司"、"个体工商户"、"其他股份有限公司(上市)"）。
     * 数据源：天眼查 companyOrgType 字段。
     * 注意：财务系统侧接收字段名需与此 key（companyOrgType）一致，若财务字段名不同请同步调整。
     */
    private String companyOrgType;

    public Map<String, Object> toBeanMap() {
        Map<String, Object> map = Maps.newHashMap();
        BeanMap.create(this).forEach((k, v) -> {
            map.put(k.toString(), v);
        });
        return map;
    }
}