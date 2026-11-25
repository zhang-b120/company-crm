package com.qykj.finmaster.task.domain.resp;

import com.qykj.finmaster.task.domain.dto.DealCommonTaskDetailInfo;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * @Author zhb
 * @Date 2025-08-18
 * @Description
 */
@Data
public class GetDealCommonTaskDetailDealResp implements Serializable {
    @Serial
    private static final long serialVersionUID = -1322225272869269619L;

    /**
     * 未处理数量
     */
    private Integer processSum = 0;

    /**
     * 昨天之前未处理数量
     */
    private Integer yesterdayCount = 0;

    /**
     * 今天待未处理数量
     */
    private Integer todayCount = 0;

    /**
     * 昨天之前未处理流程明细
     */
    private List<DealCommonTaskDetailInfo> yesterdayTaskDetailInfoList;

    /**
     * 今天未处理流程明细
     */
    private List<DealCommonTaskDetailInfo> detailInfoList;
}