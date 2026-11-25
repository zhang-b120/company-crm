package com.qykj.finmaster.third.workWx.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;

/**
 * @Author zhb
 * @Date 2025-10-16
 * @Description
 */
@Data
@TableName("work_wx_group_chat")
public class WorkWxGroupChat extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 群聊Id
     */
    @TableField("chat_id")
    private String chatId;

    /**
     * 群聊名称
     */
    @TableField("chat_name")
    private String chatName;

    /**
     * 群主
     */
    @TableField("chat_owner")
    private String chatOwner;

    /**
     * 群聊明细
     */
    @TableField("chat_detail")
    private String chatDetail;

    /**
     * 群聊状态 0 - 跟进人正常 1 - 跟进人离职 2 - 离职继承中 3 - 离职继承完成
     */
    @TableField("chat_status")
    private Integer chatStatus;

    /**
     * 是否删除 0-否 1-是
     */
    @TableLogic
    private Integer deleted;

}