package com.qykj.finmaster.order.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serializable;
@Data
public class GetOrderLogInfoReq extends PageEntity implements Serializable {

    private static final long serialVersionUID = -8488708138939519276L;
    /** 日志ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 客户ID */
    private Long customerId;

    /** 日志类型（1创建订单 2编辑订单 3删除订单 4状态变更） */
    private String logType;

    /** 日志内容 */
    private String content;

    /** 操作人账号 */
    private String operator;

    /** 操作人姓名 */
    private String operatorName;
}
