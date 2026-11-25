package com.qykj.finmaster.oa.overtime.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 加班记录 oa_overtime_record
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_overtime_record")
public class OaOvertimeRecord extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("real_name")
    private String realName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @TableField("overtime_date")
    private Date overtimeDate;

    /** 加班小时数 */
    @TableField("overtime_hours")
    private BigDecimal overtimeHours;

    /** 折算调休天数 */
    @TableField("comp_days")
    private BigDecimal compDays;

    /** 状态：UNUSED=未使用 / USED=已使用 */
    @TableField("status")
    private String status;

    @TableLogic
    private Integer deleted;
}
