package com.qykj.finmaster.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.order.domain.OrderDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 订单明细Mapper接口
 */
@Mapper
public interface OrderDetailMapper extends BaseMapper<OrderDetail> {

    /**
     * 统计正常订单下同租户、同公司（同税号公司ID列表）、同业务的订单明细条数（用于判断新增/续费）
     * 补款业务不参与校验：excludeBusinessId 传补款业务ID时，统计中排除该业务明细
     *
     * @param tenantId         租户ID
     * @param companyIds       同公司ID列表（同税号，可为单元素）
     * @param businessId       业务ID
     * @param excludeBusinessId 排除的业务ID（如补款业务ID，可为 null）
     * @return 条数
     */
    long countNormalOrderDetailsByCompanyAndBusiness(@Param("tenantId") Long tenantId,
                                                     @Param("companyIds") List<Long> companyIds,
                                                     @Param("businessId") Long businessId,
                                                     @Param("excludeBusinessId") Long excludeBusinessId);
}
