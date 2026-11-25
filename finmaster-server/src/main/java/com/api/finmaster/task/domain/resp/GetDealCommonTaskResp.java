package com.qykj.finmaster.task.domain.resp;

import com.qykj.finmaster.task.domain.dto.DealCommonTaskDetailInfo;
import com.qykj.finmaster.task.domain.dto.DealCommonTaskInfo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-08-16
 * @Description
 */
@Data
public class GetDealCommonTaskResp implements Serializable {
    @Serial
    private static final long serialVersionUID = 5965992269495529201L;

    /**
     * 未处理任务总数
     */
    private Integer taskSum;

    /**
     * 昨天之前未处理任务数量
     */
    private Integer yesterdayTaskCount = 0;

    /**
     * 今天未处理任务数量
     */
    private Integer todayTaskCount = 0;

    /**
     * 昨天之前未处理任务明细
     */
    private List<DealCommonTaskInfo> yesterdayTaskInfoList;

    /**
     * 今天未处理任务明细
     */
    private List<DealCommonTaskInfo> todayTaskInfoList;

    /**
     * 未处理任务流程数量
     */
    private Integer taskDetailSum = 0;

    /**
     * 昨天之前未处理任务流程数量
     */
    private Integer yesterdayTaskDetailCount = 0;

    /**
     * 今天待未处理任务流程数量
     */
    private Integer todayTaskDetailCount = 0;

    /**
     * 昨天之前未处理任务流程明细
     */
    private List<DealCommonTaskDetailInfo> yesterdayTaskDetailInfoList;

    /**
     * 今天未处理任务流程明细
     */
    private List<DealCommonTaskDetailInfo> todayTaskDetailInfoList;

}