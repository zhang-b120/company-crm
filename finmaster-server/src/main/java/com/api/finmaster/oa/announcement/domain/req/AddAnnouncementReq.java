package com.qykj.finmaster.oa.announcement.domain.req;

import lombok.Data;

/**
 * 新增/修改公告请求
 */
@Data
public class AddAnnouncementReq {

    /** ID（修改时传） */
    private Long id;

    /** 公告标题 */
    private String title;

    /** 公告内容（富文本） */
    private String content;

    /** 分类 */
    private String category;

    /** 优先级: LOW/NORMAL/HIGH/URGENT */
    private String priority;

    /** 是否置顶（1=是） */
    private Integer pinned;

    /** 附件URL（JSON数组） */
    private String attachments;
}
