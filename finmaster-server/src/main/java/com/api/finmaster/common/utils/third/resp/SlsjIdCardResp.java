package com.qykj.finmaster.common.utils.third.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 随联数聚 身份证二要素核验响应（工单88 / IDV-01）。仿 QccSearchResp。
 *
 * @Author zhb
 * @Date 2026-06-23
 */
@Data
public class SlsjIdCardResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 接口返回码（0=成功，其余见错误码处理） */
    private Integer code;

    /** 接口返回信息 */
    private String msg;

    /** 姓名与身份证号是否一致（仅 resultCode=01 为 true） */
    private Boolean consistent;

    /** 核验结果码：01 一致(收费) / 02 不一致(收费) / 03 不确定(不收费) / 04 失败(不收费) */
    private String resultCode;

    /** 平台订单号 */
    private String orderNumber;

    /** 商户订单号（回显） */
    private String orderId;

    /** 性别：1 男 2 女 */
    private String gender;

    /** 生日 yyyyMMdd */
    private String birthday;

    /** 年龄 */
    private String age;

    /** 省 */
    private String province;

    /** 市 */
    private String city;

    /** 县/区 */
    private String country;
}
