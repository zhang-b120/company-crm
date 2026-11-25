package com.qykj.finmaster.customer.company.domain.req;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @Author zhb
 * @Date 2025-07-29
 * @Description
 */
@Data
public class GetCustomerCompanyLogReq implements Serializable {
    @Serial
    private static final long serialVersionUID = -7812432671150117463L;

    /** 客户ID */
    private Long customerId;

    /** 日志类型（1创建 2修改 3删除） */
    private Integer logType;

}