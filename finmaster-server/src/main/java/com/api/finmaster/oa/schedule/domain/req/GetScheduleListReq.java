package com.qykj.finmaster.oa.schedule.domain.req;

import lombok.Data;

/**
 * 查询日程列表请求
 */
@Data
public class GetScheduleListReq {

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 10;

    /** 开始日期（yyyy-MM-dd） */
    private String startDate;

    /** 结束日期（yyyy-MM-dd） */
    private String endDate;

    /** 日程类型过滤：PERSONAL/MEETING/TASK */
    private String scheduleType;

    /** 用户ID（团队视图时查看指定用户） */
    private Long userId;
}
