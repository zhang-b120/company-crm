package com.qykj.finmaster.third.workWx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 企业微信会话存档拉取游标（断点续传）。工单 73 / WXMSG-01。
 *
 * @Date 2026-06-19
 */
@Data
@TableName("work_wx_msg_cursor")
public class WorkWxMsgCursor {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 游标键（如 default，预留多 secret/多主体） */
    @TableField("cursor_key")
    private String cursorKey;

    /** 已成功入库的最大 seq；下次从此 +1 拉 */
    @TableField("last_seq")
    private Long lastSeq;

    /** 时间型采集游标已处理到的最大命中时间（工单92 / WXKW-07，发票采集用） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("last_hit_time")
    private Date lastHitTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private Date updateTime;
}
