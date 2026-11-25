package com.qykj.finmaster.system.dict.entity.resp;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-07-01
 * @Description
 */
@Data
public class GetDictDataListResp implements Serializable {
    private static final long serialVersionUID = -2693497476121722286L;

    /**
     * 字典标签
     */
    private String label;

    /**
     * 字典键值
     */
    private String value;

    /**
     * 字典排序
     */
    private Integer sort;

    /**
     * 状态（1启用 0停用）
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 父Code
     */
    private String typeCode;
}