package com.qykj.finmaster.customer.company.domain.req;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-08-25
 * @Description
 */
@Data
public class AddCompanyRenewReq implements Serializable {

    @Serial
    private static final long serialVersionUID = -6431406210727008548L;

    /**
     * 公司id
     */
    private Long companyServiceId;

    /**
     * 续费生效日期
     */
    private LocalDate renewEffectiveDate;

    /**
     * 续费截止日期
     */
    private LocalDate renewEndDate;

    /**
     * 续费类型 0-正常续费 1-提前续费 2-延期续费 3-未续费
     */
    private Integer renewType;

    /**
     * 续费金额
     */
    private BigDecimal renewAmount;

    /**
     * 续费原因
     */
    private String renewReason;

    /**
     * 备注
     */
    private String remark;
}