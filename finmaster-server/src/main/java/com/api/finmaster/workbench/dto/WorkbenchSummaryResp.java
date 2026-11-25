package com.qykj.finmaster.workbench.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 工作台首页聚合返回的统一结构
 * 前端基于 kpis / ranking / pie 三块渲染；不同工作台填不同键
 */
@Data
public class WorkbenchSummaryResp {
    /** MARKET / SALES / CS */
    private String type;
    /** 实际生效的层级（已按权限 cap） */
    private String scope;
    /** KPI 卡片列表（有序） */
    private List<KpiCard> kpis = new ArrayList<>();
    /** 排行榜（如销售 TOP、管家 TOP、录入 TOP） */
    private List<RankItem> ranking = new ArrayList<>();
    /** 饼图或 TOP 分类（如线索来源、未续费原因） */
    private List<PieItem> pie = new ArrayList<>();

    @Data
    public static class KpiCard {
        /** 程序键，如 newLeads / unassigned */
        private String key;
        /** 展示文本 */
        private String label;
        /** 主值 */
        private Object value;
        /** 副说明（如 "较上期" 或单位） */
        private String sub;
        /** 环比 %，无则 null */
        private BigDecimal growth;
        /** 是否"低即是好"（用于染色方向：退款/超期等） */
        private Boolean negativeIsGood;
        /** 染色提示：normal / warning / danger */
        private String tone;
    }

    @Data
    public static class RankItem {
        private Integer rank;
        private String name;
        private Object value;
        private String sub;
    }

    @Data
    public static class PieItem {
        private String name;
        private Object value;
    }
}
