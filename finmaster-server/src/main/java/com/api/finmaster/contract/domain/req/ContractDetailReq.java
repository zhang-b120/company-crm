package com.qykj.finmaster.contract.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 合同明细请求 DTO
 */
@Data
public class ContractDetailReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 订单编号
     */
    private String orderNo;

    /**
     * 订单明细ID
     */
    private Long orderDetailId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 注册地址ID
     */
    private Long companyAddressId;

    /**
     * 备注
     */
    private String remark;
}

