package com.qykj.finmaster.system.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.product.domain.Product;
import com.qykj.finmaster.system.product.domain.req.GetProductionPageReq;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 产品Mapper接口
 */
@Mapper
public interface ProductMapper extends BaseMapper<Product> {
    /**
     * 查询产品列表
     *
     * @return 产品列表
     */
    List<GetProductionPageReq> selectProductList(@Param("params") Map<String, Object> params);

    /**
     * 根据名称查询产品
     *
     * @param productName 产品名称
     * @return 产品信息
     */
    Product selectByProductName(String productName);

    /**
     * 根据名称查询产品(排除指定ID)
     *
     * @param productName 产品名称
     * @param id 产品ID
     * @return 产品信息
     */
    Product selectByProductNameExcludeId(String productName, Long id);

    /**
     * 新增产品
     * @return 影响行数
     */
    int insertProduct(@Param("params") Map<String, Object> params);

    /**
     * 删除所有产品
     *
     * @return 影响行数
     */
    int deleteAllProducts();

    /**
     * 根据租户ID删除产品
     *
     * @param tenantId 租户ID
     * @return 影响行数
     */
    int deleteProductsByTenantId(Long tenantId);
}
