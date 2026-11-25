package com.qykj.finmaster.system.dict.entity.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * 字典类型分页查询请求实体
 * @Author zhb
 * @Date 2025-08-04
 * @Description
 */
@Data
public class GetDictTypePageReq extends PageEntity {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典名称 */
    private String name;

    /** 字典编码 */
    private String code;
} 