package com.qykj.finmaster.oa.trip.domain.req;

import lombok.Data;

/**
 * 查询出差列表请求
 */
@Data
public class GetBusinessTripListReq {

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 10;

    /** 状态过滤 */
    private String status;

    /** 申请人（管理员查询用） */
    private String applicant;
}
