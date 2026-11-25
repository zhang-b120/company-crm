package com.qykj.finmaster.third.qcc.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-05-16
 * @Description
 */
@Data
public class GetQccCompanyInfoReq implements Serializable {

    private static final long serialVersionUID = 5311680152928611357L;

    private String wechatOpenId;

    /**
     *  搜索关键字（如企业名、人名、产品名、地址、电话等
     */
    private String searchKey;

    /**
     * 省份Code(6位行政区划代码)
     */
    private String provinceCode;

    /**
     * 城市Code(6位行政区划代码)
     */
    private String cityCode;

    /**
     * 页码，默认第一页
     */
    private String pageIndex;

}