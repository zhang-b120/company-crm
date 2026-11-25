package com.qykj.finmaster.third.tianyancha.constants;

/**
 * 天眼查开放平台常量
 */
public class TianyanchaConstants {

    /** 认证 Token（Authorization header） */
    public static final String TOKEN = "2e818d7d-c8bd-477f-bb07-bd6233d42fc6";

    /** 企业基础信息接口 */
    public static final String BASEINFO_URL = "http://open.api.tianyancha.com/services/open/ic/baseinfo/normal";

    /** 股东信息接口 */
    public static final String HOLDER_URL = "http://open.api.tianyancha.com/services/open/ic/holder/2.0";

    /** 企业模糊搜索接口 */
    public static final String SEARCH_URL = "http://open.api.tianyancha.com/services/open/search/2.0";

    /** 一般纳税人资格查询接口（query：keyword 公司名/id/注册号/统一信用代码） */
    public static final String TAXPAYER_URL = "http://open.api.tianyancha.com/services/open/m/taxpayer/2.0";

    /** 单次请求超时 30s */
    public static final int TIME_OUT = 30000;
}
