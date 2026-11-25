package com.qykj.finmaster.system.taxPlaceLocation.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * @Author zhb
 * @Date 2025-08-12
 * @Description
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("tax_place_location")
public class TaxPlaceLocation extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1353246561536646451L;
    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 编码
     */
    @TableField("code")
    private String code;

    /**
     * 名称
     */
    @TableField("name")
    private String name;
}