package com.qykj.finmaster.workorder.constants;

/**
 * 任务单来源类型
 */
public enum WorkOrderSourceType {
    CONTRACT,        // 合同（代账/工商/退费）
    ORDER,           // 订单（退费）
    PARENT,          // 父任务拆出来的子任务
    LEADER,          // 客服团队长下发（续费）
    COMPANY_SERVICE, // 公司服务（续费：source_id 存 company_service.id，便于按服务追溯多次续费）
    MANUAL           // 手工创建（付款等）
}
