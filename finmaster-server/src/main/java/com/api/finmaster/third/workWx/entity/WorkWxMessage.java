package com.qykj.finmaster.third.workWx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * 企业微信群聊消息（会话内容存档解密后落库）。工单 73 / WXMSG-01。
 *
 * @Date 2026-06-19
 */
@Data
@TableName("work_wx_message")
public class WorkWxMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话存档 msgid，去重用（唯一索引） */
    @TableField("msg_id")
    private String msgId;

    /** 会话存档游标序号 */
    @TableField("seq")
    private Long seq;

    /** 群 roomid（= work_wx_group_chat.chat_id）；单聊为空 */
    @TableField("room_id")
    private String roomId;

    /** 落库时按 room_id 冗余的群名 */
    @TableField("chat_name")
    private String chatName;

    /** 发送人 userid / external_userid */
    @TableField("from_user")
    private String fromUser;

    /** 解析出的发送人名（解析不到则存 userid） */
    @TableField("from_name")
    private String fromName;

    /** 1=企业成员 2=外部成员 0=未知 */
    @TableField("from_type")
    private Integer fromType;

    /** text/image/voice/file/link/... */
    @TableField("msg_type")
    private String msgType;

    /** text 取 text.content；非文本存 JSON 摘要 / 媒体占位 */
    @TableField("content")
    private String content;

    /** 整条解密明文 JSON（阶段1新增，便于阶段3关键词/排查） */
    @TableField("raw_content")
    private String rawContent;

    /** 接收人列表（tolist JSON，阶段1新增） */
    @TableField("to_list")
    private String toList;

    /** 是否含媒体 0-否 1-是（阶段1新增，交 WXMSG-03 下载） */
    @TableField("has_media")
    private Integer hasMedia;

    /** 是否命中关键词 0-否 1-是（阶段3新增） */
    @TableField("keyword_hit")
    private Integer keywordHit;

    /** 是否已扫描关键词 0-否 1-是（阶段3新增，增量扫描水位） */
    @TableField("keyword_scanned")
    private Integer keywordScanned;

    /** 由 msgtime(epoch ms) 转换 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("msg_time")
    private Date msgTime;

    /** 租户ID（应用层赋值） */
    @TableField("tenant_id")
    private Long tenantId;

    /** 逻辑删除 0-否 1-是 */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;
}
