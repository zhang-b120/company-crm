package com.qykj.finmaster.system.bizConfig.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 业务模块配置（V2.0.23 新增）
 * 通用键值对配置，按 module 分组
 */
@Data
@TableName("sys_biz_config")
public class SysBizConfig implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 业务模块: lead/customer/order/contract/task/renewal/company... */
    private String module;

    /** 全局唯一配置键，如 lead.recycle.no_follow_days */
    @TableField("config_key")
    private String configKey;

    /** 配置项显示名 */
    @TableField("config_name")
    private String configName;

    /** 说明文案 */
    private String description;

    /** 值类型：switch / radio / checkbox / number / text / group */
    @TableField("value_type")
    private String valueType;

    /**
     * 可选项 / 子字段 schema（JSON 字符串）
     * - radio/checkbox: [{"label":"xx","value":"xx"}]
     * - group:          [{"key":"xx","label":"xx","type":"switch|number|text|radio|checkbox","description":"","min":0,"max":...,"options":[...]}]
     */
    private String options;

    /**
     * 默认值
     * - 标量类型：字符串（"1" / "14" / "abc"）
     * - group 类型：JSON 对象字符串 {"enabled":"1","no_follow_days":"14",...}
     */
    @TableField("default_value")
    private String defaultValue;

    /** 当前值（null 时取默认值）；group 类型同样存 JSON 对象字符串 */
    @TableField("config_value")
    private String configValue;

    /** 同模块内排序 */
    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("update_by")
    private String updateBy;

    @TableField("update_time")
    private Date updateTime;

    @TableField("create_time")
    private Date createTime;

    @TableLogic
    private Integer deleted;
}
