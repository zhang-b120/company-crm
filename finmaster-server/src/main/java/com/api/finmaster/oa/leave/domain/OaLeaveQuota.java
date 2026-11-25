package com.qykj.finmaster.oa.leave.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 假期配额 oa_leave_quota
 */
@Data
@TableName("oa_leave_quota")
public class OaLeaveQuota implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("leave_type")
    private String leaveType;

    @TableField("year")
    private Integer year;

    /** 年度总额度（天） */
    @TableField("total_days")
    private BigDecimal totalDays;

    /** 已使用天数 */
    @TableField("used_days")
    private BigDecimal usedDays;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private Date updateTime;

    @TableLogic
    private Integer deleted;
}
