package com.qykj.finmaster.customer.leadLog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.customer.leadLog.entity.LeadLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 线索跟进日志Mapper接口
 */
@Mapper
public interface LeadLogMapper extends BaseMapper<LeadLog> {

    /**
     * 查询线索的跟进日志列表
     *
     * @param leadId 线索ID
     * @param tenantId 租户ID
     * @return 跟进日志列表
     */
    List<LeadLog> selectByLeadId(@Param("leadId") Long leadId, @Param("tenantId") Long tenantId);

    /**
     * 新增线索日志
     *
     * @param leadLog 线索日志信息
     * @return 结果
     */
    int insertLeadLog(LeadLog leadLog);

    /**
     * 批量更新日志leadId（只更新指定logType）
     * @param fromLeadId 原线索ID
     * @param toLeadId 目标线索ID
     * @param logType 日志类型
     * @return 影响行数
     */
    int updateLeadLogLeadId(@Param("fromLeadId") Long fromLeadId, @Param("toLeadId") Long toLeadId, @Param("logType") Integer logType);
}
