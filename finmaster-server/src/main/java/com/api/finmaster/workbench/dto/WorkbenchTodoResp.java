package com.qykj.finmaster.workbench.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 工作台右下角"我的待办"面板返回体
 *
 * 由三类组成：
 *   - workOrders: 当前用户作为 current_owner_user / owner_user 且未结束的任务单
 *   - leadFollowUps: 今日 next_follow_time 的线索
 *   - customerFollowUps: 今日 next_follow_time 的私海客户
 */
@Data
public class WorkbenchTodoResp {

    private TodoCounts counts = new TodoCounts();
    private List<WorkOrderItem> workOrders = new ArrayList<>();
    private List<FollowUpItem> leadFollowUps = new ArrayList<>();
    private List<FollowUpItem> customerFollowUps = new ArrayList<>();

    @Data
    public static class TodoCounts {
        private Long workOrder;
        private Long leadFollowUp;
        private Long customerFollowUp;
    }

    @Data
    public static class WorkOrderItem {
        private Long id;
        private String taskNo;
        private String title;
        private String taskType;
        private String status;
        private Date dueTime;
        /** 是否已超期 */
        private Boolean overdue;
    }

    @Data
    public static class FollowUpItem {
        /** 业务实体 id（线索 id 或客户 id） */
        private Long id;
        /** 名称（线索名 / 客户名） */
        private String name;
        /** 联系人 / 电话 */
        private String contactName;
        private String phone;
        /** 下次跟进时间 */
        private Date nextFollowTime;
        /** 上次跟进内容（一句话） */
        private String lastFollowContent;
        /** 标签类型：APPOINTMENT=今日约定 / RECYCLE=即将回收 */
        private String tagType;
        /** 距自动回收天数（仅 RECYCLE 时有值） */
        private Integer recycleDays;
    }
}
