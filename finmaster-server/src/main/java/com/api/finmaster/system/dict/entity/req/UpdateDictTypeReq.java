package com.qykj.finmaster.system.dict.entity.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新字典类型请求实体
 * @Author zhb
 * @Date 2025-08-04
 * @Description
 */
@Data
public class UpdateDictTypeReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典类型ID */
    private Long id;

    /** 字典名称 */
    private String name;

    /** 字典编码 */
    private String code;

    /** 状态（1启用 0停用） */
    private Integer status;

    /** 备注 */
    private String remark;
} 