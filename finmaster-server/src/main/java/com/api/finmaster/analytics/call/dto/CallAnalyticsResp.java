package com.qykj.finmaster.analytics.call.dto;

import com.qykj.finmaster.analytics.common.dto.TrendPointResp;
import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public class CallAnalyticsResp {

    @Data
    public static class OverviewResp {
        private String scope;
        /** 总呼出数 */
        private Long totalCalls;
        /** 已接通数 call_status=2 */
        private Long connected;
        /** 未接通 call_status=3 */
        private Long notConnected;
        /** 接通率 = connected / (connected + notConnected) * 100 */
        private BigDecimal connectedRate;
        /** 平均通话时长（秒） */
        private BigDecimal avgDurationSec;
        /** 总通话时长（秒） */
        private Long totalDurationSec;
        /** 质检覆盖率 = qualityDone / connected * 100 */
        private BigDecimal qualityCoverageRate;
        /** 平均质检分 */
        private BigDecimal avgQualityScore;
        /** 呼出趋势 */
        private List<TrendPointResp> callTrend;
    }

    @Data
    public static class RankingItem {
        private Long callerUserId;
        @Excel(title = "姓名", sort = 1)
        private String callerUserName;
        @Excel(title = "呼出数", sort = 2)
        private Long totalCalls;
        @Excel(title = "接通数", sort = 3)
        private Long connected;
        @Excel(title = "接通率(%)", sort = 4)
        private BigDecimal connectedRate;
        @Excel(title = "总时长(秒)", sort = 5)
        private Long totalDurationSec;
        @Excel(title = "平均时长(秒)", sort = 6)
        private BigDecimal avgDurationSec;
        @Excel(title = "质检分", sort = 7)
        private BigDecimal avgQualityScore;
    }

    @Data
    public static class LowScoreCall {
        private Long id;
        private Long callerUserId;
        private String callerUserName;
        private String contactName;
        private String phoneNumber;
        private Integer callDuration;
        private Integer qualityScore;
        private java.util.Date callStartTime;
        private String recordingUrl;
    }
}
