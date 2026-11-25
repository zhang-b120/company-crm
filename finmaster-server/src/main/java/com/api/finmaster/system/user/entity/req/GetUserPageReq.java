package com.qykj.finmaster.system.user.entity.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * @Author zhb
 * @Date 2025-07-31
 * @Description
 */
@Data
public class GetUserPageReq extends PageEntity {
    @Serial
    private static final long serialVersionUID = -3481971835866950189L;

    /**
     * 用户名
     */
    private String username;

    /**
     * 姓名
     */
    private String realName;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 启用状态
     */
    private Boolean enabled;
}