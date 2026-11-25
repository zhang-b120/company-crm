package com.qykj.finmaster.third.tianyancha.domain.resp;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * /tianyancha/search 的单条结果
 * = 企查查模糊搜索的一家公司 + 天眼查 baseinfo + 天眼查 holders
 */
@Data
public class TycCompanySearchResp implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 企业名（来自天眼查基础信息，fallback 到企查查模糊搜索） */
    private String name;
    /** 统一社会信用代码 */
    private String creditCode;
    /** 法定代表人 */
    private String legalPersonName;
    /** 企业状态 */
    private String regStatus;
    /** 成立日期（展示用字符串 yyyy-MM-dd） */
    private String estiblishDate;
    /** 注册地址 */
    private String regLocation;
    /** 行业 */
    private String industry;

    /** 完整天眼查基础信息（"更多"区域使用） */
    private TycBaseInfoResp baseInfo;

    /** 股东列表 */
    private List<TycHolderItem> stockholders;

    /** 是否一般纳税人（来自天眼查 /m/taxpayer/2.0；items 非空 = true） */
    private Boolean generalTaxpayer;

    /** 纳税人类型字典码：general-tax / small-scale-tax；前端可直接绑公司表 companyType */
    private String companyType;
}
