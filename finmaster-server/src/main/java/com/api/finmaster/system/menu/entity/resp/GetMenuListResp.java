package com.qykj.finmaster.system.menu.entity.resp;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.qykj.finmaster.system.menu.entity.Menu;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-08-04
 * @Description
 */
@Data
public class GetMenuListResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -2937126625489395706L;

    private Long id;

    /** 父菜单ID */
    private Long parentId;

    /** 菜单名称 */
    private String name;

    /** 路由路径 */
    private String path;

    /** 组件路径 */
    private String component;

    /** 权限标识 */
    private String permission;

    /** 菜单图标 */
    private String icon;

    /** 菜单类型（1:目录 2:菜单 3:按钮） */
    private Integer type;

    /** 显示顺序 */
    private Integer sort;

    /** 是否启用 */
    private Boolean enabled;

    /** 子菜单列表（非数据库字段） */
    private List<GetMenuListResp> children;
}