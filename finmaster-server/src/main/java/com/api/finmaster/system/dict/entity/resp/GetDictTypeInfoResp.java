package com.qykj.finmaster.system.dict.entity.resp;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-08-04
 * @Description
 */
@Data
public class GetDictTypeInfoResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 5200777625293485358L;

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

    /** 创建时间 */
    private LocalDateTime createTime;
}