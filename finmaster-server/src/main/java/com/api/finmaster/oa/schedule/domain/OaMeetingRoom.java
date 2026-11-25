package com.qykj.finmaster.oa.schedule.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 会议室 oa_meeting_room
 */
@Data
@TableName("oa_meeting_room")
public class OaMeetingRoom implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 会议室名称 */
    @TableField("name")
    private String name;

    /** 位置 */
    @TableField("location")
    private String location;

    /** 容纳人数 */
    @TableField("capacity")
    private Integer capacity;

    /** 设施描述 */
    @TableField("facilities")
    private String facilities;

    /** 是否启用（0=禁用，1=启用） */
    @TableField("enabled")
    private Integer enabled;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private Date updateTime;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
