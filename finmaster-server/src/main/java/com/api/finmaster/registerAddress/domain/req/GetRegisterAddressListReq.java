package com.qykj.finmaster.registerAddress.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * 注册地址列表查询请求
 */
@Data
public class GetRegisterAddressListReq extends PageEntity {

    @Serial
    private static final long serialVersionUID = -4837523984752398752L;

    /**
     * 地址关键字
     */
    private String address;

    /**
     * 状态 0-未使用 1-使用中
     */
    private Integer status;

    /**
     * 是否启用 0-否 1-是
     */
    private Integer enable;
}

