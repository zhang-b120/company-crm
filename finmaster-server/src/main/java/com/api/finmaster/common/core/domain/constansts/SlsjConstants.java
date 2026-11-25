package com.qykj.finmaster.common.core.domain.constansts;

/**
 * 随联数聚（身份证二要素实名核验）常量（工单88 / IDV-01）。
 * 仿 {@link QccConstants} 硬编码范式：不进数据库、不读 sys_biz_config。
 *
 * @Author zhb
 * @Date 2026-06-23
 */
public class SlsjConstants {

    /** 会员ID */
    public static final String MEMBER_ID = "80655157";

    /** 接口密钥 */
    public static final String APP_KEY = "4b1729aa629a41bc844f52a51f00538c";

    /** 接口版本 */
    public static final String VERSION = "v1";

    /** 身份证二要素核验地址 */
    public static final String ID_CARD_AUTH_URL = "https://api.slsj.com/api/idCard/id-card-auth/query";

    /** result=01 表示姓名与身份证号一致 */
    public static final String RESULT_CONSISTENT = "01";
}
