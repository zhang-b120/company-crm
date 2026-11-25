package com.qykj.finmaster.oa.approval.domain.req;

import lombok.Data;

/**
 * 查询审批委托列表请求
 */
@Data
public class GetDelegationListReq {

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 10;
}
