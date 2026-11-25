package com.qykj.finmaster.registerAddress.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 新增注册地址请求
 */
@Data
public class AddRegisterAddressReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 2948572398475234987L;

    /**
     * 地址
     */
    private String address;

    /**
     * 是否启用 0-否 1-是
     */
    private Integer enable;

    /**
     * 备注
     */
    private String remark;
}

