package com.qykj.finmaster.workbench.dto;

import lombok.Data;

import java.util.List;

/**
 * 登录后首页加载的工作台配置，决定 Tab 有哪些 + 层级切换器可选范围
 */
@Data
public class WorkbenchConfigResp {
    /** 可见工作台列表，按 MARKET/SALES/CS 的固定顺序返回 */
    private List<WorkbenchItem> workbenches;
    /** 是否为公司级用户（展示公司负责人视角） */
    private Boolean isCompanyScope;
    /** 是否绑定 SALES_LEADER 角色 */
    private Boolean salesLeader;
    /** 是否绑定 CS_LEADER 角色 */
    private Boolean csLeader;
}
