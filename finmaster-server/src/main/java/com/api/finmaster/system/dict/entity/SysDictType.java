package com.qykj.finmaster.system.dict.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

import java.io.Serial;

/**
 * 字典类型实体类
 */
@Data
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -1964385492813771573L;
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 字典名称
     */
    @TableField("name")
    private String name;

    /**
     * 字典编码
     */
    @TableField("code")
    private String code;

    /**
     * 状态（1启用 0停用）
     */
    @TableField("status")
    private Integer status;
}
