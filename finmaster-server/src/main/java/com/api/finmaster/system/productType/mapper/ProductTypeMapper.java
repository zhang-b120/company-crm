package com.qykj.finmaster.system.productType.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.productType.domain.ProductType;

import java.util.List;

/**
 * 产品类型Mapper接口
 */
public interface ProductTypeMapper extends BaseMapper<ProductType> {
    /**
     * 根据编码查询产品类型
     *
     * @param code 类型编码
     * @return 产品类型信息
     */
    ProductType selectByCode(String code);

    /**
     * 根据编码查询产品类型(排除指定ID)
     *
     * @param code 类型编码
     * @param id   类型ID
     * @return 产品类型信息
     */
    ProductType selectByCodeExcludeId(String code, Long id);

    /**
     * 根据名称或编码模糊查询产品类型列表
     *
     * @param keyword 搜索关键词
     * @return 产品类型列表
     */
    List<ProductType> selectByKeyword(String keyword);
}
