package com.qykj.finmaster.system.serviceMaintenance.domain.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-06
 * @Description
 */
@Data
public class ServiceProductInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 8331889404460312274L;

    /**
     * 关联产品信息ID
     */
    private Long productId;

    /**
     * 关联产品信息名称
     */
    private String productName;
}