package com.qykj.finmaster.system.dict.entity.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 新增字典数据请求实体
 * @Author zhb
 * @Date 2025-08-04
 * @Description
 */
@Data
public class AddDictDataReq implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /** 字典类型ID */
    private Long typeId;

    /** 字典标签 */
    private String label;

    /** 字典键值 */
    private String value;

    /** 字典排序 */
    private Integer sort;

    /** 状态（1启用 0停用） */
    private Integer status;

    /** 备注 */
    private String remark;
} 