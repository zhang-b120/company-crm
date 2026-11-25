package com.qykj.finmaster.system.serviceMaintenance.service;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceBusiness;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceMaintenance;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceMaintenanceProcess;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceMaintenanceProduct;
import com.qykj.finmaster.system.serviceMaintenance.domain.dto.ServiceProcessInfo;
import com.qykj.finmaster.system.serviceMaintenance.domain.dto.ServiceProductInfo;
import com.qykj.finmaster.system.serviceMaintenance.domain.req.*;
import com.qykj.finmaster.system.serviceMaintenance.domain.resp.GetServiceMaintenanceInfoResp;
import com.qykj.finmaster.system.serviceMaintenance.domain.resp.GetServiceMaintenanceListResp;
import com.qykj.finmaster.system.serviceMaintenance.domain.resp.GetServiceProcessInfoResp;
import com.qykj.finmaster.system.serviceMaintenance.manager.impl.ServiceBusinessManagerImpl;
import com.qykj.finmaster.system.serviceMaintenance.manager.impl.ServiceMaintenanceManagerImpl;
import com.qykj.finmaster.system.serviceMaintenance.manager.impl.ServiceMaintenanceProcessManagerImpl;
import com.qykj.finmaster.system.serviceMaintenance.manager.impl.ServiceMaintenanceProductManagerImpl;
import com.qykj.finmaster.system.serviceMaintenance.mapper.ServiceMaintenanceMapper;
import com.qykj.finmaster.system.bussiness.domain.SysBusiness;
import com.qykj.finmaster.system.bussiness.manager.impl.SysBusinessManagerImpl;
import com.qykj.finmaster.system.bussiness.mapper.SysBusinessProductMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ServiceMaintenanceServiceImpl {

    @Autowired
    public ServiceMaintenanceMapper serviceMaintenanceMapper;
    @Autowired
    public ServiceMaintenanceManagerImpl serviceMaintenanceManager;
    @Autowired
    public ServiceProcessServiceImpl serviceProcessService;
    @Autowired
    public ServiceMaintenanceProductManagerImpl serviceMaintenanceProductManager;
    @Autowired
    public ServiceMaintenanceProcessManagerImpl serviceMaintenanceProcessManager;
    @Autowired
    public CustomerCompanyManagerImpl customerCompanyManager;
    @Autowired
    public SysBusinessProductMapper sysBusinessProductMapper;
    @Autowired
    public SysBusinessManagerImpl sysBusinessManager;
    @Autowired
    public ServiceBusinessManagerImpl serviceBusinessManager;

    /**
     * 获取服务维护列表
     *
     * @param req
     * @return
     */
    public List<GetServiceMaintenanceListResp> getServiceMaintenanceList(GetServiceMaintenanceListReq req) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        List<GetServiceMaintenanceListResp> respList = serviceMaintenanceMapper.getServiceMaintenanceList(params);
        if(CollUtil.isEmpty(respList)){
            return Collections.emptyList();
        }
        List<Long> idList = respList.stream().map(GetServiceMaintenanceListResp::getId).toList();
        // 批量查询服务-业务关联（一个服务可关联多个业务）
        List<ServiceBusiness> serviceBusinessList = serviceBusinessManager.lambdaQuery()
                .in(ServiceBusiness::getServiceId, idList)
                .list();
        Map<Long, List<Long>> serviceToBusinessIds = serviceBusinessList.stream()
                .collect(Collectors.groupingBy(ServiceBusiness::getServiceId,
                        Collectors.mapping(ServiceBusiness::getBusinessId, Collectors.toList())));
        List<Long> businessIdList = serviceBusinessList.stream().map(ServiceBusiness::getBusinessId).distinct().toList();
        Map<Long, SysBusiness> businessMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(businessIdList)) {
            List<SysBusiness> businessList = sysBusinessManager.listByIds(businessIdList);
            if (CollUtil.isNotEmpty(businessList)) {
                businessMap = businessList.stream().collect(Collectors.toMap(SysBusiness::getId, b -> b));
            }
        }
        List<com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessProductResp> businessProductList =
                CollUtil.isEmpty(businessIdList) ? Collections.emptyList() : sysBusinessProductMapper.getProductListByBusinessIdList(businessIdList);
        Map<Long, List<com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessProductResp>> businessProductMap =
                businessProductList.stream().collect(Collectors.groupingBy(com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessProductResp::getBusinessId));
        Map<Long, SysBusiness> finalBusinessMap = businessMap;
        respList.forEach(item -> {
            List<Long> bizIds = serviceToBusinessIds.get(item.getId());
            if (CollUtil.isNotEmpty(bizIds)) {
                item.setBusinessIds(bizIds);
                item.setBusinessName(bizIds.stream()
                        .map(id -> finalBusinessMap.containsKey(id) ? finalBusinessMap.get(id).getBusinessName() : null)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.joining(",")));
                List<ServiceProductInfo> allProducts = new ArrayList<>();
                for (Long bid : bizIds) {
                    List<com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessProductResp> products = businessProductMap.get(bid);
                    if (CollUtil.isNotEmpty(products)) {
                        allProducts.addAll(CopierCopyUtils.convertTo(products, ServiceProductInfo.class));
                    }
                }
                if (CollUtil.isNotEmpty(allProducts)) {
                    item.setProductInfoList(allProducts);
                }
            }
        });
        List<ServiceMaintenanceProcess> serviceProcessList = serviceMaintenanceProcessManager.lambdaQuery()
                .in(ServiceMaintenanceProcess::getServiceId, idList)
                .list();
        if(CollUtil.isNotEmpty(serviceProcessList)){
            Map<Long, List<ServiceMaintenanceProcess>> serviceProcessMap = serviceProcessList.stream().collect(Collectors.groupingBy(ServiceMaintenanceProcess::getServiceId));
            respList.forEach(item -> {
                if(CollUtil.isNotEmpty(serviceProcessMap) && CollUtil.isNotEmpty(serviceProcessMap.get(item.getId()))){
                    item.setProcessInfoList(CopierCopyUtils.convertTo(serviceProcessMap.get(item.getId()), ServiceProcessInfo.class));
                }
            });
        }
        return respList;
    }

    /**
     * 获取服务维护细节
     */
    public GetServiceMaintenanceInfoResp getServiceMaintenanceDetail(Long id) {
        GetServiceMaintenanceInfoResp resp = new GetServiceMaintenanceInfoResp();
        ServiceMaintenance serviceMaintenance = serviceMaintenanceManager
                .lambdaQuery()
                .eq(ServiceMaintenance::getId, id)
                .one();
        if(ObjectUtils.isNotEmpty(serviceMaintenance)){
            resp = CopierCopyUtils.convertTo(serviceMaintenance, GetServiceMaintenanceInfoResp.class);
            List<ServiceBusiness> sbList = serviceBusinessManager.lambdaQuery()
                    .eq(ServiceBusiness::getServiceId, id)
                    .list();
            if (CollUtil.isNotEmpty(sbList)) {
                List<Long> bizIds = sbList.stream().map(ServiceBusiness::getBusinessId).toList();
                resp.setBusinessIds(bizIds);
                List<SysBusiness> businessList = sysBusinessManager.listByIds(bizIds);
                if (CollUtil.isNotEmpty(businessList)) {
                    resp.setBusinessName(businessList.stream().map(SysBusiness::getBusinessName).collect(Collectors.joining(",")));
                }
                List<com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessProductResp> allProducts =
                        sysBusinessProductMapper.getProductListByBusinessIdList(bizIds);
                if (CollUtil.isNotEmpty(allProducts)) {
                    resp.setProductInfoList(CopierCopyUtils.convertTo(allProducts, ServiceProductInfo.class));
                }
            }
        }
        return resp;
    }

    /**
     * 新增服务维护
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addServiceMaintenance(AddServiceMaintenanceListReq addReq, LoginUser loginUser) {
        if (CollUtil.isEmpty(addReq.getBusinessIds())) {
            throw new BusinessException("请至少选择一个业务！");
        }
        this.checkServiceName(addReq.getServiceName(), null);
        ServiceMaintenance serviceMaintenance = CopierCopyUtils.convertTo(addReq, ServiceMaintenance.class);
        if (ObjectUtils.isNotEmpty(loginUser)) {
            serviceMaintenance.setCreateBy(loginUser.getUsername());
            serviceMaintenance.setCreateByName(loginUser.getRealName());
        }
        serviceMaintenance.setCreateTime(new Date());
        serviceMaintenanceManager.save(serviceMaintenance);
        for (Long businessId : addReq.getBusinessIds()) {
            ServiceBusiness sb = new ServiceBusiness();
            sb.setServiceId(serviceMaintenance.getId());
            sb.setBusinessId(businessId);
            serviceBusinessManager.save(sb);
        }
        return Boolean.TRUE;
    }

    /**
     * 修改服务维护
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean editServiceMaintenance(UpdateServiceMaintenanceListReq updateReq, LoginUser loginUser) {
        if (CollUtil.isEmpty(updateReq.getBusinessIds())) {
            throw new BusinessException("请至少选择一个业务！");
        }
        this.checkServiceName(updateReq.getServiceName(), updateReq.getId());
        ServiceMaintenance serviceMaintenance = CopierCopyUtils.convertTo(updateReq, ServiceMaintenance.class);
        if (ObjectUtils.isNotEmpty(loginUser)) {
            serviceMaintenance.setUpdateBy(loginUser.getUsername());
            serviceMaintenance.setUpdateByName(loginUser.getRealName());
        }
        serviceMaintenance.setUpdateTime(new Date());
        serviceMaintenanceManager.updateById(serviceMaintenance);
        serviceBusinessManager.lambdaUpdate().eq(ServiceBusiness::getServiceId, updateReq.getId()).remove();
        for (Long businessId : updateReq.getBusinessIds()) {
            ServiceBusiness sb = new ServiceBusiness();
            sb.setServiceId(updateReq.getId());
            sb.setBusinessId(businessId);
            serviceBusinessManager.save(sb);
        }
        return Boolean.TRUE;
    }

    /**
     * 添加服务产品关联信息
     * @param productInfoList
     * @param serviceMaintenanceId
     * @param loginUser
     */
    @Transactional(rollbackFor = Exception.class)
    public void addServiceProductInfo(List<ServiceProductInfo> productInfoList, Long serviceMaintenanceId, LoginUser loginUser){
        List<ServiceMaintenanceProduct> serviceProductList = productInfoList.stream().map(item -> {
            ServiceMaintenanceProduct info = CopierCopyUtils.convertTo(item, ServiceMaintenanceProduct.class);
            info.setServiceId(serviceMaintenanceId);
            if (ObjectUtils.isNotEmpty(loginUser)) {
                info.setCreateBy(loginUser.getUsername());
                info.setCreateByName(loginUser.getRealName());
            }
            info.setCreateTime(new Date());
            return info;
        }).toList();
        serviceMaintenanceProductManager.saveBatch(serviceProductList);
    }

    /**
     * 校验产品是否存在其他服务
     * @param productIdList
     * @return
     */
    public void checkProductInfo(List<Long> productIdList, Long serviceId){
        List<ServiceMaintenanceProduct> list = serviceMaintenanceProductManager.lambdaQuery()
                .in(ServiceMaintenanceProduct::getProductId, productIdList)
                .ne(ObjectUtils.isNotEmpty(serviceId), ServiceMaintenanceProduct::getServiceId, serviceId)
                .list();
        if(CollUtil.isNotEmpty(list)){
            List<String> productNameList = list.stream().map(ServiceMaintenanceProduct::getProductName).distinct().toList();
            throw new BusinessException(MessageFormat.format(ResultMsg.SERVICE_PRODUCT_SAVE_EXIST, StringUtils.join(productNameList, GlobalConstant.Symbol.COMMA)));
        }
    }

    /**
     * 校验服务名称是否存在
     * @param serviceName
     * @param serviceId
     */
    public void checkServiceName(String serviceName, Long serviceId){
        List<ServiceMaintenance> list = serviceMaintenanceManager.lambdaQuery()
                .eq(ServiceMaintenance::getServiceName, serviceName)
                .ne(ObjectUtils.isNotEmpty(serviceId), ServiceMaintenance::getId, serviceId)
                .list();
        if(CollUtil.isNotEmpty(list)){
            throw new BusinessException(ResultMsg.SERVICE_ADD_NAME_EXIST);
        }
    }

    /**
     * 删除服务维护
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteServiceMaintenance(Long id, LoginUser loginUser) {
        if(ObjectUtils.isEmpty(id)){
            return Boolean.FALSE;
        }
        serviceMaintenanceManager.lambdaUpdate()
                .set(ServiceMaintenance::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(ObjectUtils.isNotEmpty(loginUser), ServiceMaintenance::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), ServiceMaintenance::getUpdateByName, loginUser.getRealName())
                .set(ServiceMaintenance::getUpdateTime, new Date())
                .eq(ServiceMaintenance::getId, id)
                .update();
        serviceMaintenanceProductManager.lambdaUpdate()
                .set(ServiceMaintenanceProduct::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(ObjectUtils.isNotEmpty(loginUser), ServiceMaintenanceProduct::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), ServiceMaintenanceProduct::getUpdateByName, loginUser.getRealName())
                .set(ServiceMaintenanceProduct::getUpdateTime, new Date())
                .eq(ServiceMaintenanceProduct::getServiceId, id)
                .update();
        serviceMaintenanceProcessManager.lambdaUpdate()
                .set(ServiceMaintenanceProcess::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(ObjectUtils.isNotEmpty(loginUser), ServiceMaintenanceProcess::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), ServiceMaintenanceProcess::getUpdateByName, loginUser.getRealName())
                .set(ServiceMaintenanceProcess::getUpdateTime, new Date())
                .eq(ServiceMaintenanceProcess::getServiceId, id)
                .update();
        serviceBusinessManager.lambdaUpdate().eq(ServiceBusiness::getServiceId, id).remove();
        return Boolean.TRUE;
    }

    /**
     * 分配流程
     * @param req
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean distributeProcess(DistributeServiceProcessReq req, LoginUser loginUser) {
        if(ObjectUtils.isEmpty(req.getId())){
            return Boolean.FALSE;
        }
        //先删除后添加
        serviceMaintenanceProcessManager.lambdaUpdate()
                .set(ServiceMaintenanceProcess::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(ObjectUtils.isNotEmpty(loginUser), ServiceMaintenanceProcess::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), ServiceMaintenanceProcess::getUpdateByName, loginUser.getRealName())
                .set(ServiceMaintenanceProcess::getUpdateTime, new Date())
                .eq(ServiceMaintenanceProcess::getServiceId, req.getId())
                .update();
        if(CollUtil.isNotEmpty(req.getProcessInfoList())) {
            List<ServiceMaintenanceProcess> serviceMaintenanceProcessList = req.getProcessInfoList().stream().map(item -> {
                ServiceMaintenanceProcess serviceProcess = CopierCopyUtils.convertTo(item, ServiceMaintenanceProcess.class);
                serviceProcess.setServiceId(req.getId());
                if (ObjectUtils.isNotEmpty(loginUser)) {
                    serviceProcess.setCreateBy(loginUser.getUsername());
                    serviceProcess.setCreateByName(loginUser.getRealName());
                }
                serviceProcess.setCreateTime(new Date());
                return serviceProcess;
            }).toList();
            serviceMaintenanceProcessManager.saveBatch(serviceMaintenanceProcessList);
        }
        return Boolean.TRUE;
    }

    /**
     *  获取服务流程
     * @param req
     * @return
     */
    public List<GetServiceProcessInfoResp> getServiceProcessList(GetServiceProcessListReq req) {
        List<GetServiceProcessInfoResp> respList = serviceProcessService.getListByServiceId(req.getId());
        if(CollUtil.isNotEmpty(respList)){
            //特定流程赋初始值
            respList.forEach(item -> {
                if(DictConstants.SERVICE_PROCESS_CREATE_ACCOUNT_SET.equals(item.getProcessCode())){
                    String taskData = item.getBackupFiled();
                    CustomerCompany customerCompany = customerCompanyManager.getById(req.getCompanyId());
                    if(ObjectUtils.isNotEmpty(customerCompany)) {
                        JSONObject jsonObject = JSON.parseObject(taskData);
                        if(jsonObject.containsKey("own")) {
                            JSONArray ownArray = jsonObject.getJSONArray("own");
                            for (int i = 0; i < ownArray.size(); i++) {
                                JSONObject data = ownArray.getJSONObject(i);
                                if ("companyName".equals(data.getString("prop"))) {
                                    data.put("value", customerCompany.getCompanyName());
                                }
                                if ("companyType".equals(data.getString("prop"))) {
                                    data.put("value", customerCompany.getCompanyType());
                                }
                                if ("creditCode".equals(data.getString("prop"))) {
                                    data.put("value", customerCompany.getTaxNumber());
                                }
                            }
                        }
                        taskData = jsonObject.toJSONString();
                    }
                    item.setBackupFiled(taskData);
                }
            });
        }
        return respList;
    }

    /**
     * 获取服务维护信息
     * @param productId
     * @return
     */
    public GetServiceMaintenanceInfoResp getServiceByProductId(Long productId) {
        GetServiceMaintenanceInfoResp resp = new GetServiceMaintenanceInfoResp();
        if (ObjectUtils.isEmpty(productId)) {
            return resp;
        }
        ServiceMaintenance serviceMaintenance = serviceMaintenanceMapper.getServiceByProductId(productId);
        if (ObjectUtils.isNotEmpty(serviceMaintenance)) {
            resp = CopierCopyUtils.convertTo(serviceMaintenance, GetServiceMaintenanceInfoResp.class);
            Long serviceId = serviceMaintenance.getId();
            List<ServiceBusiness> sbList = serviceBusinessManager.lambdaQuery()
                    .eq(ServiceBusiness::getServiceId, serviceId)
                    .list();
            if (CollUtil.isNotEmpty(sbList)) {
                List<Long> bizIds = sbList.stream().map(ServiceBusiness::getBusinessId).toList();
                resp.setBusinessIds(bizIds);
                List<SysBusiness> businessList = sysBusinessManager.listByIds(bizIds);
                if (CollUtil.isNotEmpty(businessList)) {
                    resp.setBusinessName(businessList.stream().map(SysBusiness::getBusinessName).collect(Collectors.joining(",")));
                }
                List<com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessProductResp> allProducts = new ArrayList<>();
                for (Long bid : bizIds) {
                    List<com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessProductResp> list =
                            sysBusinessProductMapper.getProductListByBusinessId(bid);
                    if (CollUtil.isNotEmpty(list)) {
                        allProducts.addAll(list);
                    }
                }
                if (CollUtil.isNotEmpty(allProducts)) {
                    resp.setProductInfoList(CopierCopyUtils.convertTo(allProducts, ServiceProductInfo.class));
                }
            }
        }
        return resp;
    }
}
