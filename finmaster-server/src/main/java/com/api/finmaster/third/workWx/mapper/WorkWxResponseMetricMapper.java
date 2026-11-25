package com.qykj.finmaster.third.workWx.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.third.workWx.entity.WorkWxResponseMetric;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface WorkWxResponseMetricMapper extends BaseMapper<WorkWxResponseMetric> {

    /** 按群统计：rounds/replied/avg_resp/timeout/unreplied */
    List<Map<String, Object>> statsByGroup(@Param("tenantId") Long tenantId, @Param("since") Date since);

    /** 按员工统计（first_reply_user，已回复）：reply_count/avg_resp/timeout */
    List<Map<String, Object>> statsByMember(@Param("tenantId") Long tenantId, @Param("since") Date since);

    /**
     * 会话回合明细（工单 82 / WXRESP-03）：metric ⨝ work_wx_message（客户消息/员工回复）。
     * statuses 为空表示不限状态；keyword 模糊匹配客户消息内容；orderBy: time/waited/respSeconds。
     */
    List<Map<String, Object>> turnsList(@Param("tenantId") Long tenantId,
                                        @Param("startTime") Date startTime,
                                        @Param("endTime") Date endTime,
                                        @Param("roomId") String roomId,
                                        @Param("memberUser") String memberUser,
                                        @Param("customerUser") String customerUser,
                                        @Param("statuses") List<String> statuses,
                                        @Param("keyword") String keyword,
                                        @Param("orderBy") String orderBy,
                                        @Param("offset") Integer offset,
                                        @Param("limit") Integer limit);

    /** 会话回合明细总数（与 turnsList 同 WHERE）。 */
    Long turnsCount(@Param("tenantId") Long tenantId,
                    @Param("startTime") Date startTime,
                    @Param("endTime") Date endTime,
                    @Param("roomId") String roomId,
                    @Param("memberUser") String memberUser,
                    @Param("customerUser") String customerUser,
                    @Param("statuses") List<String> statuses,
                    @Param("keyword") String keyword);
}
