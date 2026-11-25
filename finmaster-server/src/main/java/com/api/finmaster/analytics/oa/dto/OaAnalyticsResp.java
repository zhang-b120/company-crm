package com.qykj.finmaster.analytics.oa.dto;

import com.qykj.finmaster.common.annotation.Excel;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public class OaAnalyticsResp {

    @Data
    public static class AttendanceComplianceResp {
        private String scope;
        private Long totalRecords;
        private Long normalCount;
        private Long lateCount;
        private Long earlyCount;
        private Long absentCount;
        private Long leaveCount;
        private BigDecimal complianceRate;   // normal / total * 100
        private BigDecimal lateRate;
        private BigDecimal absentRate;
        private List<StatusItem> statusDist;
        /** 按人合规率（TOP 不合规 20） */
        private List<UserCompliance> worstComplianceUsers;
    }

    @Data
    public static class StatusItem {
        private String status;
        private String statusLabel;
        private Long count;
    }

    @Data
    public static class LeaveOvertimeResp {
        private String scope;
        /** 请假总天数 */
        private BigDecimal totalLeaveDays;
        /** 已审批通过的请假单数 */
        private Long approvedLeaveCount;
        /** 请假类型分布 */
        private List<LeaveTypeItem> leaveTypeDist;
        /** 加班总小时数 */
        private BigDecimal totalOvertimeHours;
        /** 加班单数 */
        private Long overtimeCount;
        /** 请假 TOP 10 */
        private List<UserLeaveItem> topLeaveUsers;
    }

    @Data
    public static class LeaveTypeItem {
        private String leaveType;
        private Long count;
        private BigDecimal totalDays;
    }

    @Data
    public static class UserLeaveItem {
        private String applicant;
        private String applicantName;
        private Long leaveCount;
        private BigDecimal totalDays;
    }

    @Data
    public static class UserCompliance {
        private Long userId;
        @Excel(title = "账号", sort = 2)
        private String username;
        @Excel(title = "姓名", sort = 1)
        private String realName;
        @Excel(title = "工作日", sort = 3)
        private Long totalDays;
        @Excel(title = "迟到次数", sort = 4)
        private Long lateCount;
        @Excel(title = "缺勤次数", sort = 5)
        private Long absentCount;
        @Excel(title = "合规率(%)", sort = 6)
        private BigDecimal complianceRate;
    }
}
