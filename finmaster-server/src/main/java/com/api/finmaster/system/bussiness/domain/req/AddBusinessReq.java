package com.qykj.finmaster.system.bussiness.domain.req;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 新增业务请求
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddBusinessReq implements Serializable {

    @Serial
    private static final long serialVersionUID = 3459872349875234987L;

    /**
     * 业务编码
     */
    private String businessCode;

    /**
     * 业务名称
     */
    private String businessName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 合同是否必须关联订单 0-否 1-是
     */
    private Integer contractRelatedOrder;

    /**
     * 判断转为续费订单次数阈值
     */
    private Integer checkRenewalCount;

    /**
     * 存在时不可新增的业务ID，逗号分隔多选（如 2,3,5）
     */
    private String checkAddBusinessId;
}

