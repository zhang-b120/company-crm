package com.qykj.finmaster.system.productType.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class UpdateProductTypeReq implements Serializable {

    @Serial
    private static final long serialVersionUID = -5895245697627128169L;
    private Long id;
    /**
     * 类型名称
     */
    private String name;

    /**
     * 类型编码
     */
    private String code;

    /**
     * 描述
     */
    private String description;
}
