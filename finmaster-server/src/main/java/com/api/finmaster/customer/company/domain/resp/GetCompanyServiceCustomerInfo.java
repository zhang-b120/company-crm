package com.qykj.finmaster.customer.company.domain.resp;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @Author zhb
 * @Date 2025-08-25
 * @Description
 */
@Data
public class GetCompanyServiceCustomerInfo implements Serializable {
    @Serial
    private static final long serialVersionUID = 4993763881280839539L;

    private Long id;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 联系人
     */
    private String contactName;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 客户类型（1私海 2公海）
     */
    private Integer customerType;

    /**
     * 创建时间
     */
    private Date createTime;
}