package com.qykj.finmaster.system.appletUser.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-06-03
 * @Description
 */
@Data
public class GetAppletUserInfoResp implements Serializable {

    private static final long serialVersionUID = -3982066696450899877L;
    /**
     * 公司名称
     */
    private String name;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 是否验证税密 0-未验证 1-已验证
     */
    private Integer isVerifyTax;

    /**
     * 是否开通发票服务 0-未开通 1-已开通
     */
    private Integer isInvoicing;

    /**
     * 账套ID
     */
    private String accountSetsId;

    /**
     * 当前账期
     */
    private String currentAccountDate;

    /**
     * 启用账期
     */
    private String enableDate;

    /**
     * 是否为小程序用户
     */
    private Boolean isAppletUser = Boolean.FALSE;

    /**
     * 公司ID
     */
    private Long companyId;
}