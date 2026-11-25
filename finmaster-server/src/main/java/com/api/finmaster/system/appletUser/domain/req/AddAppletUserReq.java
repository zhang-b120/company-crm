package com.qykj.finmaster.system.appletUser.domain.req;

import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * @Author zhb
 * @Date 2025-06-03
 * @Description
 */
@Data
public class AddAppletUserReq implements Serializable {

    private static final long serialVersionUID = 7700283016416974039L;
    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickName;

    /** 手机号 */
    private String phone;

    /** 头像 */
    private String avatar;

    /** 微信openId */
    private String openId;

    /** 分享人openId 授权时此字段必传*/
    private String shareOpenId;

    /** 分享人openId 授权时此字段必传*/
    private Long companyId;
}