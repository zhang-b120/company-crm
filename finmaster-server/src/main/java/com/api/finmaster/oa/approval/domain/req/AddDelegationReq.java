package com.qykj.finmaster.oa.approval.domain.req;

import lombok.Data;

/**
 * 新增/编辑审批委托请求
 */
@Data
public class AddDelegationReq {

    /** 主键ID（更新时传入） */
    private Long id;

    /** 被委托人账号 */
    private String delegateTo;

    /** 被委托人姓名 */
    private String delegateToName;

    /** 委托开始日期（yyyy-MM-dd） */
    private String startDate;

    /** 委托结束日期（yyyy-MM-dd） */
    private String endDate;

    /** 业务类型（多个用逗号分隔，为空表示全部） */
    private String businessTypes;

    /** 是否启用（1=启用，0=禁用） */
    private Integer enabled;
}
