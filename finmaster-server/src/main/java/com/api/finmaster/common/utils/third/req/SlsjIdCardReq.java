package com.qykj.finmaster.common.utils.third.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 随联数聚 身份证二要素核验请求参数（工单88 / IDV-01）。仿 QccSearchReq。
 *
 * @Author zhb
 * @Date 2026-06-23
 */
@Data
public class SlsjIdCardReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** 姓名 */
    private String name;

    /** 身份证号（18 位） */
    private String idCard;

    /** 商户订单号（可空，空则工具内自动生成） */
    private String orderId;
}
