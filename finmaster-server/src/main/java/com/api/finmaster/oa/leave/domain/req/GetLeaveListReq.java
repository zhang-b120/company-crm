package com.qykj.finmaster.oa.leave.domain.req;

import lombok.Data;

/**
 * 查询请假列表请求
 */
@Data
public class GetLeaveListReq {

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 10;

    /** 状态过滤 */
    private String status;

    /** 请假类型过滤 */
    private String leaveType;

    /** 申请人（管理员查询用） */
    private String applicant;
}
