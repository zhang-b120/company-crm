package com.qykj.finmaster.third.tianyancha.domain.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * 天眼查模糊搜索返回的单条数据（/services/open/search/2.0 items 数组元素）
 * 字段数量按天眼查文档口径给全；未返回的字段留 null 即可
 */
@Data
public class TycSearchItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String creditCode;
    private String legalPersonName;
    /** 企业类型：1-公司 2-个体工商户等（与 baseinfo.type 同口径） */
    private Integer type;
    private String regStatus;
    private String regLocation;
    /**
     * 成立时间：模糊搜索接口返回的是字符串（如 "2021-04-22 00:00:00.0"），
     * 无成立日期的主体（如村委会）会返回 "-"，故用 String 承载，避免 fastjson parseLong("-") 报错。
     * （注意：BASEINFO 详情接口的 estiblishTime 才是毫秒时间戳 Long，见 TycBaseInfoResp）
     */
    private String estiblishTime;
    private String regCapital;
    /** 注册号 */
    private String regNumber;
    private String orgNumber;
    private String matchType;
}
