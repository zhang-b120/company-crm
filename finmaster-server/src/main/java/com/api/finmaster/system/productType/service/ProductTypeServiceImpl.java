package com.qykj.finmaster.system.productType.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.exception.ServiceException;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.product.manager.ProductManager;
import com.qykj.finmaster.system.productType.domain.req.AddProductTypeReq;
import com.qykj.finmaster.system.productType.domain.ProductType;
import com.qykj.finmaster.system.productType.domain.req.UpdateProductTypeReq;
import com.qykj.finmaster.system.productType.domain.resp.GetProductTypeResp;
import com.qykj.finmaster.system.productType.manager.impl.ProductTypeManagerImpl;
import com.qykj.finmaster.system.productType.mapper.ProductTypeMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 产品类型Service业务层处理
 */
@Service
public class ProductTypeServiceImpl extends ServiceImpl<ProductTypeMapper, ProductType> {

    @Autowired
    private ProductManager productManager;
    @Autowired
    private ProductTypeMapper productTypeMapper;
    @Autowired
    private ProductTypeManagerImpl productTypeManager;

    /**
     * 查询产品类型列表
     *
     * @return 产品类型列表
     */
    public List<GetProductTypeResp> selectProductTypeList() {
        // 不限制租户，查询所有产品类型
        List<ProductType> list = productTypeManager.list();
        if(CollUtil.isEmpty(list)){
            return Collections.emptyList();
        }
        List<GetProductTypeResp> respList = list.stream()
                .map(item -> {
                    GetProductTypeResp resp = CopierCopyUtils.convertTo(item, GetProductTypeResp.class);
                    resp.setCreateTime(DateUtils.dateToLocalDateTime(item.getCreateTime()));
                    resp.setUpdateTime(DateUtils.dateToLocalDateTime(item.getUpdateTime()));
                    return resp;
                }).toList();
        return respList;
    }

    /**
     * 查询产品类型信息
     *
     * @param id 产品类型ID
     * @return 产品类型信息
     */
    public ProductType selectProductTypeById(Long id) {
        return productTypeManager.getById(id);
    }

    /**
     * 新增产品类型
     *
     * @param productType 产品类型信息
     * @param tenantId
     * @param loginUser
     * @return 结果
     */
    public Boolean insertProductType(AddProductTypeReq req, Long tenantId, LoginUser loginUser) {
        // 校验类型编码是否唯一
        if(checkCodeUnique(req.getCode())){
            throw new BusinessException(ResultMsg.PRODUCT_TYPE_ADD_EXISTS_CODE);
        }
        ProductType productType = CopierCopyUtils.convertTo(req, ProductType.class);
        productType.setTenantId(tenantId);
        productType.setCreateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)){
            productType.setCreateBy(loginUser.getUsername());
            productType.setCreateByName(loginUser.getRealName());
        }
        return productTypeManager.save(productType);
    }

    /**
     * 修改产品类型
     *
     * @param req 产品类型信息
     * @param tenantId
     * @param loginUser
     * @return 结果
     */
    public Boolean updateProductType(UpdateProductTypeReq req, Long tenantId, LoginUser loginUser) {
        // 校验类型编码是否唯一
        if(checkCodeUnique(req.getCode())){
            throw new BusinessException(ResultMsg.PRODUCT_TYPE_ADD_EXISTS_CODE);
        }
        ProductType productType = CopierCopyUtils.convertTo(req, ProductType.class);
        productType.setTenantId(tenantId);
        productType.setUpdateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)){
            productType.setUpdateBy(loginUser.getUsername());
            productType.setUpdateByName(loginUser.getRealName());
        }
        return productTypeManager.updateById(productType);
    }

    /**
     * 校验编码是否唯一
     * @param code
     * @return
     */
    public Boolean checkCodeUnique(String code){
        Boolean check = Boolean.FALSE;
        if (StringUtils.isNotEmpty(code)) {
            ProductType existType = productTypeManager.lambdaQuery()
                    .eq(ProductType::getCode, code)
                    .one();
            if (ObjectUtils.isNotEmpty(existType)) {
                check = Boolean.TRUE;
            }
        }
        return check;
    }

    /**
     * 删除产品类型信息
     *
     * @param id 产品类型ID
     * @return 结果
     */
    public Boolean deleteProductTypeById(Long id) {
        return productTypeManager.removeById(id);
    }

    /**
     * 根据关键词搜索产品类型
     *
     * @param keyword 搜索关键词
     * @return 产品类型列表
     */
    public List<ProductType> searchProductTypes(String keyword) {
        if (StringUtils.isEmpty(keyword)) {
            // 如果没有关键词，返回所有产品类型
            return list();
        } else {
            // 如果有关键词，进行模糊搜索
            return baseMapper.selectByKeyword(keyword);
        }
    }
}
