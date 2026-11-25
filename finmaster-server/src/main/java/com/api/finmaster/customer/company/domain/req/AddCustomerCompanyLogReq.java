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
public class AddCustomerCompanyLogReq implements Serializable {
    @Serial
    private static final long serialVersionUID = -7686711637383186913L;

    /** 客户ID */
    private Long customerId;

    /** 日志类型（1创建 2修改 3删除） */
    private Integer logType;

    /** 操作内容 */
    private String content;

    /** 操作人ID */
    private Long operatorId;

    /** 操作人姓名 */
    private String operatorName;
}