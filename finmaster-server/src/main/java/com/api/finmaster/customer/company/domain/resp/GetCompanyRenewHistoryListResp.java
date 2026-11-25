package com.qykj.finmaster.customer.company.domain.resp;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @Author zhb
 * @Date 2025-08-26
 * @Description
 */
@Data
public class GetCompanyRenewHistoryListResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 4220433060564848874L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 续费开始日期
     */
    private LocalDate renewEffectiveDate;

    /**
     * 续费截止日期
     */
    private LocalDate renewEndDate;

    /**
     * 续费类型 0-正常续费 1-提前续费 2-延期续费
     */
    private Integer renewType;

    /**
     * 续费原因
     */
    private String renewReason;

    /**
     * 续费金额
     */
    private BigDecimal renewAmount;

    /**
     * 备注
     */
    private String remark;

}