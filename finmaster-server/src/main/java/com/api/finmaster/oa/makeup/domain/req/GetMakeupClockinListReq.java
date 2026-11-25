package com.qykj.finmaster.oa.makeup.domain.req;

import lombok.Data;

/**
 * 查询补卡列表请求
 */
@Data
public class GetMakeupClockinListReq {

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 10;

    /** 状态过滤 */
    private String status;

    /** 考勤日期 */
    private String attendanceDate;
}
