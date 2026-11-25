package com.qykj.finmaster.system.bussiness.domain.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 业务详情响应
 */
@Data
public class GetBusinessDetailResp implements Serializable {

    @Serial
    private static final long serialVersionUID = -2394875234987523498L;

    private Long id;

    private String businessCode;

    private String businessName;

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

    private String remark;

    private String createByName;

    private LocalDateTime createTime;

    private String updateByName;

    private LocalDateTime updateTime;
}

