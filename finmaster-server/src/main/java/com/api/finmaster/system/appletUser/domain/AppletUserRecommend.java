package com.qykj.finmaster.system.appletUser.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.qykj.finmaster.common.core.domain.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.util.Date;

/**
 * 小程序用户推荐类
 */
@Data
@TableName("sys_applet_user_recommend")
@EqualsAndHashCode(callSuper=false)
public class AppletUserRecommend extends BaseEntity {

    @Serial
    private static final long serialVersionUID = -6205727442244850828L;
    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 推荐人openID */
    @TableField("recommend_open_id")
    private String recommendOpenId;

    /** 被推荐人openID */
    @TableField("recommended_open_id")
    private String recommendedOpenId;

    /** 是否删除 */
    @TableLogic
    private Integer deleted;
} 