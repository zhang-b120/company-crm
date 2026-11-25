package com.qykj.finmaster.oa.announcement.domain.req;

import lombok.Data;

/**
 * 查询公告列表请求
 */
@Data
public class GetAnnouncementListReq {

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页数量 */
    private Integer pageSize = 10;

    /** 标题模糊查询 */
    private String title;

    /** 分类 */
    private String category;

    /** 状态 */
    private String status;

    /** 优先级 */
    private String priority;
}
