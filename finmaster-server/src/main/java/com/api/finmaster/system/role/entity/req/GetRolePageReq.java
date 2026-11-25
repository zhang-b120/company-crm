package com.qykj.finmaster.system.role.entity.req;

import com.qykj.finmaster.common.core.domain.page.PageEntity;
import lombok.Data;

import java.io.Serial;

/**
 * @Author zhb
 * @Date 2025-08-01
 * @Description
 */
@Data
public class GetRolePageReq extends PageEntity {
    @Serial
    private static final long serialVersionUID = -6018848538956580467L;

    /**
     * 角色名称
     */
    private String name;

    /**
     * 角色编码
     */
    private String code;

    /**
     * 状态
     */
    private Boolean enabled;
}