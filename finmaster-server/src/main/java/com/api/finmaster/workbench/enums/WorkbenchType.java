package com.qykj.finmaster.workbench.enums;

/**
 * 工作台类型，对应 sys_role.workbench_type（V2.0.68 起改为按角色分发，
 * 兼容期 sys_department.workbench_type 字段保留但不再作为权威配置）
 */
public enum WorkbenchType {

    MARKET("MARKET", "市场部工作台"),
    SALES("SALES", "销售部工作台"),
    SALES_LEADER("SALES_LEADER", "销售团队长工作台"),
    CS("CS", "客服部工作台"),
    CS_LEADER("CS_LEADER", "客服团队长工作台"),
    BOSS("BOSS", "经营看板"),
    NONE("NONE", "未绑定");

    private final String code;
    private final String label;

    WorkbenchType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static WorkbenchType fromCode(String code) {
        if (code == null) return NONE;
        for (WorkbenchType t : values()) {
            if (t.code.equalsIgnoreCase(code)) return t;
        }
        return NONE;
    }

    public static boolean isBusinessType(String code) {
        WorkbenchType t = fromCode(code);
        return t == MARKET || t == SALES || t == SALES_LEADER
                || t == CS || t == CS_LEADER || t == BOSS;
    }

    /** 是否团队长类型（默认开启 TEAM 视角） */
    public static boolean isLeaderType(String code) {
        WorkbenchType t = fromCode(code);
        return t == SALES_LEADER || t == CS_LEADER;
    }
}
