package com.qykj.finmaster.contract.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.constansts.FileConstants;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.ChineseAmountUtil;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.DocxTemplateUtil;
import com.qykj.finmaster.common.utils.XwpfHtmlUtil;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.contract.domain.Contract;
import com.qykj.finmaster.contract.domain.ContractDetail;
import com.qykj.finmaster.contract.domain.req.AddContractReq;
import com.qykj.finmaster.customer.company.domain.CompanyService;
import com.qykj.finmaster.customer.company.domain.CompanyServiceRenew;
import com.qykj.finmaster.customer.company.manager.impl.CompanyServiceRenewManagerImpl;
import com.qykj.finmaster.customer.company.domain.req.AddCompanyRenewReq;
import com.qykj.finmaster.customer.company.manager.impl.CompanyServiceManagerImpl;
import com.qykj.finmaster.customer.company.service.CompanyServiceServiceImpl;
import com.qykj.finmaster.contract.domain.req.ContractDetailReq;
import com.qykj.finmaster.contract.domain.req.GetContractListReq;
import com.qykj.finmaster.contract.domain.req.UpdateContractReq;
import com.qykj.finmaster.contract.domain.resp.ContractDetailInfoResp;
import com.qykj.finmaster.contract.domain.resp.GetContractDetailResp;
import com.qykj.finmaster.contract.domain.resp.GetContractListResp;
import com.qykj.finmaster.contract.domain.resp.OrderDetailForContractResp;
import com.qykj.finmaster.contract.manager.impl.ContractDetailManagerImpl;
import com.qykj.finmaster.contract.manager.impl.ContractManagerImpl;
import com.qykj.finmaster.contract.mapper.ContractMapper;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.order.domain.OrderDetail;
import com.qykj.finmaster.order.manager.impl.OrderDetailManagerImpl;
import com.qykj.finmaster.order.manager.impl.OrderManagerImpl;
import com.qykj.finmaster.order.mapper.OrderDetailMapper;
import com.qykj.finmaster.order.service.OrderServiceImpl;
import com.qykj.finmaster.registerAddress.domain.RegisterAddress;
import com.qykj.finmaster.registerAddress.manager.impl.RegisterAddressManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.bussiness.domain.SysBusiness;
import com.qykj.finmaster.system.bussiness.manager.impl.SysBusinessManagerImpl;
import com.qykj.finmaster.system.dict.entity.SysDictData;
import com.qykj.finmaster.system.dict.mapper.SysDictDataMapper;
import com.qykj.finmaster.system.bussiness.domain.resp.GetBusinessProductResp;
import com.qykj.finmaster.system.bussiness.mapper.SysBusinessProductMapper;
import com.qykj.finmaster.system.product.domain.Product;
import com.qykj.finmaster.system.product.manager.impl.ProductManagerImpl;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceBusiness;
import com.qykj.finmaster.system.serviceMaintenance.manager.impl.ServiceBusinessManagerImpl;
import com.qykj.finmaster.task.domain.CommonTask;
import com.qykj.finmaster.task.manager.impl.CommonTaskManagerImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 合同Service实现
 */
