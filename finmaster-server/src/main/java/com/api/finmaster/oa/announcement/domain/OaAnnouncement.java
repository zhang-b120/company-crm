package com.qykj.finmaster.oa.announcement.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 公告通知 oa_announcement
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("oa_announcement")
public class OaAnnouncement extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 租户ID */
    @TableField("tenant_id")
    private Long tenantId;

    /** 公告标题 */
    @TableField("title")
    private String title;

    /** 公告内容（富文本） */
    @TableField("content")
    private String content;

    /** 分类（字典: oa_announcement_category） */
    @TableField("category")
    private String category;

    /** 优先级: LOW/NORMAL/HIGH/URGENT */
    @TableField("priority")
    private String priority;

    /** 状态: DRAFT/PUBLISHED/ARCHIVED */
    @TableField("status")
    private String status;

    /** 是否置顶（1=是） */
    @TableField("pinned")
    private Integer pinned;

    /** 实际发布时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("publish_time")
    private Date publishTime;

    /** 附件URL（JSON数组） */
    @TableField("attachments")
    private String attachments;

    /** 发布人账号 */
    @TableField("publisher")
    private String publisher;

    /** 发布人姓名 */
    @TableField("publisher_name")
    private String publisherName;

    /** 逻辑删除 */
    @TableLogic
    private Integer deleted;
}
