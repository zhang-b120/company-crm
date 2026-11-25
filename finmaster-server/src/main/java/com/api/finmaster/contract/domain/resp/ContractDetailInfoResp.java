package com.qykj.finmaster.contract.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 合同明细响应
 */
@Data
public class ContractDetailInfoResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String orderNo;

    private Long orderDetailId;

    private Long productId;

    private String productName;

    private String productNumber;

    private Long companyAddressId;

    private String address;

    private String remark;
}

