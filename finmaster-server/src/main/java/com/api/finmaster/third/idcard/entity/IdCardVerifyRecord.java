package com.qykj.finmaster.third.idcard.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 身份证二要素核验结果缓存（工单90 / IDV-03）。
 * 一致(resultCode=01)的核验结果缓存，按 (tenant_id, name, id_card) 唯一；命中则不再调 API、不再计费。
 *
 * @Author zhb
 * @Date 2026-06-24
 */
@Data
@TableName("id_card_verify_record")
public class IdCardVerifyRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("name")
    private String name;

    @TableField("id_card")
    private String idCard;

    /** 核验结果码：01 一致（仅缓存 01） */
    @TableField("result_code")
    private String resultCode;

    /** 是否一致 0-否 1-是 */
    @TableField("consistent")
    private Integer consistent;

    /** 性别 1男2女 */
    @TableField("gender")
    private String gender;

    /** 生日 yyyyMMdd */
    @TableField("birthday")
    private String birthday;

    @TableField("age")
    private String age;

    @TableField("province")
    private String province;

    @TableField("city")
    private String city;

    @TableField("country")
    private String country;

    /** 平台订单号（对账用） */
    @TableField("order_number")
    private String orderNumber;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("verify_time")
    private Date verifyTime;

    /** 来源：SLSJ */
    @TableField("source")
    private String source;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
