package com.qykj.finmaster.cs.profile.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 映射 financial.tax_officer 表，支持跨库 CRUD */
@Data
@TableName("financial.tax_officer")
public class FinancialTaxOfficer {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long accountSetId;

    /** 办税员姓名 */
    private String name;

    /** 登录手机号 */
    private String phone;

    /** 电税密码 */
    private String password;

    /** 登录渠道：officer/proxy/legal_person/ca */
    private String loginType;

    /** 法人身份证号（login_type=legal_person 时使用） */
    private String idCard;

    /** 是否默认办税员 */
    private Boolean isDefault;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