@lombok.extern.slf4j.Slf4j
@Service
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> {

    @Autowired
    private ContractManagerImpl contractManager;
    @Autowired
    private ContractDetailManagerImpl contractDetailManager;
    @Autowired
    private ContractMapper contractMapper;
    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;
    @Autowired
    private OrderManagerImpl orderManager;
    @Autowired
    private OrderDetailManagerImpl orderDetailManager;
    @Autowired
    private ProductManagerImpl productManager;
    @Autowired
    private RegisterAddressManagerImpl registerAddressManager;
    @Autowired
    private SysBusinessManagerImpl sysBusinessManager;
    @Autowired
    private SysBusinessProductMapper sysBusinessProductMapper;
    @Autowired
    private ServiceBusinessManagerImpl serviceBusinessManager;
    @Lazy
    @Autowired
    private OrderServiceImpl orderService;
    @Autowired
    private CompanyServiceManagerImpl companyServiceManager;
    @Autowired
    private CompanyServiceServiceImpl companyServiceService;
    @Autowired
    private CompanyServiceRenewManagerImpl companyServiceRenewManager;
    @Autowired
    private CommonTaskManagerImpl commonTaskManager;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private SysDictDataMapper sysDictDataMapper;

    /**
     * 查询合同列表（分页）
     */
    public List<GetContractListResp> getContractList(GetContractListReq req, Long tenantId) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        params.put("tenantId", tenantId);
        List<GetContractListResp> list = contractMapper.getContractList(params);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        list.forEach(item -> {
            // 日期转换已在Mapper中完成，这里不需要额外转换
        });
        return list;
    }

    /**
     * 续费任务详情专用：按 companyId 查公司全部合同（绕过数据权限 / 不分页）。
     * 与 {@link #getContractList} 同源，只是不应用 dataScopeUserIds。
     */
    public List<GetContractListResp> getContractListBypassDataScope(GetContractListReq req, Long tenantId) {
        Map<String, Object> params = req.toBeanMap();
        params.remove("dataScopeUserIds");
        params.put("tenantId", tenantId);
        List<GetContractListResp> list = contractMapper.getContractList(params);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list;
    }

    /**
     * 获取合同详情
     */
    public GetContractDetailResp getContractDetail(Long id) {
        Contract contract = contractManager.getById(id);
        if (ObjectUtils.isEmpty(contract)) {
            return null;
        }
        GetContractDetailResp resp = new GetContractDetailResp();
        resp.setId(contract.getId());
        resp.setContractNo(contract.getContractNo());
        resp.setCompanyId(contract.getCompanyId());
        resp.setBusinessId(contract.getBusinessId());
        resp.setContractType(contract.getContractType());
        resp.setContractStartTime(DateUtils.dateToLocalDateTime(contract.getContractStartTime()));
        resp.setContractEndTime(DateUtils.dateToLocalDateTime(contract.getContractEndTime()));
        resp.setContractPrice(contract.getContractPrice());
        resp.setHaveOrder(contract.getHaveOrder());
        resp.setHaveAddress(contract.getHaveAddress());
        resp.setStatus(contract.getStatus());
        resp.setRemark(contract.getRemark());
        resp.setCreateByName(contract.getCreateByName());
        resp.setCreateTime(DateUtils.dateToLocalDateTime(contract.getCreateTime()));
        resp.setUpdateByName(contract.getUpdateByName());
        resp.setUpdateTime(DateUtils.dateToLocalDateTime(contract.getUpdateTime()));
        // 查询合同明细
        List<ContractDetail> detailList = contractDetailManager.lambdaQuery()
                .eq(ContractDetail::getContractId, id)
                .list();
        
        if (CollUtil.isNotEmpty(detailList)) {
            List<Long> orderDetailIds = detailList.stream()
                    .map(ContractDetail::getOrderDetailId)
                    .filter(ObjectUtils::isNotEmpty)
                    .distinct()
                    .toList();
            
            List<Long> productIds = detailList.stream()
                    .map(ContractDetail::getProductId)
                    .filter(ObjectUtils::isNotEmpty)
                    .distinct()
                    .toList();
            
            List<Long> addressIds = detailList.stream()
                    .map(ContractDetail::getCompanyAddressId)
                    .filter(ObjectUtils::isNotEmpty)
                    .distinct()
                    .toList();
            
            // 批量查询订单明细
            Map<Long, OrderDetail> orderDetailMap = Collections.emptyMap();
            if (CollUtil.isNotEmpty(orderDetailIds)) {
                orderDetailMap = orderDetailManager.listByIds(orderDetailIds).stream()
                        .collect(Collectors.toMap(OrderDetail::getId, od -> od));
            }
            
            // 批量查询产品（使用最终变量，便于在 lambda 中使用）
            Map<Long, Product> productMap = CollUtil.isNotEmpty(productIds)
                    ? productManager.listByIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, p -> p))
                    : Collections.emptyMap();
            
            // 批量查询地址（使用最终变量，便于在 lambda 中使用）
            Map<Long, RegisterAddress> addressMap = CollUtil.isNotEmpty(addressIds)
                    ? registerAddressManager.listByIds(addressIds).stream()
                        .collect(Collectors.toMap(RegisterAddress::getId, a -> a))
                    : Collections.emptyMap();
            
            // 组装明细信息
            List<ContractDetailInfoResp> detailRespList = detailList.stream().map(detail -> {
                ContractDetailInfoResp detailResp = CopierCopyUtils.convertTo(detail, ContractDetailInfoResp.class);
                
                // 设置产品信息
                if (detail.getProductId() != null && productMap.containsKey(detail.getProductId())) {
                    Product product = productMap.get(detail.getProductId());
                    detailResp.setProductName(product.getProductName());
                    detailResp.setProductNumber(product.getProductNumber());
                }
                
                // 设置地址信息
                if (detail.getCompanyAddressId() != null && addressMap.containsKey(detail.getCompanyAddressId())) {
                    RegisterAddress address = addressMap.get(detail.getCompanyAddressId());
                    detailResp.setAddress(address.getAddress());
                }
                
                return detailResp;
            }).toList();
            
            resp.setDetailList(detailRespList);
        }
        
        // 设置公司信息
        if (contract.getCompanyId() != null) {
            CustomerCompany company = customerCompanyManager.getById(contract.getCompanyId());
            if (ObjectUtils.isNotEmpty(company)) {
                resp.setCompanyName(company.getCompanyName());
            }
        }
        
        // 设置业务信息
        if (contract.getBusinessId() != null) {
            SysBusiness business = sysBusinessManager.getById(contract.getBusinessId());
            if (ObjectUtils.isNotEmpty(business)) {
                resp.setBusinessName(business.getBusinessName());
            }
        }
        
        return resp;
    }

    /**
     * 新增合同
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addContract(AddContractReq req, Long tenantId, LoginUser loginUser) {
        if (ObjectUtils.isEmpty(req.getCompanyId())) {
            throw new BusinessException("公司ID不能为空！");
        }
        if (ObjectUtils.isEmpty(req.getBusinessId())) {
            throw new BusinessException("业务类型不能为空！");
        }
        if (ObjectUtils.isEmpty(req.getContractStartTime()) || ObjectUtils.isEmpty(req.getContractEndTime())) {
            throw new BusinessException("合同开始时间和结束时间不能为空！");
        }
        if (req.getContractPrice() == null) {
            throw new BusinessException("合同价格不能为空！");
        }
        
        // 校验公司是否存在
        CustomerCompany company = customerCompanyManager.getById(req.getCompanyId());
        if (ObjectUtils.isEmpty(company)) {
            throw new BusinessException("公司不存在！");
        }
        
        // 校验业务是否存在
        SysBusiness business = sysBusinessManager.getById(req.getBusinessId());
        if (ObjectUtils.isEmpty(business)) {
            throw new BusinessException("业务类型不存在！");
        }
        
        // 如果基于订单创建，校验订单明细
        if (CollUtil.isNotEmpty(req.getDetailList())) {
            // 校验所有订单明细必须属于同一公司
            List<Long> orderDetailIds = req.getDetailList().stream()
                    .map(ContractDetailReq::getOrderDetailId)
                    .filter(ObjectUtils::isNotEmpty)
                    .toList();
            
            if (CollUtil.isNotEmpty(orderDetailIds)) {
                List<OrderDetail> orderDetails = orderDetailManager.listByIds(orderDetailIds);
                for (OrderDetail od : orderDetails) {
                    if (!req.getCompanyId().equals(od.getCompanyId())) {
                        throw new BusinessException("所有订单明细必须属于同一公司！");
                    }
                }
            }
        }

        // 特定业务类型必须“存在订单”：由业务配置 contract_related_order 控制
        boolean forceHaveOrder = business.getContractRelatedOrder() != null && business.getContractRelatedOrder().equals(GlobalConstant.Number.ONE_INT);
        if (forceHaveOrder) {
            boolean hasDetailWithOrder = CollUtil.isNotEmpty(req.getDetailList())
                    && req.getDetailList().stream().anyMatch(d -> d.getOrderDetailId() != null);
            if (!hasDetailWithOrder) {
                throw new BusinessException("当前业务类型必须关联至少一条订单明细！");
            }
        }
        
        // 生成合同编号（确保唯一性）
        String contractNo = generateContractNo();
        int retryCount = 0;
        while (contractManager.lambdaQuery()
                .eq(Contract::getContractNo, contractNo)
                .exists() && retryCount < 10) {
            contractNo = generateContractNo();
            retryCount++;
        }
        if (retryCount >= 10) {
            throw new BusinessException("生成合同编号失败，请重试！");
        }
        
        // 创建合同
        Contract contract = CopierCopyUtils.convertTo(req, Contract.class);
        contract.setContractNo(contractNo);
        contract.setTenantId(tenantId);
        // 根据公司+业务自动计算合同类型（contract_new / contract_renew）
        String contractType = getAutoContractType(req.getCompanyId(), req.getBusinessId(), tenantId);
        contract.setContractType(contractType);
        // 任务单标识：
        // - 新增合同：仅首个“同服务”的新增合同允许转任务单；
        // - 续费合同：若仅存在一份同服务合同且该合同 remark 为“伽玛账数据导入”，视为首次“正式”合同，允许转任务单。
        Integer transferTaskFlag = GlobalConstant.Number.ZERO_INT;
        if (DictConstants.CONTRACT_TYPE_NEW.equals(contractType) || DictConstants.CONTRACT_TYPE_RENEW.equals(contractType)) {
            // 1) 找到当前业务对应的服务ID集合
            List<ServiceBusiness> currentBizServices = serviceBusinessManager.lambdaQuery()
                    .eq(ServiceBusiness::getBusinessId, req.getBusinessId())
                    .list();
            if (CollUtil.isEmpty(currentBizServices)) {
                // 若未配置服务关联，则新增/续费合同都不可转任务单
                transferTaskFlag = GlobalConstant.Number.ZERO_INT;
            } else {
                Set<Long> serviceIds = currentBizServices.stream()
                        .map(ServiceBusiness::getServiceId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                if (CollUtil.isEmpty(serviceIds)) {
                    // 未能解析出服务ID，同样视为未配置服务关联，不可转任务单
                    transferTaskFlag = GlobalConstant.Number.ZERO_INT;
                } else {
                    // 2) 找到这些服务下关联的所有业务ID（一个服务可关联多个业务）
                    List<ServiceBusiness> allBizForServices = serviceBusinessManager.lambdaQuery()
                            .in(ServiceBusiness::getServiceId, serviceIds)
                            .list();
                    Set<Long> relatedBusinessIds = allBizForServices.stream()
                            .map(ServiceBusiness::getBusinessId)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    // 查询当前公司在“同服务”下所有已完成合同
                    List<Contract> sameServiceCompletedContracts = contractManager.lambdaQuery()
                            .eq(Contract::getCompanyId, req.getCompanyId())
                            .eq(Contract::getStatus, DictConstants.CONTRACT_STATUS_COMPLETED)
                            .in(CollUtil.isNotEmpty(relatedBusinessIds), Contract::getBusinessId, relatedBusinessIds)
                            .list();
                    if (DictConstants.CONTRACT_TYPE_NEW.equals(contractType)) {
                        // 新增合同：仅当不存在同服务已完成合同时允许转任务单
                        transferTaskFlag = CollUtil.isEmpty(sameServiceCompletedContracts)
                                ? GlobalConstant.Number.ONE_INT
                                : GlobalConstant.Number.ZERO_INT;
                    } else {
                        // 续费合同：若仅存在一份同服务已完成合同，且该合同为“伽玛账数据导入”，则允许转任务单
                        /*transferTaskFlag = (sameServiceCompletedContracts.size() == 1
                                && "伽玛账数据导入".equals(sameServiceCompletedContracts.get(0).getRemark()))
                                ? GlobalConstant.Number.ONE_INT
                                : GlobalConstant.Number.ZERO_INT;*/
                        transferTaskFlag = GlobalConstant.Number.ONE_INT;
                    }
                }
            }
        }
        contract.setTransferTask(transferTaskFlag);
        contract.setStatus(DictConstants.CONTRACT_STATUS_DRAFT); // 默认草稿状态
        // 是否存在订单：只要有明细且至少一条有订单明细ID
        boolean hasOrder = CollUtil.isNotEmpty(req.getDetailList()) &&
                req.getDetailList().stream().anyMatch(d -> d.getOrderDetailId() != null);
        // 是否存在地址：只要有明细且至少一条有关联地址
        boolean hasAddress = CollUtil.isNotEmpty(req.getDetailList()) &&
                req.getDetailList().stream().anyMatch(d -> d.getCompanyAddressId() != null);
        contract.setHaveOrder(hasOrder ? GlobalConstant.Number.ONE_INT : GlobalConstant.Number.ZERO_INT);
        contract.setHaveAddress(hasAddress ? GlobalConstant.Number.ONE_INT : GlobalConstant.Number.ZERO_INT);
        contract.setCreateTime(new Date());
        if (ObjectUtils.isNotEmpty(loginUser)) {
            contract.setCreateBy(loginUser.getUsername());
            contract.setCreateByName(loginUser.getRealName());
        }
        contractManager.save(contract);
        
        // 创建合同明细
        if (CollUtil.isNotEmpty(req.getDetailList())) {
            List<ContractDetail> detailList = req.getDetailList().stream().map(detailReq -> {
                ContractDetail detail = CopierCopyUtils.convertTo(detailReq, ContractDetail.class);
                detail.setContractId(contract.getId());
                detail.setTenantId(tenantId);
                detail.setCreateTime(new Date());
                if (ObjectUtils.isNotEmpty(loginUser)) {
                    detail.setCreateBy(loginUser.getUsername());
                    detail.setCreateByName(loginUser.getRealName());
                }
                
                // 如果有关联订单明细，获取订单号和产品ID
                if (detail.getOrderDetailId() != null) {
                    OrderDetail orderDetail = orderDetailManager.getById(detail.getOrderDetailId());
                    if (ObjectUtils.isNotEmpty(orderDetail)) {
                        Order order = orderManager.getById(orderDetail.getOrderId());
                        if (ObjectUtils.isNotEmpty(order)) {
                            detail.setOrderNo(order.getOrderNo());
                        }
                        if (orderDetail.getProductId() != null) {
                            detail.setProductId(orderDetail.getProductId());
                        }
                    }
                }
                
                return detail;
            }).toList();
            
            contractDetailManager.saveBatch(detailList);
            
            // 更新地址状态为使用中
            List<Long> addressIds = req.getDetailList().stream()
                    .map(ContractDetailReq::getCompanyAddressId)
                    .filter(ObjectUtils::isNotEmpty)
                    .distinct()
                    .toList();
            
            if (CollUtil.isNotEmpty(addressIds)) {
                for (Long addressId : addressIds) {
                    RegisterAddress address = registerAddressManager.getById(addressId);
                    if (ObjectUtils.isNotEmpty(address) && address.getStatus() != null && address.getStatus() == GlobalConstant.Number.ZERO_INT) {
                        address.setStatus(GlobalConstant.Number.ONE_INT); // 使用中
                        address.setUpdateTime(new Date());
                        if (ObjectUtils.isNotEmpty(loginUser)) {
                            address.setUpdateBy(loginUser.getUsername());
                            address.setUpdateByName(loginUser.getRealName());
                        }
                        registerAddressManager.updateById(address);
                    }
                }
            }
        }
        
        return Boolean.TRUE;
    }

    /**
     * 编辑合同
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateContract(UpdateContractReq req, Long tenantId, LoginUser loginUser) {
        if (ObjectUtils.isEmpty(req.getId())) {
            throw new BusinessException("未获取到要编辑的合同ID！");
        }

        Contract contract = contractManager.getById(req.getId());
        if (ObjectUtils.isEmpty(contract)) {
            throw new BusinessException("合同不存在！");
        }
        // 合同修改规则：
        //   - DRAFT：全字段可改（走完整路径）
        //   - COMPLETED + NEW：只允许改 contractStartTime / contractEndTime / remark，并联动同步公司服务
        //   - COMPLETED + RENEW / TERMINATED / DISUSED：禁止
        boolean isDraft = DictConstants.CONTRACT_STATUS_DRAFT.equals(contract.getStatus());
        boolean isCompletedNew = DictConstants.CONTRACT_STATUS_COMPLETED.equals(contract.getStatus())
                && DictConstants.CONTRACT_TYPE_NEW.equals(contract.getContractType());
        if (!isDraft && !isCompletedNew) {
            if (DictConstants.CONTRACT_STATUS_COMPLETED.equals(contract.getStatus())
                    && DictConstants.CONTRACT_TYPE_RENEW.equals(contract.getContractType())) {
                throw new BusinessException("续费合同完成后不允许修改，如需调整请废弃后重新发起");
            }
            throw new BusinessException("合同当前状态不允许修改");
        }

        // 已完成（NEW）合同的"仅时间字段"修改路径：跳过地址 / detail / 标识等所有大动作
        if (isCompletedNew) {
            updateCompletedNewContractTime(contract, req, loginUser);
            return Boolean.TRUE;
        }

        // 1. 先处理地址释放：释放旧明细中占用的地址
        List<ContractDetail> oldDetailList = contractDetailManager.lambdaQuery()
                .eq(ContractDetail::getContractId, req.getId())
                .list();
        if (CollUtil.isNotEmpty(oldDetailList)) {
            List<Long> oldAddressIds = oldDetailList.stream()
                    .map(ContractDetail::getCompanyAddressId)
                    .filter(ObjectUtils::isNotEmpty)
                    .distinct()
                    .toList();
            if (CollUtil.isNotEmpty(oldAddressIds)) {
                for (Long addressId : oldAddressIds) {
                    RegisterAddress address = registerAddressManager.getById(addressId);
                    if (ObjectUtils.isNotEmpty(address)) {
                        address.setStatus(GlobalConstant.Number.ZERO_INT); // 未使用
                        address.setUpdateTime(new Date());
                        if (ObjectUtils.isNotEmpty(loginUser)) {
                            address.setUpdateBy(loginUser.getUsername());
                            address.setUpdateByName(loginUser.getRealName());
                        }
                        registerAddressManager.updateById(address);
                    }
                }
            }
        }

        // 更新合同基本信息
        contract.setContractType(req.getContractType());
        contract.setContractStartTime(req.getContractStartTime());
        contract.setContractEndTime(req.getContractEndTime());
        contract.setContractPrice(req.getContractPrice());
        contract.setRemark(req.getRemark());
        contract.setUpdateTime(new Date());
        if (ObjectUtils.isNotEmpty(loginUser)) {
            contract.setUpdateBy(loginUser.getUsername());
            contract.setUpdateByName(loginUser.getRealName());
        }
        contractManager.updateById(contract);
        
        // 更新合同明细（先删除后添加）
        if (CollUtil.isNotEmpty(req.getDetailList())) {
            // 删除原有明细
            contractDetailManager.lambdaUpdate()
                    .set(ContractDetail::getDeleted, GlobalConstant.Number.ONE_INT)
                    .set(ContractDetail::getUpdateTime, new Date())
                    .set(ObjectUtils.isNotEmpty(loginUser), ContractDetail::getUpdateBy, loginUser.getUsername())
                    .set(ObjectUtils.isNotEmpty(loginUser), ContractDetail::getUpdateByName, loginUser.getRealName())
                    .eq(ContractDetail::getContractId, req.getId())
                    .update();
            
            // 添加新明细
            List<ContractDetail> detailList = req.getDetailList().stream().map(detailReq -> {
                ContractDetail detail = CopierCopyUtils.convertTo(detailReq, ContractDetail.class);
                detail.setContractId(req.getId());
                detail.setTenantId(tenantId);
                detail.setCreateTime(new Date());
                if (ObjectUtils.isNotEmpty(loginUser)) {
                    detail.setCreateBy(loginUser.getUsername());
                    detail.setCreateByName(loginUser.getRealName());
                }
                
                // 如果有关联订单明细，获取订单号和产品ID
                if (detail.getOrderDetailId() != null) {
                    OrderDetail orderDetail = orderDetailManager.getById(detail.getOrderDetailId());
                    if (ObjectUtils.isNotEmpty(orderDetail)) {
                        Order order = orderManager.getById(orderDetail.getOrderId());
                        if (ObjectUtils.isNotEmpty(order)) {
                            detail.setOrderNo(order.getOrderNo());
                        }
                        if (orderDetail.getProductId() != null) {
                            detail.setProductId(orderDetail.getProductId());
                        }
                    }
                }
                
                return detail;
            }).toList();
            
            contractDetailManager.saveBatch(detailList);
            
            // 2. 地址占用：根据新的明细列表设置地址为“使用中”
            List<Long> newAddressIds = req.getDetailList().stream()
                    .map(ContractDetailReq::getCompanyAddressId)
                    .filter(ObjectUtils::isNotEmpty)
                    .distinct()
                    .toList();
            if (CollUtil.isNotEmpty(newAddressIds)) {
                for (Long addressId : newAddressIds) {
                    RegisterAddress address = registerAddressManager.getById(addressId);
                    if (ObjectUtils.isNotEmpty(address) && address.getStatus() != null && address.getStatus() == GlobalConstant.Number.ZERO_INT) {
                        address.setStatus(GlobalConstant.Number.ONE_INT); // 使用中
                        address.setUpdateTime(new Date());
                        if (ObjectUtils.isNotEmpty(loginUser)) {
                            address.setUpdateBy(loginUser.getUsername());
                            address.setUpdateByName(loginUser.getRealName());
                        }
                        registerAddressManager.updateById(address);
                    }
                }
            }

            // 更新合同标识（根据新的明细列表重新计算）
            boolean hasOrder = req.getDetailList().stream().anyMatch(d -> d.getOrderDetailId() != null);
            boolean hasAddress = req.getDetailList().stream().anyMatch(d -> d.getCompanyAddressId() != null);
            contract.setHaveOrder(hasOrder ? GlobalConstant.Number.ONE_INT : GlobalConstant.Number.ZERO_INT);
            contract.setHaveAddress(hasAddress ? GlobalConstant.Number.ONE_INT : GlobalConstant.Number.ZERO_INT);
        } else {
            // 如果没有明细，更新标识
            contract.setHaveOrder(GlobalConstant.Number.ZERO_INT);
            contract.setHaveAddress(GlobalConstant.Number.ZERO_INT);
        }
        contractManager.updateById(contract);

        return Boolean.TRUE;
    }

    /**
     * 已完成（COMPLETED）+ 新增（NEW）合同的「仅时间」修改路径。
     * 只取 req 中的 contractStartTime / contractEndTime / remark 三个字段更新，
     * 不动 contract_type / contract_price / contract_detail / 地址占用。
     * 完成后联动同步 company_service 的服务期。
     */
    private void updateCompletedNewContractTime(Contract contract, UpdateContractReq req, LoginUser loginUser) {
        if (req.getContractStartTime() == null || req.getContractEndTime() == null) {
            throw new BusinessException("合同开始/结束时间不能为空");
        }
        if (!req.getContractEndTime().after(req.getContractStartTime())) {
            throw new BusinessException("合同结束时间必须晚于开始时间");
        }
        contract.setContractStartTime(req.getContractStartTime());
        contract.setContractEndTime(req.getContractEndTime());
        if (req.getRemark() != null) {
            contract.setRemark(req.getRemark());
        }
        contract.setUpdateTime(new Date());
        if (ObjectUtils.isNotEmpty(loginUser)) {
            contract.setUpdateBy(loginUser.getUsername());
            contract.setUpdateByName(loginUser.getRealName());
        }
        contractManager.updateById(contract);

        // 联动同步关联的 company_service 日期
        syncCompanyServiceDates(contract);
    }

    /**
     * NEW 合同时间修改后联动同步关联的 company_service。
     * 规则：
     *   - 反查 company_service WHERE source_id = contract.id（NEW 合同与 cs 是 1:1）
     *   - 查不到 → log warn 跳过（不阻断主流程）
     *   - cs.status = CANCELLED → log info 跳过（关联服务已取消，不再同步）
     *   - 否则同步 effective_date / end_date；actual_end_date 仅当无 active renew 记录时同步
     */
    private void syncCompanyServiceDates(Contract contract) {
        try {
            CompanyService cs = companyServiceManager.lambdaQuery()
                    .eq(CompanyService::getSourceId, contract.getId())
                    .last("LIMIT 1")
                    .one();
            if (cs == null) {
                log.warn("[合同时间联动] 合同 {} 未找到关联的 company_service，跳过同步" + contract.getContractNo());
                return;
            }
            if ("CANCELLED".equals(cs.getStatus())) {
                log.info("[合同时间联动] 合同 {} 关联的 company_service 已取消，仅改合同不同步服务期",
                        contract.getContractNo());
                return;
            }

            java.time.LocalDate newStart = contract.getContractStartTime() != null
                    ? DateUtils.dateToLocalDateTime(contract.getContractStartTime()).toLocalDate()
                    : null;
            java.time.LocalDate newEnd = contract.getContractEndTime() != null
                    ? DateUtils.dateToLocalDateTime(contract.getContractEndTime()).toLocalDate()
                    : null;

            // 是否有 active 续费记录（决定要不要回写 actual_end_date）
            long renewCount = companyServiceRenewManager.lambdaQuery()
                    .eq(CompanyServiceRenew::getCompanyServiceId, cs.getId())
                    .count();

            CompanyService upd = new CompanyService();
            upd.setId(cs.getId());
            upd.setEffectiveDate(newStart);
            upd.setEndDate(newEnd);
            // 没有续费记录时才同步 actual_end_date；有续费记录的服务期已被续费扩展，contract 改了不应回退
            if (renewCount == 0) {
                upd.setActualEndDate(newEnd);
            }
            upd.setUpdateTime(new Date());
            companyServiceManager.updateById(upd);
            log.info("[合同时间联动] 合同 {} → company_service {} 已同步 effective={} end={} actualEnd={}（renewCount={}）",
                    contract.getContractNo(), cs.getId(), newStart, newEnd,
                    renewCount == 0 ? newEnd : "保持原值", renewCount);
        } catch (Exception e) {
            // 同步失败不回滚合同更新（合同主表已 commit），仅 log
            log.error("[合同时间联动] 同步 company_service 失败 contractId={}" + contract.getId(), e);
        }
    }

    /**
     * 删除合同（逻辑删除）
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteContract(Long id, LoginUser loginUser) {
        if (ObjectUtils.isEmpty(id)) {
            return Boolean.FALSE;
        }
        
        Contract contract = contractManager.getById(id);
        if (ObjectUtils.isEmpty(contract)) {
            return Boolean.TRUE;
        }
        
        // 回收地址
        List<ContractDetail> detailList = contractDetailManager.lambdaQuery()
                .eq(ContractDetail::getContractId, id)
                .list();
        
        if (CollUtil.isNotEmpty(detailList)) {
            List<Long> addressIds = detailList.stream()
                    .map(ContractDetail::getCompanyAddressId)
                    .filter(ObjectUtils::isNotEmpty)
                    .distinct()
                    .toList();
            
            if (CollUtil.isNotEmpty(addressIds)) {
                for (Long addressId : addressIds) {
                    RegisterAddress address = registerAddressManager.getById(addressId);
                    if (ObjectUtils.isNotEmpty(address)) {
                        address.setStatus(GlobalConstant.Number.ZERO_INT); // 未使用
                        address.setUpdateTime(new Date());
                        if (ObjectUtils.isNotEmpty(loginUser)) {
                            address.setUpdateBy(loginUser.getUsername());
                            address.setUpdateByName(loginUser.getRealName());
                        }
                        registerAddressManager.updateById(address);
                    }
                }
            }
        }
        
        // 删除合同明细
        contractDetailManager.lambdaUpdate()
                .set(ContractDetail::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(ContractDetail::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), ContractDetail::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), ContractDetail::getUpdateByName, loginUser.getRealName())
                .eq(ContractDetail::getContractId, id)
                .update();
        
        // 删除合同（逻辑删除：显式将 deleted 置为 1，避免 @TableLogic 导致 updateById 未更新该字段）
        contractManager.lambdaUpdate()
                .set(Contract::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(Contract::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), Contract::getUpdateBy, loginUser != null ? loginUser.getUsername() : null)
                .set(ObjectUtils.isNotEmpty(loginUser), Contract::getUpdateByName, loginUser != null ? loginUser.getRealName() : null)
                .eq(Contract::getId, id)
                .update();
        
        return Boolean.TRUE;
    }

    /**
     * 更新合同状态
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateContractStatus(Long id, String status, LoginUser loginUser) {
        if (ObjectUtils.isEmpty(id) || StringUtils.isEmpty(status)) {
            return Boolean.FALSE;
        }

        Contract contract = contractManager.getById(id);
        if (ObjectUtils.isEmpty(contract)) {
            return Boolean.FALSE;
        }

        // 只有草稿状态才能废弃
        if (DictConstants.CONTRACT_STATUS_DISUSED.equals(status) &&
                !(DictConstants.CONTRACT_STATUS_DRAFT.equals(contract.getStatus()) || DictConstants.CONTRACT_STATUS_TERMINATED.equals(contract.getStatus()))) {
            throw new BusinessException("只有草稿状态的合同才能执行废弃操作！");
        }

        contract.setStatus(status);
        contract.setUpdateTime(new Date());
        if (ObjectUtils.isNotEmpty(loginUser)) {
            contract.setUpdateBy(loginUser.getUsername());
            contract.setUpdateByName(loginUser.getRealName());
        }

        // 新增合同变为完成时，将关联的订单明细中 RENEW 改为 NEW
        if (DictConstants.CONTRACT_STATUS_COMPLETED.equals(status) && DictConstants.CONTRACT_TYPE_NEW.equals(contract.getContractType())) {
            List<ContractDetail> detailList = contractDetailManager.lambdaQuery()
                    .eq(ContractDetail::getContractId, id)
                    .list();
            if (CollUtil.isNotEmpty(detailList)) {
                List<Long> orderDetailIds = detailList.stream()
                        .map(ContractDetail::getOrderDetailId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();
                if (CollUtil.isNotEmpty(orderDetailIds)) {
                    orderService.updateOrderDetailTypeToNewIfRenew(orderDetailIds);
                }
            }
            // 新增合同完成时，添加公司服务
            addCompanyService(contract);
        }

        // 续费合同变为完成时：为合同对应公司服务自动添加一条续费记录（含校验，失败则回滚）
        if (DictConstants.CONTRACT_STATUS_COMPLETED.equals(status) && DictConstants.CONTRACT_TYPE_RENEW.equals(contract.getContractType())) {
            // 1. 根据合格业务类型(service_business)得到 service_id 列表，再按公司 id 查 company_service
            List<Long> serviceIds = serviceBusinessManager.lambdaQuery()
                    .eq(ServiceBusiness::getBusinessId, contract.getBusinessId())
                    .list().stream()
                    .map(ServiceBusiness::getServiceId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            if (CollUtil.isEmpty(serviceIds)) {
                throw new BusinessException("当前公司不存在公司服务，请联系管理员！");
            }
            //查询根据查询此公司税号所有公司
            CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                    .eq(CustomerCompany::getId, contract.getCompanyId())
                    .one();
            List<CustomerCompany> customerCompanyList = customerCompanyManager.lambdaQuery()
                    .eq(CustomerCompany::getTaxNumber, customerCompany.getTaxNumber())
                    .list();
            List<Long> companyIdList = customerCompanyList.stream().map(CustomerCompany::getId).collect(Collectors.toList());
            List<CompanyService> companyServices = companyServiceManager.lambdaQuery()
                    .in(CompanyService::getCompanyId, companyIdList)
                    .in(CompanyService::getServiceId, serviceIds)
                    .list();
            if (CollUtil.isEmpty(companyServices)) {
                throw new BusinessException("当前公司不存在公司服务，请联系管理员！");
            }
            CompanyService companyService = companyServices.get(0);
            LocalDate renewStart = contract.getContractStartTime() != null
                    ? DateUtils.dateToLocalDateTime(contract.getContractStartTime()).toLocalDate()
                    : null;
            LocalDate renewEnd = contract.getContractEndTime() != null
                    ? DateUtils.dateToLocalDateTime(contract.getContractEndTime()).toLocalDate()
                    : null;
            if (renewStart == null || renewEnd == null) {
                throw new BusinessException("合同开始或结束时间为空，无法自动生成续费记录");
            }
            // 2.3 续费校验：续费开始日期晚于实际结束日期；续费结束日期晚于续费开始日期（不符合则回滚并提示）
            LocalDate actualEnd = companyService.getActualEndDate();
            if (actualEnd != null && !renewStart.isAfter(actualEnd)) {
                throw new BusinessException("续费开始日期须晚于实际结束日期");
            }
            if (!renewEnd.isAfter(renewStart)) {
                throw new BusinessException("续费结束日期须晚于续费开始日期");
            }
            AddCompanyRenewReq req = new AddCompanyRenewReq();
            req.setCompanyServiceId(companyService.getId());
            req.setRenewEffectiveDate(renewStart);
            req.setRenewEndDate(renewEnd);
            req.setRenewType(0);
            req.setRenewAmount(contract.getContractPrice() != null ? contract.getContractPrice() : BigDecimal.ZERO);
            req.setRemark("由" + contract.getContractNo() + "自动生成续费记录");
            companyServiceService.addCompanyRenew(req, loginUser);

            // S3-4 6.3.8 续费合同完成 → 自动关闭对应的"待办续费任务"
            // 同时按 tax_number 广播：家族（同税号）下所有 CC 关联的同 business 的 company_service 一并清空
            try {
                java.util.Set<Long> closeServiceIds = new java.util.HashSet<>();
                companyServices.forEach(cs -> closeServiceIds.add(cs.getId()));
                // 收集这些 company_service 关联的 (company_id, business_id) 对，按家族扩展
                List<Long> originCompanyIds = companyServices.stream()
                        .map(CompanyService::getCompanyId)
                        .filter(java.util.Objects::nonNull).distinct().toList();
                Long businessIdForRenew = companyServices.stream()
                        .map(CompanyService::getBusinessId)
                        .filter(java.util.Objects::nonNull).findFirst().orElse(null);
                if (CollUtil.isNotEmpty(originCompanyIds) && businessIdForRenew != null) {
                    java.util.Set<Long> familyCompanyIds = new java.util.HashSet<>(originCompanyIds);
                    for (Long cid : originCompanyIds) {
                        List<Long> sameTax = customerCompanyManager.getCompanyIdsBySameTaxNumber(cid, null);
                        if (CollUtil.isNotEmpty(sameTax)) familyCompanyIds.addAll(sameTax);
                    }
                    List<Long> familyServiceIds = companyServiceManager.lambdaQuery()
                            .select(CompanyService::getId)
                            .in(CompanyService::getCompanyId, familyCompanyIds)
                            .eq(CompanyService::getBusinessId, businessIdForRenew)
                            .list().stream().map(CompanyService::getId).toList();
                    if (CollUtil.isNotEmpty(familyServiceIds)) {
                        closeServiceIds.addAll(familyServiceIds);
                    }
                }
                companyServiceManager.lambdaUpdate()
                        .set(CompanyService::getPendingRenewalAssignee, null)
                        .set(CompanyService::getPendingRenewalAssigneeName, null)
                        .set(CompanyService::getPendingRenewalAssignedAt, null)
                        .set(CompanyService::getPendingRenewalNote, null)
                        .in(CompanyService::getId, closeServiceIds)
                        .update();
            } catch (Exception ignored) {
                // 关闭失败不影响主流程
            }
        }

        // 如果状态为已终止或已废弃，需要释放地址资源（订单明细不做逻辑删除，仅通过状态控制是否可再次使用）
        if (DictConstants.CONTRACT_STATUS_TERMINATED.equals(status) || DictConstants.CONTRACT_STATUS_DISUSED.equals(status)) {
            List<ContractDetail> detailList = contractDetailManager.lambdaQuery()
                    .eq(ContractDetail::getContractId, id)
                    .list();

            if (CollUtil.isNotEmpty(detailList)) {
                List<Long> addressIds = detailList.stream()
                        .map(ContractDetail::getCompanyAddressId)
                        .filter(ObjectUtils::isNotEmpty)
                        .distinct()
                        .toList();

                if (CollUtil.isNotEmpty(addressIds)) {
                    for (Long addressId : addressIds) {
                        RegisterAddress address = registerAddressManager.getById(addressId);
                        if (ObjectUtils.isNotEmpty(address)) {
                            address.setStatus(GlobalConstant.Number.ZERO_INT); // 未使用
                            address.setUpdateTime(new Date());
                            if (ObjectUtils.isNotEmpty(loginUser)) {
                                address.setUpdateBy(loginUser.getUsername());
                                address.setUpdateByName(loginUser.getRealName());
                            }
                            registerAddressManager.updateById(address);
                        }
                    }
                }
            }
        }

        // 如果状态为已废弃，存在公司服务需要进行删除，任务单进行关闭
        if(DictConstants.CONTRACT_STATUS_DISUSED.equals(status)){
            CompanyService companyService = companyServiceManager.lambdaQuery()
                    .eq(CompanyService::getSourceNumber, contract.getContractNo())
                    .one();
            if(ObjectUtils.isNotEmpty(companyService)){
                companyServiceManager.lambdaUpdate()
                        .eq(CompanyService::getSourceNumber, contract.getContractNo())
                        .set(CompanyService::getRemark, companyService.getRemark() + " 合同" + contract.getContractNo() + "废弃，停止服务！")
                        .set(CompanyService::getDeleted, GlobalConstant.Number.ONE_INT)
                        .update();
            }

            CommonTask commonTask = commonTaskManager.lambdaQuery()
                    .eq(CommonTask::getSourceNumber, contract.getContractNo())
                    .one();

            if(ObjectUtils.isNotEmpty(commonTask)){
                commonTaskManager.lambdaUpdate()
                        .eq(CommonTask::getSourceNumber, contract.getContractNo())
                        .set(CommonTask::getRemark, commonTask.getRemark() + " 合同" + contract.getContractNo() + "废弃，取消任务单！")
                        .set(CommonTask::getPreStatus, commonTask.getStatus())
                        .set(CommonTask::getStatus, DictConstants.COMMON_TASK_STATUS_CANCELLED)
                        .update();
            }
        }

        return contractManager.updateById(contract);
    }

    /**
     * 新增合同完成时，添加公司服务
     * 来源仅为合同：source_number/source_id 为合同编码/合同ID
     */
    private void addCompanyService(Contract contract) {
        if (ObjectUtils.isEmpty(contract.getCompanyId())) {
            return;
        }
        CompanyService companyService = new CompanyService();
        companyService.setCompanyId(contract.getCompanyId());
        companyService.setSourceNumber(contract.getContractNo());
        companyService.setSourceId(contract.getId());
        // 创建人取合同创建人（数据权限过滤依赖此字段）
        companyService.setCreateBy(contract.getCreateBy());
        companyService.setCreateByName(contract.getCreateByName());
        companyService.setCreateTime(new Date());
        // 业务类型ID
        if (ObjectUtils.isNotEmpty(contract.getBusinessId())) {
            companyService.setBusinessId(contract.getBusinessId());
        }
        // 通过 business_id 查找对应的 service_id
        if (ObjectUtils.isEmpty(contract.getBusinessId())) {
            throw new BusinessException("合同业务类型为空，无法创建公司服务");
        }
        List<ServiceBusiness> serviceBizList = serviceBusinessManager.lambdaQuery()
                .eq(ServiceBusiness::getBusinessId, contract.getBusinessId())
                .list();
        if (CollUtil.isEmpty(serviceBizList)) {
            throw new BusinessException("该业务类型未配置对应服务，请联系管理员");
        }
        companyService.setServiceId(serviceBizList.get(0).getServiceId());
        // 销售负责人取合同创建人
        companyService.setSaleOwnerUser(contract.getCreateBy());
        companyService.setSaleOwnerUserName(contract.getCreateByName());
        if (ObjectUtils.isNotEmpty(contract.getContractStartTime())) {
            companyService.setEffectiveDate(DateUtils.dateToLocalDateTime(contract.getContractStartTime()).toLocalDate());
        }
        if (ObjectUtils.isNotEmpty(contract.getContractEndTime())) {
            LocalDate endDate = DateUtils.dateToLocalDateTime(contract.getContractEndTime()).toLocalDate();
            companyService.setEndDate(endDate);
            companyService.setActualEndDate(endDate);
        }
        // 查找客户ID
        CustomerCompany company = customerCompanyManager.getById(contract.getCompanyId());
        if (ObjectUtils.isNotEmpty(company)) {
            companyService.setCustomerId(company.getCustomerId());
        }
        // 管家负责人：如已有公司服务则沿用，否则取合同创建人
        List<CompanyService> companyServiceList = companyServiceManager.lambdaQuery()
                .eq(CompanyService::getCompanyId, companyService.getCompanyId())
                .list();
        if (CollUtil.isNotEmpty(companyServiceList)) {
            CompanyService existing = companyServiceList.get(GlobalConstant.Number.ZERO_INT);
            companyService.setButlerOwnerUser(existing.getButlerOwnerUser());
            companyService.setButlerOwnerUserName(existing.getButlerOwnerUserName());
        } else {
            companyService.setButlerOwnerUser(contract.getCreateBy());
            companyService.setButlerOwnerUserName(contract.getCreateByName());
        }
        companyServiceManager.save(companyService);
    }

    /**
     * 生成合同编号
     */
    private String generateContractNo() {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 生成6位随机数
        int random = (int) (Math.random() * 1000000);
        return String.format("HT%s%06d", dateStr, random);
    }

    /**
     * 判断同公司、同业务下是否存在非废弃合同（供订单模块判断明细类型 NEW/RENEW）
     *
     * @param tenantId  租户ID
     * @param companyId 公司ID
     * @param businessId 业务ID
     * @return 存在返回 true
     */
    public boolean existsNonDisusedContractByCompanyAndBusiness(Long tenantId, Long companyId, Long businessId) {
        if (tenantId == null || companyId == null || businessId == null) {
            return false;
        }
        List<Long> companyIds = customerCompanyManager.getCompanyIdsBySameTaxNumber(companyId, tenantId);
        if (CollUtil.isEmpty(companyIds)) {
            companyIds = Collections.singletonList(companyId);
        }
        return contractManager.lambdaQuery()
                .eq(Contract::getTenantId, tenantId)
                .in(Contract::getCompanyId, companyIds)
                .eq(Contract::getBusinessId, businessId)
                .ne(Contract::getStatus, DictConstants.CONTRACT_STATUS_DISUSED)
                .exists();
    }

    /**
     * 订单→合同 一键生成：根据订单预装合同表单初值
     *
     * 返回结构：
     *   {
     *     orderId, orderNo, customerId, customerName,
     *     groups: [  // 每个 group 对应一张可建合同（按 companyId+businessId 分组）
     *       {
     *         companyId, companyName, taxNumber,
     *         businessId, businessName,
     *         contractType (contract_new | contract_renew),
     *         contractStartTime, contractEndTime,
     *         contractPrice,
     *         detailList: [ {orderDetailId, productId, productName, unitPrice, quantity, amount} ]
     *       }
     *     ]
     *   }
     */
    public Map<String, Object> prepareQuickContractFromOrder(Long orderId, Long tenantId) {
        Order order = orderManager.getById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!"PAID".equals(order.getStatus())) {
            throw new BusinessException("仅已付款订单可一键生成合同，当前订单状态：" + order.getStatus());
        }

        List<OrderDetail> details = orderDetailManager.lambdaQuery()
                .eq(OrderDetail::getOrderId, orderId)
                .eq(OrderDetail::getTenantId, tenantId)
                .list();
        if (CollUtil.isEmpty(details)) {
            throw new BusinessException("订单无可用明细");
        }

        // 按 (companyId, businessId) 分组
        Map<String, List<OrderDetail>> grouped = new LinkedHashMap<>();
        for (OrderDetail d : details) {
            if (d.getCompanyId() == null || d.getBusinessId() == null) continue;
            String key = d.getCompanyId() + "_" + d.getBusinessId();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }

        List<Map<String, Object>> groups = new ArrayList<>();
        for (Map.Entry<String, List<OrderDetail>> entry : grouped.entrySet()) {
            List<OrderDetail> groupDetails = entry.getValue();
            OrderDetail first = groupDetails.get(0);

            CustomerCompany company = customerCompanyManager.getById(first.getCompanyId());
            SysBusiness business = sysBusinessManager.getById(first.getBusinessId());

            // 自动合同类型
            String contractType = getAutoContractType(first.getCompanyId(), first.getBusinessId(), tenantId);

            // 合同周期：取各明细 effectiveDate/endDate 的最小最大
            LocalDate startTime = null, endTime = null;
            BigDecimal totalPrice = BigDecimal.ZERO;
            for (OrderDetail d : groupDetails) {
                if (d.getEffectiveDate() != null && (startTime == null || d.getEffectiveDate().isBefore(startTime))) {
                    startTime = d.getEffectiveDate();
                }
                if (d.getEndDate() != null && (endTime == null || d.getEndDate().isAfter(endTime))) {
                    endTime = d.getEndDate();
                }
                if (d.getAmount() != null) {
                    totalPrice = totalPrice.add(d.getAmount());
                }
            }

            Map<String, Object> group = new LinkedHashMap<>();
            group.put("companyId", first.getCompanyId());
            group.put("companyName", company != null ? company.getCompanyName() : null);
            group.put("taxNumber", company != null ? company.getTaxNumber() : null);
            group.put("businessId", first.getBusinessId());
            group.put("businessName", business != null ? business.getBusinessName() : null);
            group.put("contractType", contractType);
            group.put("contractStartTime", startTime);
            group.put("contractEndTime", endTime);
            group.put("contractPrice", totalPrice);

            List<Map<String, Object>> detailList = new ArrayList<>();
            for (OrderDetail d : groupDetails) {
                Map<String, Object> di = new LinkedHashMap<>();
                di.put("orderDetailId", d.getId());
                di.put("productId", d.getProductId());
                di.put("productName", d.getProductName());
                di.put("unitPrice", d.getUnitPrice());
                di.put("quantity", d.getQuantity());
                di.put("amount", d.getAmount());
                detailList.add(di);
            }
            group.put("detailList", detailList);

            groups.add(group);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", order.getId());
        result.put("orderNo", order.getOrderNo());
        result.put("customerId", order.getCustomerId());
        result.put("customerName", order.getCustomerName());
        result.put("groups", groups);
        return result;
    }

    /**
     * 续费 一键生成：根据 CompanyService 预装续费合同初值
     * 流程：companyService → 找 businessId → 取上次已完成合同 → 复制明细 + 周期顺延
     */
    public Map<String, Object> prepareQuickRenewContract(Long companyServiceId, Long tenantId) {
        CompanyService cs = companyServiceManager.getById(companyServiceId);
        if (cs == null) {
            throw new BusinessException("服务记录不存在");
        }
        if (cs.getCompanyId() == null) {
            throw new BusinessException("服务记录缺少公司信息");
        }

        // 找 businessId：通过 CompanyService.serviceId → ServiceBusiness 反查
        Long businessId = cs.getBusinessId();
        if (businessId == null && cs.getServiceId() != null) {
            ServiceBusiness sb = serviceBusinessManager.lambdaQuery()
                    .eq(ServiceBusiness::getServiceId, cs.getServiceId())
                    .last("LIMIT 1")
                    .one();
            if (sb != null) businessId = sb.getBusinessId();
        }
        if (businessId == null) {
            throw new BusinessException("无法定位该服务对应的业务（business_id）");
        }

        CustomerCompany company = customerCompanyManager.getById(cs.getCompanyId());
        SysBusiness business = sysBusinessManager.getById(businessId);

        // 找上次"已完成"合同
        Contract lastContract = contractManager.lambdaQuery()
                .eq(Contract::getCompanyId, cs.getCompanyId())
                .eq(Contract::getBusinessId, businessId)
                .eq(Contract::getStatus, DictConstants.CONTRACT_STATUS_COMPLETED)
                .orderByDesc(Contract::getContractEndTime)
                .last("LIMIT 1")
                .one();

        // 周期顺延：start = 旧 actualEndDate + 1 day，end = start + (旧周期 or 1 year)
        LocalDate startTime;
        LocalDate endTime;
        BigDecimal contractPrice = BigDecimal.ZERO;
        if (cs.getActualEndDate() != null) {
            startTime = cs.getActualEndDate().plusDays(1);
            endTime = startTime.plusYears(1).minusDays(1);
        } else {
            startTime = LocalDate.now();
            endTime = startTime.plusYears(1).minusDays(1);
        }
        if (lastContract != null && lastContract.getContractPrice() != null) {
            contractPrice = lastContract.getContractPrice();
        }

        // 上次合同明细（用于"参考"展示）
        List<Map<String, Object>> detailList = new ArrayList<>();
        if (lastContract != null) {
            List<ContractDetail> lastDetails = contractDetailManager.lambdaQuery()
                    .eq(ContractDetail::getContractId, lastContract.getId())
                    .list();
            for (ContractDetail d : lastDetails) {
                Product product = d.getProductId() != null ? productManager.getById(d.getProductId()) : null;
                Map<String, Object> di = new LinkedHashMap<>();
                di.put("orderDetailId", null); // 续费时无新订单明细，留空
                di.put("productId", d.getProductId());
                di.put("productName", product != null ? product.getProductName() : null);
                di.put("companyAddressId", d.getCompanyAddressId());
                detailList.add(di);
            }
        }

        Map<String, Object> group = new LinkedHashMap<>();
        group.put("companyId", cs.getCompanyId());
        group.put("companyName", company != null ? company.getCompanyName() : null);
        group.put("taxNumber", company != null ? company.getTaxNumber() : null);
        group.put("businessId", businessId);
        group.put("businessName", business != null ? business.getBusinessName() : null);
        group.put("contractType", DictConstants.CONTRACT_TYPE_RENEW);
        group.put("contractStartTime", startTime);
        group.put("contractEndTime", endTime);
        group.put("contractPrice", contractPrice);
        group.put("detailList", detailList);
        group.put("isRenewal", true);
        group.put("lastContractNo", lastContract != null ? lastContract.getContractNo() : null);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("companyServiceId", companyServiceId);
        result.put("customerId", cs.getCustomerId());
        result.put("groups", Collections.singletonList(group));
        return result;
    }

    /**
     * 根据公司和业务自动计算合同类型，与订单明细类型 {@code OrderServiceImpl#resolveOrderDetailType} 保持一致：
     *
     * 1) 业务为补款（business_code=BALANCE）：合同不会单独建，但兜底返回 NEW；
     * 2) 取业务上配置的"判断转续费的订单数阈值"checkRenewalCount（{@link SysBusiness#getCheckRenewalCount()}）；
     * 3) 按"同税号公司列表 + 业务ID"统计【正常订单（非补款，已支付）下的非补款明细】数量 count：
     *    - count >  0 且 count >= checkRenewalCount  → contract_renew（续费合同）
     *    - count == 0  → 兜底看是否存在非作废历史合同（兼容老客户没订单的情况） → 存在为 RENEW，否则 NEW
     *    - 0 < count < checkRenewalCount → contract_new（新增合同）
     */
    public String getAutoContractType(Long companyId, Long businessId, Long tenantId) {
        if (companyId == null || businessId == null) {
            throw new BusinessException("计算合同类型时公司ID和业务ID不能为空！");
        }
        SysBusiness business = sysBusinessManager.getById(businessId);
        if (business == null) {
            return DictConstants.CONTRACT_TYPE_NEW;
        }
        // 补款业务不会独立建合同，兜底 NEW
        if (DictConstants.BUSINESS_CODE_BALANCE.equals(business.getBusinessCode())) {
            return DictConstants.CONTRACT_TYPE_NEW;
        }
        int checkRenewalCount = business.getCheckRenewalCount() != null
                ? business.getCheckRenewalCount() : GlobalConstant.Number.ZERO_INT;
        List<Long> companyIds = customerCompanyManager.getCompanyIdsBySameTaxNumber(companyId, tenantId);
        if (CollUtil.isEmpty(companyIds)) {
            companyIds = Collections.singletonList(companyId);
        }
        // 取补款业务ID（用于在统计时排除补款明细）
        Long balanceBusinessId = null;
        SysBusiness balanceBiz = sysBusinessManager.lambdaQuery()
                .eq(SysBusiness::getBusinessCode, DictConstants.BUSINESS_CODE_BALANCE)
                .last("LIMIT 1")
                .one();
        if (balanceBiz != null) {
            balanceBusinessId = balanceBiz.getId();
        }
        // 规则 1：该公司 + 业务下没有任何"非废弃合同" → NEW（首份合同，必须先走新合同沉淀 company_service）
        boolean existsContract = existsNonDisusedContractByCompanyAndBusiness(tenantId, companyId, businessId);
        if (!existsContract) {
            return DictConstants.CONTRACT_TYPE_NEW;
        }
        // 规则 2：存在历史合同时按业务的续费阈值判定
        //   - 工商业务 checkRenewalCount=999 → 实际 count 永远 < 999 → 永远 NEW（每张工商独立）
        //   - 代账报税 checkRenewalCount=1   → 第二张起判 RENEW（代账只允许一份正式服务，后续都是续费）
        long count = orderDetailMapper.countNormalOrderDetailsByCompanyAndBusiness(
                tenantId, companyIds, businessId, balanceBusinessId);
        if (count >= checkRenewalCount && checkRenewalCount > 0) {
            // 续费判定的硬约束：必须已沉淀 company_service，否则续费完成时会校验失败，先走 NEW
            boolean hasCompanyService = existsCompanyServiceByCompanyAndBusiness(tenantId, companyIds, businessId);
            return hasCompanyService ? DictConstants.CONTRACT_TYPE_RENEW : DictConstants.CONTRACT_TYPE_NEW;
        }
        return DictConstants.CONTRACT_TYPE_NEW;
    }

    /**
     * 该公司（含同税号其它公司）+ 该业务下，是否已经沉淀过 company_service 记录。
     * company_service 只在合同完成时由 addCompanyService 写入，因此此方法等价于"已有正式合同已完成"。
     */
    private boolean existsCompanyServiceByCompanyAndBusiness(Long tenantId, List<Long> companyIds, Long businessId) {
        if (CollUtil.isEmpty(companyIds) || businessId == null) return false;
        List<Long> serviceIds = serviceBusinessManager.lambdaQuery()
                .eq(ServiceBusiness::getBusinessId, businessId)
                .list().stream()
                .map(ServiceBusiness::getServiceId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(serviceIds)) return false;
        return companyServiceManager.lambdaQuery()
                .in(CompanyService::getCompanyId, companyIds)
                .in(CompanyService::getServiceId, serviceIds)
                .exists();
    }

    /**
     * 获取指定公司+业务下可用于合同关联的订单明细。
     * contractId 不为空时（编辑场景），仅排除被其他合同占用的明细，当前合同已占用的仍返回。
     */
    public List<OrderDetailForContractResp> getOrderDetailsForContract(Long companyId, Long businessId, Long tenantId, Long contractId) {
        if (companyId == null || businessId == null) {
            throw new BusinessException("查询订单明细时公司ID和业务ID不能为空！");
        }
        // 查询业务关联的产品
        List<GetBusinessProductResp> businessProducts = sysBusinessProductMapper.getProductListByBusinessId(businessId);
        if (CollUtil.isEmpty(businessProducts)) {
            return Collections.emptyList();
        }
        List<Long> productIdList = businessProducts.stream()
                .map(GetBusinessProductResp::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isEmpty(productIdList)) {
            return Collections.emptyList();
        }
        // 查询该公司下、这些产品的订单明细（排除补款类型 BALANCE）
        List<OrderDetail> orderDetailList = orderDetailManager.lambdaQuery()
                .eq(OrderDetail::getCompanyId, companyId)
                .eq(OrderDetail::getTenantId, tenantId)
                .in(OrderDetail::getProductId, productIdList)
                .and(w -> w.ne(OrderDetail::getOrderDetailType, DictConstants.ORDER_DETAIL_TYPE_BALANCE).or().isNull(OrderDetail::getOrderDetailType))
                .list();
        if (CollUtil.isEmpty(orderDetailList)) {
            return Collections.emptyList();
        }
        // 补款业务不参与合同订单查询：仅保留正常订单（非补款订单）的明细，且排除补款业务的明细
        Long balanceBusinessId = null;
        SysBusiness balanceBiz = sysBusinessManager.lambdaQuery()
                .eq(SysBusiness::getBusinessCode, DictConstants.BUSINESS_CODE_BALANCE)
                .last("LIMIT 1")
                .one();
        if (balanceBiz != null) {
            balanceBusinessId = balanceBiz.getId();
        }
        // 编辑模式：先查出当前合同已绑定的明细 id 集合；这些明细不论订单状态/产品映射如何都保留以便回显
        Set<Long> currentContractDetailIds = Collections.emptySet();
        if (ObjectUtils.isNotEmpty(contractId)) {
            currentContractDetailIds = contractDetailManager.lambdaQuery()
                    .eq(ContractDetail::getContractId, contractId)
                    .eq(ContractDetail::getTenantId, tenantId)
                    .list().stream()
                    .map(ContractDetail::getOrderDetailId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
        }

        Set<Long> orderIds = orderDetailList.stream().map(OrderDetail::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (!orderIds.isEmpty()) {
            List<Order> orders = orderManager.listByIds(orderIds);
            Set<Long> normalOrderIds = orders.stream()
                    .filter(o -> DictConstants.ORDER_TYPE_NORMAL.equals(o.getOrderType())
                            && DictConstants.ORDER_STATUS_PAID.equals(o.getStatus()))
                    .map(Order::getId)
                    .collect(Collectors.toSet());
            final Long finalBalanceBusinessId = balanceBusinessId;
            final Set<Long> finalCurrentIds = currentContractDetailIds;
            orderDetailList = orderDetailList.stream()
                    .filter(d -> {
                        // 当前合同已绑定的明细：跳过订单状态过滤，强制保留以便回显
                        if (finalCurrentIds.contains(d.getId())) return true;
                        return normalOrderIds.contains(d.getOrderId())
                                && (finalBalanceBusinessId == null || !finalBalanceBusinessId.equals(d.getBusinessId()));
                    })
                    .toList();
        }

        // 编辑模式补救：上面 lambdaQuery 用了 productIdList 过滤，当前合同的明细如果 product_id 已不在该业务映射里
        // 会被直接过滤光。这里把"现有结果集里缺失的当前合同明细"直接 selectByIds 拉回来，绕过所有上游过滤。
        if (!currentContractDetailIds.isEmpty()) {
            Set<Long> alreadyIn = orderDetailList.stream().map(OrderDetail::getId).collect(Collectors.toSet());
            List<Long> missingIds = currentContractDetailIds.stream()
                    .filter(id -> !alreadyIn.contains(id))
                    .toList();
            if (!missingIds.isEmpty()) {
                List<OrderDetail> missingDetails = orderDetailManager.listByIds(missingIds);
                if (CollUtil.isNotEmpty(missingDetails)) {
                    List<OrderDetail> merged = new java.util.ArrayList<>(orderDetailList);
                    merged.addAll(missingDetails);
                    orderDetailList = merged;
                }
            }
        }

        if (CollUtil.isEmpty(orderDetailList)) {
            return Collections.emptyList();
        }

        // 过滤掉已经被任何合同（任意状态）占用的订单明细
        List<Long> orderDetailIdList = orderDetailList.stream()
                .map(OrderDetail::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (CollUtil.isNotEmpty(orderDetailIdList)) {
            // 查询这些明细中，哪些已经出现在合同明细表中（逻辑未删除）
            List<ContractDetail> usedDetails = contractDetailManager.lambdaQuery()
                    .in(ContractDetail::getOrderDetailId, orderDetailIdList)
                    .eq(ContractDetail::getTenantId, tenantId)
                    .list();
            if (CollUtil.isNotEmpty(usedDetails)) {
                // 先取出涉及到的合同ID
                List<Long> contractIds = usedDetails.stream()
                        .map(ContractDetail::getContractId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList();

                Map<Long, Contract> contractMap = new HashMap<>();
                if (CollUtil.isNotEmpty(contractIds)) {
                    contractMap = contractManager.listByIds(contractIds).stream()
                            .collect(Collectors.toMap(Contract::getId, c -> c));
                }

                // 仅保留“未废弃”的合同对应的明细作为占用；编辑时当前合同占用的不算占用
                Map<Long, Contract> finalContractMap = contractMap;
                final Long excludeContractId = contractId;
                Set<Long> usedOrderDetailIds = usedDetails.stream()
                        .filter(d -> {
                            if (ObjectUtils.isNotEmpty(excludeContractId) && excludeContractId.equals(d.getContractId())) {
                                return false; // 当前合同占用的明细不视为占用，便于编辑回显
                            }
                            Contract c = finalContractMap.get(d.getContractId());
                            return c != null && !DictConstants.CONTRACT_STATUS_DISUSED.equals(c.getStatus());
                        })
                        .map(ContractDetail::getOrderDetailId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

                if (CollUtil.isNotEmpty(usedOrderDetailIds)) {
                    orderDetailList = orderDetailList.stream()
                            .filter(d -> !usedOrderDetailIds.contains(d.getId()))
                            .toList();
                }
            }
        }
        if (CollUtil.isEmpty(orderDetailList)) {
            return Collections.emptyList();
        }
        // 查询订单编号
        List<Long> orderIdList = orderDetailList.stream()
                .map(OrderDetail::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Order> orderMap = Collections.emptyMap();
        if (CollUtil.isNotEmpty(orderIdList)) {
            orderMap = orderManager.listByIds(orderIdList).stream()
                    .collect(Collectors.toMap(Order::getId, o -> o));
        }
        // 查询公司名称
        String companyName = null;
        CustomerCompany company = customerCompanyManager.getById(companyId);
        if (ObjectUtils.isNotEmpty(company)) {
            companyName = company.getCompanyName();
        }
        String finalCompanyName = companyName;
        Map<Long, GetBusinessProductResp> productInfoMap = businessProducts.stream()
                .collect(Collectors.toMap(GetBusinessProductResp::getProductId, p -> p, (a, b) -> a));
        Map<Long, Order> finalOrderMap = orderMap;

        return orderDetailList.stream().map(detail -> {
            OrderDetailForContractResp resp = new OrderDetailForContractResp();
            resp.setOrderDetailId(detail.getId());
            resp.setProductId(detail.getProductId());
            resp.setProductName(detail.getProductName());
            if (detail.getProductId() != null && productInfoMap.containsKey(detail.getProductId())) {
                resp.setProductNumber(productInfoMap.get(detail.getProductId()).getProductNumber());
            }
            resp.setCompanyId(detail.getCompanyId());
            resp.setCompanyName(finalCompanyName);
            resp.setEffectiveDate(detail.getEffectiveDate());
            resp.setEndDate(detail.getEndDate());
            if (detail.getOrderId() != null && finalOrderMap.containsKey(detail.getOrderId())) {
                resp.setOrderNo(finalOrderMap.get(detail.getOrderId()).getOrderNo());
            }
            resp.setUnitPrice(detail.getUnitPrice());
            resp.setQuantity(detail.getQuantity());
            return resp;
        }).toList();
    }

    /**
     * 生成合同文件第一步：根据合同 + 公司 + 当前日期预填占位符值，前端用于预览/编辑。
     * 仅做合同状态校验，不写文件。
     * 占位符与最新模板对齐（contract_file_template.docx）：
     *   partyA / partyAIdCard / partyAContract / partyAPhone
     *   partyBContract / partyBPhone（乙方联系信息暂不考虑，留空让用户填）
     *   partyBBank / partyBBankAccount（乙方开户行/银行账号，从字典 PAYMENT_TYPE_BANK 取首条按 "-" 拆分）
     *   companyType（公司类型，dict label）
     *   periodA（记账报税服务有效期，默认与合同周期一致 yyyy-MM）
     *   periodB（开票服务有效期，默认 "-"）
     *   amountL / amountC
     *   supplement（默认 "-"）
     *   signingY / signingM / signingD
     */
    public Map<String, String> prepareContractFileValues(Long contractId, String supplement, Long tenantId) {
        Contract c = validateContractForGeneration(contractId, tenantId);

        CustomerCompany company = customerCompanyManager.getById(c.getCompanyId());
        Map<String, String> values = new LinkedHashMap<>();
        values.put("partyA", nullToEmpty(company == null ? null : company.getCompanyName()));
        values.put("partyAContract", nullToEmpty(company == null ? null : company.getLegalName()));
        values.put("partyAPhone", nullToEmpty(company == null ? null : company.getLegalPhone()));
        // 甲方证件号 → 法人身份证号
        values.put("partyAIdCard", nullToEmpty(company == null ? null : company.getLegalIdCard()));
        // 乙方联系人/电话暂不考虑，留空让用户在预览时手填
        values.put("partyBContract", "");
        values.put("partyBPhone", "");

        // 乙方开户银行 + 银行账号：从字典 PAYMENT_TYPE_BANK 取首条，value 按"-"拆为 (银行名, 账号)
        String[] bankInfo = lookupPartyBBankInfo("PAYMENT_TYPE_BANK");
        values.put("partyBBank", bankInfo[0]);
        values.put("partyBBankAccount", bankInfo[1]);

        // 公司类型 → 字典标签（dict_type=COMPANY_TYPE）
        values.put("companyType", lookupDictLabel("COMPANY_TYPE",
                company == null ? null : company.getCompanyType()));

        // periodA 默认按合同周期；periodB 默认 "-"，需要时由用户在预览页改
        values.put("periodA", formatPeriod(toLocalDate(c.getContractStartTime()), toLocalDate(c.getContractEndTime())));
        values.put("periodB", "-");

        LocalDate today = LocalDate.now();
        values.put("signingY", String.valueOf(today.getYear()));
        values.put("signingM", String.valueOf(today.getMonthValue()));
        values.put("signingD", String.valueOf(today.getDayOfMonth()));

        BigDecimal price = c.getContractPrice() == null ? BigDecimal.ZERO : c.getContractPrice();
        values.put("amountL", price.stripTrailingZeros().toPlainString());
        values.put("amountC", ChineseAmountUtil.toChinese(price));
        // supplement 未提供时默认 "-"
        values.put("supplement", supplement == null || supplement.isEmpty() ? "-" : supplement);
        return values;
    }

    /**
     * 取字典 PAYMENT_TYPE_BANK 的首条 value，按第一个 "-" 拆分为 (银行名, 账号)。
     * 字典示例：value="山东章丘农村商业银行股份有限公司城区支行-2040001054205000010613"
     */
    private String[] lookupPartyBBankInfo(String typeCode) {
        String[] result = {"", ""};
        try {
            List<SysDictData> dicts = sysDictDataMapper.getDictDataListByTypeCode(typeCode);
            if (dicts != null && !dicts.isEmpty()) {
                String raw = dicts.get(0).getValue();
                if (raw != null && !raw.isEmpty()) {
                    int sep = raw.indexOf('-');
                    if (sep > 0) {
                        result[0] = raw.substring(0, sep);
                        result[1] = raw.substring(sep + 1);
                    } else {
                        result[0] = raw;
                    }
                }
            }
        } catch (Exception ignored) {
            // 字典缺失时返回空字符串，预览页用户可手填
        }
        return result;
    }

    private String lookupDictLabel(String typeCode, String value) {
        if (value == null || value.isEmpty()) return "";
        try {
            List<SysDictData> dicts = sysDictDataMapper.getDictDataListByTypeCode(typeCode);
            if (dicts != null) {
                for (SysDictData d : dicts) {
                    if (value.equals(d.getValue())) return nullToEmpty(d.getLabel());
                }
            }
        } catch (Exception ignored) {
            // 容错：dict 服务异常时返回原始值，预览页用户可手改
        }
        return value;
    }

    private static String formatPeriod(LocalDate start, LocalDate end) {
        if (start == null && end == null) return "";
        // 只精确到年月，按合同模板需求
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        String s = start == null ? "" : start.format(fmt);
        String e = end == null ? "" : end.format(fmt);
        return s + " 至 " + e;
    }

    /**
     * 预览渲染：用 values 在内存里把占位符替换为带哨兵的标记串，转 HTML 时输出为 contenteditable 红框 span，
     * 前端用户可直接在文档里编辑。不落盘、不写库。
     */
    public String renderContractFilePreviewHtml(Long contractId, Map<String, String> values, Long tenantId) {
        validateContractForGeneration(contractId, tenantId);
        Map<String, String> safeValues = values == null ? new HashMap<>() : new HashMap<>(values);
        safeValues.replaceAll((k, v) -> v == null ? "" : v);
        File template = new File(FileConstants.FILE_URL_CONTRACT, FileConstants.CONTRACT_TEMPLATE_FILENAME);
        if (!template.exists()) {
            throw new BusinessException("合同模板文件缺失：" + template.getAbsolutePath());
        }
        try (java.io.FileInputStream in = new java.io.FileInputStream(template);
             XWPFDocument doc = new XWPFDocument(in)) {
            DocxTemplateUtil.applyValuesWithMarkers(doc, safeValues);
            return XwpfHtmlUtil.toHtml(doc);
        } catch (IOException e) {
            throw new BusinessException("渲染预览失败：" + e.getMessage());
        }
    }

    /**
     * 生成合同文件第二步：用前端预览修改后的 values 渲染模板并落盘 docx，回写 contract.contract_file_url。
     */
    @Transactional(rollbackFor = Exception.class)
    public String generateContractFileWithValues(Long contractId, Map<String, String> values, LoginUser loginUser, Long tenantId) {
        Contract c = validateContractForGeneration(contractId, tenantId);

        Map<String, String> safeValues = values == null ? new HashMap<>() : new HashMap<>(values);
        safeValues.replaceAll((k, v) -> v == null ? "" : v);

        File dir = new File(FileConstants.FILE_URL_CONTRACT);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new BusinessException("合同文件目录创建失败：" + dir.getAbsolutePath());
        }
        File template = new File(dir, FileConstants.CONTRACT_TEMPLATE_FILENAME);
        if (!template.exists()) {
            throw new BusinessException("合同模板文件缺失：" + template.getAbsolutePath());
        }
        String fileName = c.getContractNo() + "_"
                + DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now()) + ".docx";
        File output = new File(dir, fileName);
        try {
            DocxTemplateUtil.render(template.getAbsolutePath(), safeValues, output.getAbsolutePath());
        } catch (IOException e) {
            throw new BusinessException("生成合同文件失败：" + e.getMessage());
        }

        String url = FileConstants.FILE_GET_CONTRACT + fileName;
        Contract update = new Contract();
        update.setId(contractId);
        update.setContractFileUrl(url);
        contractMapper.updateById(update);
        return url;
    }

    private Contract validateContractForGeneration(Long contractId, Long tenantId) {
        Contract c = contractMapper.selectById(contractId);
        if (c == null || (c.getDeleted() != null && c.getDeleted() == 1)) {
            throw new BusinessException("合同不存在");
        }
        if (tenantId != null && !tenantId.equals(c.getTenantId())) {
            throw new BusinessException("无权操作该合同");
        }
        if (!"COMPLETED".equals(c.getStatus())) {
            throw new BusinessException("仅已完成的合同可生成合同文件");
        }
        // 已生成的合同允许重新生成：覆盖 contract_file_url 指向新文件，旧文件留在磁盘上不主动清理
        return c;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static LocalDate toLocalDate(java.util.Date date) {
        if (date == null) return null;
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
