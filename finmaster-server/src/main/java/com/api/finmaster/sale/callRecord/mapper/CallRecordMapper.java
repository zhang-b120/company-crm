package com.qykj.finmaster.sale.callRecord.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.sale.callRecord.entity.CallRecord;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface CallRecordMapper extends BaseMapper<CallRecord> {

    /**
     * 按销售负责人统计通话关联的客户订单数
     * 线索通话：call_record.lead_id → customer_info.lead_id → customer_info.id → orders.customer_id
     * 私海通话：call_record.customer_id → orders.customer_id
     */
    @Select({"<script>",
            "SELECT cr.caller_user_id AS callerUserId, cr.caller_user_name AS callerUserName,",
            "COUNT(DISTINCT o.id) AS relatedOrderCount",
            "FROM call_record cr",
            "LEFT JOIN customer_info ci ON (",
            "  (cr.customer_id IS NOT NULL AND ci.id = cr.customer_id)",
            "  OR (cr.customer_id IS NULL AND cr.lead_id IS NOT NULL AND ci.lead_id = cr.lead_id)",
            ")",
            "LEFT JOIN orders o ON o.customer_id = ci.id AND o.status != 'CANCELLED'",
            "WHERE cr.tenant_id = #{tenantId} AND cr.deleted = 0 AND cr.call_status = 2",
            "<if test=\"startDate != null and startDate != ''\">AND cr.call_end_time &gt;= #{startDate} </if>",
            "<if test=\"endDate != null and endDate != ''\">AND cr.call_end_time &lt;= #{endDate} </if>",
            "GROUP BY cr.caller_user_id, cr.caller_user_name",
            "ORDER BY relatedOrderCount DESC",
            "</script>"})
    List<Map<String, Object>> selectRelatedOrderCountByUser(
            @Param("tenantId") Long tenantId,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);
}
