package com.qykj.finmaster.history.mapper;

import com.qykj.finmaster.history.HistoryOrderDetailRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 历史订单查询 Mapper，仅用于一次性生成历史合同
 */
@Mapper
public interface HistoryOrderMapper {

    /**
     * 查询需要用于生成历史合同的订单明细
     *
     * @param deployTime 上线时间边界（可选，字符串格式与 orders.create_time 一致），为 null 或空则不过滤时间
     * @return 历史订单明细列表
     */
    List<HistoryOrderDetailRecord> selectHistoryOrderDetails(@Param("deployTime") String deployTime);
}

