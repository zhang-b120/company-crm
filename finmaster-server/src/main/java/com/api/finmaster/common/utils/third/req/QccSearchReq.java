package com.qykj.finmaster.common.utils.third.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 企查查查询公司信息参数
 * @Author zhb
 * @Date 2025-05-16
 * @Description
 */
@Data
public class QccSearchReq implements Serializable {

    private static final long serialVersionUID = -8809184064259007405L;
    /**
     *搜索关键字（如企业名、人名、产品名、地址、电话等
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