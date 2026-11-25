package com.qykj.finmaster.registerAddress.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 注册地址列表响应
 */
@Data
public class GetRegisterAddressListResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 723984752398475234L;

    private Long id;

    private String address;

    private Integer status;

    private Integer enable;

    private String remark;

    private String createByName;

    private LocalDateTime createTime;
}

