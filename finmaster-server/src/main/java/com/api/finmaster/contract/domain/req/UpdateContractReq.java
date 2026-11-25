package com.qykj.finmaster.contract.domain.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 编辑合同请求
 */
@Data
public class UpdateContractReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 合同ID
     */
    private Long id;

    /**
     * 公司ID
     */
    private Long companyId;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 合同类型 新增合同/续费合同
     */
    private String contractType;

    /**
     * 合同开始时间（仅日期）
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date contractStartTime;

    /**
     * 合同结束时间（仅日期）
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date contractEndTime;

    /**
     * 合同价格
     */
    private BigDecimal contractPrice;

    /**
     * 备注
     */
    private String remark;

    /**
     * 合同明细列表（覆盖式保存）
     */
    private List<ContractDetailReq> detailList;
}

