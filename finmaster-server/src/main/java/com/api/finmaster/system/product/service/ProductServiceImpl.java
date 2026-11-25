package com.qykj.finmaster.system.product.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.exception.ServiceException;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.CustomerCompanyManager;
import com.qykj.finmaster.system.dict.entity.SysDictData;
import com.qykj.finmaster.system.dict.service.SysDictDataServiceImpl;
import com.qykj.finmaster.system.product.domain.Product;
import com.qykj.finmaster.system.product.domain.req.AddProductionReq;
import com.qykj.finmaster.system.product.domain.req.GetProductionPageReq;
import com.qykj.finmaster.system.product.domain.req.UaGetProductListReq;
import com.qykj.finmaster.system.product.domain.req.UpdateProductionReq;
import com.qykj.finmaster.system.product.domain.resp.GetProductListDetailResp;
import com.qykj.finmaster.system.product.domain.resp.GetProductListResp;
import com.qykj.finmaster.system.product.manager.impl.ProductManagerImpl;
import com.qykj.finmaster.system.product.mapper.ProductMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.math.BigDecimal;

import java.util.*;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 产品Service业务层处理
 */
@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> {
    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SysDictDataServiceImpl sysDictDataService;

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ProductManagerImpl productManager;
    @Autowired
    private CustomerCompanyManager customerCompanyManager;


    /**
     * 查询产品列表
     *
     * @return 产品列表
     */
    public List<GetProductionPageReq> selectProductList(GetProductionPageReq getProductionPageReq) {
        try {
            getProductionPageReq.startPage();
            Map<String, Object> params = getProductionPageReq.toBeanMap();
            // 不设置租户ID，查询所有产品
            List<GetProductionPageReq> list = baseMapper.selectProductList(params);
            return list;
        } catch (Exception e) {
            // 如果查询失败，返回空列表
            return Collections.emptyList();
        }
    }

    /**
     * 查询产品信息
     *
     * @param id 产品ID
     * @return 产品信息
     */
    public GetProductListDetailResp selectProductById(Long id) {
        // 不限制租户，查询所有产品
        Product product = getById(id);
        return CopierCopyUtils.convertTo(product, GetProductListDetailResp.class);
    }

    /**
     * 新增产品
     *
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertProduct(AddProductionReq addProductionReq, Long tenantId) {
        addProductionReq.setTenantId(tenantId); // 设置租户ID（保持数据完整性）

        // 获取MES_FLAG字典值
        SysDictData mesFlag = sysDictDataService.selectDictDataByTypeAndLabel("OUT_SYSTEM", "MES_FLAG");
        if (mesFlag != null && "开启".equals(mesFlag.getValue())) {
            addProductionReq.setProductType("HARDWARE");
            addProductionReq.setUnitPrice(new BigDecimal(0));
            addProductionReq.setMaxDiscount(0);
            // 获取MES产品新增URL配置
            SysDictData mesUrlDict = sysDictDataService.selectDictDataByTypeAndLabel("OUT_SYSTEM", "MES_URL");
            if (mesUrlDict == null || StringUtils.isEmpty(mesUrlDict.getValue())) {
                throw new ServiceException("MES系统URL配置不存在");
            }

            // 获取MES产品新增参数配置
            SysDictData mesProductAddParamsDict = sysDictDataService.selectDictDataByTypeAndLabel("OUT_SYSTEM", "MES_PRODUCT_PARAM");
            if (mesProductAddParamsDict == null || StringUtils.isEmpty(mesProductAddParamsDict.getValue())) {
                throw new ServiceException("MES产品参数配置不存在");
            }

            String mesProductAddUrl = mesUrlDict.getValue() + "/pccommon/addProduct4Out";
            String mesProductAddParams = mesProductAddParamsDict.getValue();
            String[] classIdArray = mesProductAddParams.replace("[", "").replace("]", "").split(",");

            // 构建请求参数
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("name", addProductionReq.getProductName());
            requestMap.put("number", addProductionReq.getProductNumber());
            requestMap.put("classId", classIdArray[0]);
            requestMap.put("status", "0");
            requestMap.put("typeId", "A");
            requestMap.put("unitId", addProductionReq.getUnitId());
            requestMap.put("unitName", addProductionReq.getUnit());
            requestMap.put("specification", addProductionReq.getSpecification());

            // 调用MES接口
            ResponseEntity<String> response = restTemplate.postForEntity(mesProductAddUrl, requestMap, String.class);
            if (response.getStatusCode() != HttpStatus.OK) {
                throw new ServiceException("调用外部系统接口失败");
            }

        }
        Product product = CopierCopyUtils.convertTo(addProductionReq, Product.class);
        return productManager.save(product);

    }

    /**
     * 修改产品
     *
     * @return 结果
     */
    public Boolean updateProduct(UpdateProductionReq updateProductionReq, Long tenantId) {
        Product product = CopierCopyUtils.convertTo(updateProductionReq, Product.class);
        product.setTenantId(tenantId); // 设置租户ID（保持数据完整性）

        // 校验产品名称是否唯一（全局唯一）
        if (StringUtils.isNotEmpty(product.getProductName())) {
            Product existProduct = productMapper.selectByProductNameExcludeId(product.getProductName(), product.getId());
            if (existProduct != null) {
                throw new ServiceException("产品名称已存在");
            }
        }
        productManager.getBaseMapper().updateById(product);
        return Boolean.TRUE;
    }

    /**
     * 删除产品信息
     *
     * @param id 产品ID
     * @return 结果
     */
    public int deleteProductById(Long id) {
        try {
            // 不限制租户，可以删除任何产品
            Product product = getById(id);
            if (product == null) {
                throw new ServiceException("产品不存在");
            }
            return removeById(id) ? 1 : 0;
        } catch (Exception e) {
            throw new ServiceException("删除产品失败：" + e.getMessage());
        }
    }

    /**
     * 批量删除产品
     */
    public Boolean deleteProductByIds(Long[] ids) {
        productManager.getBaseMapper().deleteBatchIds(List.of(ids));
        return Boolean.TRUE;
    }

    /**
     * 同步产品信息
     */
    @Transactional(rollbackFor = Exception.class)
    public int syncProduct(Long tenantId) {
        // 获取MES产品URL配置
        SysDictData mesUrlDict = sysDictDataService.selectDictDataByTypeAndLabel("OUT_SYSTEM", "MES_URL");
        if (mesUrlDict == null || StringUtils.isEmpty(mesUrlDict.getValue())) {
            throw new ServiceException("MES系统URL配置不存在");
        }

        // 获取MES产品参数配置
        SysDictData mesProductParamDict = sysDictDataService.selectDictDataByTypeAndLabel("OUT_SYSTEM", "MES_PRODUCT_PARAM");
        if (mesProductParamDict == null || StringUtils.isEmpty(mesProductParamDict.getValue())) {
            throw new ServiceException("MES产品参数配置不存在");
        }

        String mesProductUrl = mesUrlDict.getValue() + "pccommon/getproductLists";
        String mesProductParam = mesProductParamDict.getValue();

        // 解析classId参数
        String[] classIdArray = mesProductParam.replace("[", "").replace("]", "").split(",");
        Long[] classIds = new Long[classIdArray.length];
        for (int i = 0; i < classIdArray.length; i++) {
            classIds[i] = Long.parseLong(classIdArray[i].trim());
        }

        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("classId", classIds);

        // 调用MES接口获取产品数据
        String response = restTemplate.postForObject(mesProductUrl, requestBody, String.class);
        if (StringUtils.isEmpty(response)) {
            throw new ServiceException("MES接口返回数据为空");
        }

        JSONObject jsonResponse = JSON.parseObject(response);
        JSONArray productArray = jsonResponse.getJSONArray("rows");

        if (productArray == null || productArray.isEmpty()) {
            throw new ServiceException("MES接口返回的产品数据为空");
        }

        // 获取当前租户ID


        // 清空当前租户的产品数据
        productMapper.deleteProductsByTenantId(tenantId);

        // 批量插入新数据
        int count = 0;
        for (int i = 0; i < productArray.size(); i++) {
            JSONObject jsonObject = productArray.getJSONObject(i);
            Product product = new Product();
            // 设置租户ID
            product.setTenantId(tenantId);
            // 根据MES返回的数据结构设置产品属性
            product.setProductName(jsonObject.getString("name"));
            product.setProductNumber(jsonObject.getString("number"));
            product.setProductType("HARDWARE"); // 默认为硬件产品
            product.setUnit(jsonObject.getString("unit"));
            product.setSpecification(StringUtils.isNotEmpty(jsonObject.getString("specification")) ? jsonObject.getString("specification") : "-");
            product.setUnitPrice(new BigDecimal("0.00")); // 默认单价为0
            product.setMaxDiscount(10); // 默认折扣为0
            product.setDescription(jsonObject.getString("name") + " " + jsonObject.getString("specification"));
            productManager.save(product);
            count += 1;
        }
        return count;
    }

    /**
     * 小程序获取产品列表
     *
     * @param req
     * @return
     */
    public List<GetProductListResp> uaGetProductList(UaGetProductListReq req) {
        String companyType = null;
        if (ObjectUtils.isNotEmpty(req.getCompanyId())) {
            CustomerCompany customerCompany = customerCompanyManager.lambdaQuery().eq(CustomerCompany::getId, req.getCompanyId()).one();
            if (ObjectUtils.isNotEmpty(customerCompany)) {
                companyType = customerCompany.getCompanyType();
            }
        }
        String finalCompanyType = companyType;

        List<Product> productList = productManager.lambdaQuery().eq(StringUtils.isNotEmpty(req.getProductType()), Product::getProductType, req.getProductType()).and(item -> item.like(StringUtils.isNotEmpty(finalCompanyType), Product::getProductNumber, finalCompanyType).or().eq(Product::getProductNumber, "invoicing")).list();
        if (CollUtil.isEmpty(productList)) return Collections.emptyList();

        return CopierCopyUtils.convertTo(productList, GetProductListResp.class);
    }

    public List<Map<String, Object>> getUnitList() {
            // 获取MES产品URL配置
            SysDictData mesUrlDict = sysDictDataService.selectDictDataByTypeAndLabel("OUT_SYSTEM", "MES_URL");
            if (mesUrlDict == null || StringUtils.isEmpty(mesUrlDict.getValue())) {
                // 如果MES配置不存在，返回默认单位列表
                return getDefaultUnitList();
            }

            String mesProductUrl = mesUrlDict.getValue() + "masterdata/unit/getLists";

            // 调用MES接口获取单位数据
            String response = restTemplate.getForObject(mesProductUrl, String.class);
            if (StringUtils.isEmpty(response)) {
                return getDefaultUnitList();
            }

            JSONObject jsonResponse = JSON.parseObject(response);
            JSONArray unitArray = jsonResponse.getJSONArray("data");

            if (unitArray == null || unitArray.isEmpty()) {
                return getDefaultUnitList();
            }

            // 转换数据格式
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (int i = 0; i < unitArray.size(); i++) {
                JSONObject jsonObject = unitArray.getJSONObject(i);
                Map<String, Object> unit = new HashMap<>();
                unit.put("id", jsonObject.getString("id"));
                unit.put("name", jsonObject.getString("name"));
                unit.put("value", jsonObject.getString("name")); // 为了兼容前端的不同字段名
                unit.put("label", jsonObject.getString("name")); // 为了兼容前端的不同字段名
                result.add(unit);
            }
            return result;
    }

    /**
     * 获取默认单位列表
     */
    private List<Map<String, Object>> getDefaultUnitList() {
        List<Map<String, Object>> defaultUnits = new ArrayList<>();

        // 添加常用单位
        String[] units = {"个", "件", "台", "套", "米", "千克", "升", "平方米", "立方米", "小时", "天", "月", "年"};

        for (int i = 0; i < units.length; i++) {
            Map<String, Object> unit = new HashMap<>();
            unit.put("id", String.valueOf(i + 1));
            unit.put("name", units[i]);
            unit.put("value", units[i]);
            unit.put("label", units[i]);
            defaultUnits.add(unit);
        }

        return defaultUnits;
    }
}
