package com.qykj.finmaster.workorder.domain.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 任务单列表查询（分页 + 排序字段继承自 PageEntity：pageNum/pageSize/orderByColumn/isAsc）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class QueryWorkOrderReq extends PageEntity {
    private static final long serialVersionUID = 1L;

    private String taskType;
    private String status;
    private String taskNo;
    private Long companyId;
    private Long customerId;

    /** 标题模糊匹配 */
    private String title;

    /** 客户名称模糊匹配（后端解析为 customerId 列表过滤） */
    private String customerName;

    /** 公司名称模糊匹配（后端解析为 companyId 列表过滤） */
    private String companyName;

    /** 当前负责人 username（查"我的待办"用） */
    private String currentOwnerUser;

    /** 发起人 username（查"我发起的"用） */
    private String ownerUser;

    /** 「我的全部」过滤：current_owner_user = mineUser OR owner_user = mineUser */
    private String mineUser;

    /** 只看顶级任务，不看子任务 */
    private Boolean topLevelOnly;
}
