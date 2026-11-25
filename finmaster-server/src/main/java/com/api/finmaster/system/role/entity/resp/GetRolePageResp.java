package com.qykj.finmaster.system.role.entity.resp;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Author zhb
 * @Date 2025-08-01
 * @Description
 */
@Data
public class GetRolePageResp implements Serializable {

    @Serial
    private static final long serialVersionUID = 4851521938289079370L;
    /** 角色ID */
    private Long id;

    /** 角色名称 */
    private String name;

    /** 角色编码 */
    private String code;

    /** 角色描述 */
    private String description;

    /** 排序 */
    private Integer sort;

    /** 是否启用 */
    private Boolean enabled;

    /** 工作台类型：MARKET / SALES / CS / BOSS / NONE */
    private String workbenchType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}