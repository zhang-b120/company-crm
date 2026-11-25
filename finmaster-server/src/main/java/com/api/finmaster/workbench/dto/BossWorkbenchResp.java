package com.qykj.finmaster.workbench.dto;

import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 老板经营看板返回体
 * 六个板块：核心 KPI / 部门对比 / 经营趋势 / 健康度 / 人员排行 / 结构分析
 */
@Data
public class BossWorkbenchResp {

    /** 实际生效的层级（BOSS 工作台强制 COMPANY，但若当前用户权限不足会降级） */
    private String scope;

    /** ① 公司核心 KPI 卡片 */
    private List<KpiCard> coreKpis = new ArrayList<>();

    /** ② 部门运营对比 */
    private DeptCard marketDept;
    private DeptCard salesDept;
    private DeptCard csDept;

    /** ③ 经营趋势 */
    private List<TrendPointResp> revenueTrend = new ArrayList<>();
    private List<TrendPointResp> customerTrend = new ArrayList<>();

    /** ④ 健康度指标 */
    private List<HealthItem> healthIndicators = new ArrayList<>();

    /** ⑤ 人员排行 */
    private List<SalesRankItem> salesTop = new ArrayList<>();
    private List<ButlerRankItem> butlerTop = new ArrayList<>();

    /** ⑥ 结构分析 */
    private List<PieItem> leadSourcePie = new ArrayList<>();
    private List<PieItem> productMixPie = new ArrayList<>();

    // ─── 内部类 ──────────────────────────────────────────────────────────

    @Data
    public static class KpiCard {
        private String key;
        private String label;
        private Object value;
        /** 单位：money | count | percent */
        private String unit;
        /** 同环比 %，无则 null */
        private BigDecimal growth;
        /** 是否负向指标（退款/流失等） */
        private Boolean negativeIsGood;
    }

    @Data
    public static class DeptCard {
        /** MARKET / SALES / CS */
        private String deptType;
        private String label;
        private List<DeptKpi> items = new ArrayList<>();
    }

    @Data
    public static class DeptKpi {
        private String label;
        private Object value;
        /** money | count | percent */
        private String unit;
    }

    @Data
    public static class HealthItem {
        private String key;
        private String label;
        /** 百分比值（0-100） */
        private BigDecimal rate;
        /** 是否反向指标（高即是坏，退款率） */
        private Boolean negativeIsGood;
        /** normal / warning / danger */
        private String tone;
        /** 阈值说明，如 "目标 ≥ 70%" */
        private String threshold;
    }

    @Data
    public static class SalesRankItem {
        private Integer rank;
        private String username;
        private String realName;
        private Long orderCount;
        private BigDecimal orderAmount;
        private BigDecimal contributionRate;
        private BigDecimal growth;
    }

    @Data
    public static class ButlerRankItem {
        private Integer rank;
        private String username;
        private String realName;
        private Long expiringCount;
        private Long renewedCount;
        private BigDecimal renewalRate;
        private BigDecimal renewAmount;
    }

    @Data
    public static class PieItem {
        private String name;
        private Object value;
    }
}
