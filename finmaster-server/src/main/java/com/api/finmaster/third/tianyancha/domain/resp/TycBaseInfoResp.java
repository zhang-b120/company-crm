package com.qykj.finmaster.third.tianyancha.domain.resp;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 天眼查 /ic/baseinfo/normal 的 result 对象
 * 字段名与 API 返回字段严格一致
 */
@Data
public class TycBaseInfoResp implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;                          // 天眼查企业 id
    private String name;                      // 企业名
    private String alias;                     // 简称
    private String creditCode;                // 统一社会信用代码
    private String taxNumber;                 // 纳税人识别号
    private String regNumber;                 // 注册号
    private String orgNumber;                 // 组织机构代码

    private String legalPersonName;           // 法人
    private Integer type;                     // 法人类型 1-人 2-公司

    private String regStatus;                 // 企业状态
    private String regCapital;                // 注册资本
    private String regCapitalCurrency;        // 注册资本币种
    private String actualCapital;             // 实收注册资金
    private String actualCapitalCurrency;     // 实收注册资本币种

    private String companyOrgType;            // 企业类型（组织形式）
    private Integer compForm;                 // 组成形式 1-个人 2-家庭

    private String industry;                  // 行业
    private IndustryAll industryAll;          // 行业四级分类

    private String regLocation;               // 注册地址
    private String regLocationHalfWidth;      // 注册地址（半角）
    private String base;                      // 省份简称
    private String city;                      // 市
    private String district;                  // 区
    private String districtCode;              // 行政区划代码
    private String economicFunctionZone1;     // 经济功能区1
    private String economicFunctionZone2;     // 经济功能区2

    private String regInstitute;              // 登记机关
    private Long estiblishTime;               // 成立日期（时间戳）
    private Long approvedTime;                // 核准时间
    private Long fromTime;                    // 经营开始时间
    private Long toTime;                      // 经营结束时间
    private Long revokeDate;                  // 吊销日期
    private String revokeReason;              // 吊销原因
    private Long cancelDate;                  // 注销日期
    private String cancelReason;              // 注销原因

    private String staffNumRange;             // 人员规模
    private Integer socialStaffNum;           // 参保人数
    private Integer isMicroEnt;               // 是否小微企业
    private String aboveScale;                // 规上企业
    private Integer percentileScore;          // 企业评分（万分制）

    private String bondName;                  // 股票名
    private String usedBondName;              // 股票曾用名
    private String bondNum;                   // 股票号
    private String bondType;                  // 股票类型

    private String businessScope;             // 经营范围
    private String tags;                      // 企业标签
    private String property3;                 // 英文名
    private String historyNames;              // 曾用名（拼接）
    private List<String> historyNameList;     // 曾用名数组

    private Long updateTimes;                 // 更新时间

    @Data
    public static class IndustryAll implements Serializable {
        private static final long serialVersionUID = 1L;
        private String category;              // 门类
        private String categoryBig;           // 大类
        private String categoryMiddle;        // 中类
        private String categorySmall;         // 小类
        private String categoryCodeFirst;     // 门类代码
        private String categoryCodeSecond;    // 大类代码
        private String categoryCodeThird;     // 中类代码
        private String categoryCodeFourth;    // 小类代码
    }
}
