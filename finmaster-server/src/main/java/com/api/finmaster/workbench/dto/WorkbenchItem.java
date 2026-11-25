package com.qykj.finmaster.workbench.dto;

import lombok.Data;

import java.util.List;

/**
 * 单个工作台的配置项，含类型、名称、层级权限
 */
@Data
public class WorkbenchItem {
    /** MARKET / SALES / CS */
    private String type;
    /** 展示名称 */
    private String label;
    /** 当前用户在此工作台下可切换的层级（PERSONAL/TEAM/COMPANY） */
    private List<String> availableScopes;
    /** 是否为默认工作台（用户登录后优先展示） */
    private Boolean defaultActive;
    /** 进入工作台时默认 scope（团队长默认 TEAM，员工 PERSONAL） */
    private String defaultScope;
}
