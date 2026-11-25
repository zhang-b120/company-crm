package com.qykj.finmaster.system.appletUser.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 小程序用户实体类
 */
@Data
@TableName("sys_applet_user")
@EqualsAndHashCode(callSuper=false)
public class AppletUser extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户名 */
    @TableField("username")
    private String username;

    /** 昵称 */
    @TableField("nick_name")
    private String nickName;

    /** 手机号 */
    @TableField("phone")
    private String phone;

    /** 头像 */
    @TableField("avatar")
    private String avatar;

    /** 是否启用 */
    @TableField("enabled")
    private Integer enabled;

    /** 是否删除 */
    @TableLogic
    private Integer deleted;

    /** 来源 （0-注册 1-授权）*/
    @TableField("source")
    private Integer source;

    /** 微信openId */
    @TableField("open_id")
    private String openId;
} 