package com.qykj.finmaster.system.dict.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

import java.io.Serial;

/**
 * 字典数据实体类
 */
@Data
@TableName("sys_dict_data")
public class SysDictData extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 3348955950492090577L;
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /**
     * 字典类型ID
     */
    @TableField("type_id")
    private Long typeId;

    /**
     * 字典标签
     */
    @TableField("label")
    private String label;

    /**
     * 字典键值
     */
    @TableField("value")
    private String value;

    /**
     * 字典排序
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 状态（1启用 0停用）
     */
    @TableField("status")
    private Integer status;
}
