package com.qykj.finmaster.oa.announcement.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 公告阅读记录 oa_announcement_read
 */
@Data
@TableName("oa_announcement_read")
public class OaAnnouncementRead implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("tenant_id")
    private Long tenantId;

    @TableField("announcement_id")
    private Long announcementId;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("real_name")
    private String realName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("read_time")
    private Date readTime;
}
