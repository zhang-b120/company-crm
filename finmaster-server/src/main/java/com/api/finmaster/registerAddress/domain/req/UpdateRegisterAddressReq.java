package com.qykj.finmaster.registerAddress.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 编辑注册地址请求
 */
@Data
public class UpdateRegisterAddressReq implements Serializable {

    @Serial
    private static final long serialVersionUID = -2983475293847523987L;

    /**
     * 地址ID
     */
    private Long id;

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

