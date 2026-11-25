package com.qykj.finmaster.system.taxPlaceLocation.entity.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-08-12
 * @Description
 */
@Data
public class GetTaxPlaceLocationPageResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 4871267116487871635L;

    private String code;

    private String name;
}