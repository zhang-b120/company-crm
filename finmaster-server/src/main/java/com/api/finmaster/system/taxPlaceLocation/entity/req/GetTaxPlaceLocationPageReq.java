package com.qykj.finmaster.system.taxPlaceLocation.entity.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * @Author zhb
 * @Date 2025-08-12
 * @Description
 */
@Data
public class GetTaxPlaceLocationPageReq extends PageEntity {
    @Serial
    private static final long serialVersionUID = -5919980268725066388L;

    private String code;

    private String name;
}