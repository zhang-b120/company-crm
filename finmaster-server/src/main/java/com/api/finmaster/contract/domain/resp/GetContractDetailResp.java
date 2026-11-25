package com.qykj.finmaster.contract.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 合同详情响应
 */
@Data
public class GetContractDetailResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;

    private String contractNo;

    private Long companyId;

    private String companyName;

    private Long businessId;

    private String businessName;

    private String contractType;

    private LocalDateTime contractStartTime;

    private LocalDateTime contractEndTime;

    private BigDecimal contractPrice;

    private Integer haveOrder;

    private Integer haveAddress;

    private String status;

    private String remark;

    private String createByName;

    private LocalDateTime createTime;

    private String updateByName;

    private LocalDateTime updateTime;

    /**
     * 合同明细列表
     */
    private List<ContractDetailInfoResp> detailList;
}

