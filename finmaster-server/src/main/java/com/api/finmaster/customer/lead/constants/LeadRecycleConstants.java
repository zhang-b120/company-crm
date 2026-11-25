package com.qykj.finmaster.customer.lead.constants;

/**
 * 线索自动回收相关常量（cron 任务与各处"即将回收/待回收"预览查询共用）
 */
public final class LeadRecycleConstants {

    private LeadRecycleConstants() {}

    /** 销售部部门 ID（线索回收仅命中此部门及子部门成员名下） */
    public static final Long SALES_DEPT_ID = 6L;
}
