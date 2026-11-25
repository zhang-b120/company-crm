package com.qykj.finmaster.oa.approval.domain.req;

import lombok.Data;

/**
 * 保存审批定义请求
 */
@Data
public class SaveDefinitionReq {

    /** 主键ID（更新时传入） */
    private Long id;

    /** 审批名称 */
    private String name;

    /** 审批类型（LEAVE/EXPENSE/PAYMENT等） */
    private String type;

    /** 审批节点配置JSON */
    private String nodesJson;

    /** 是否启用 */
    private Boolean enabled;
}
