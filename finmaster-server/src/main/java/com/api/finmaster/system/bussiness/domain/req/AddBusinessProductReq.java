package com.qykj.finmaster.system.bussiness.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 新增业务产品关联请求
 */
@Data
public class AddBusinessProductReq implements Serializable {

    @Serial
    private static final long serialVersionUID = -129384752398475234L;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 需要关联的产品ID列表
     */
    private List<Long> productIdList;
}

