package com.qykj.finmaster.order.service;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.pagehelper.PageInfo;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.FileConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.LogMessage;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.FileUtils;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.common.utils.third.FinancialUtils;
import com.qykj.finmaster.customer.company.domain.CompanyService;
import com.qykj.finmaster.customer.company.domain.CompanyServiceRenew;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CompanyServiceManagerImpl;
import com.qykj.finmaster.customer.company.manager.impl.CompanyServiceRenewManagerImpl;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.impl.CustomerInfoManagerImpl;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.manager.impl.LeadManagerImpl;
import com.qykj.finmaster.order.domain.RefundOrderInfo;
import com.qykj.finmaster.order.domain.dto.OrderDetailInfo;
import com.qykj.finmaster.order.domain.OrderLog;
import com.qykj.finmaster.order.domain.OrderDetail;
import com.qykj.finmaster.order.domain.req.*;
import com.qykj.finmaster.order.domain.resp.*;
import com.qykj.finmaster.order.manager.impl.OrderDetailManagerImpl;
import com.qykj.finmaster.order.manager.impl.OrderLogManagerImpl;
import com.qykj.finmaster.order.manager.impl.OrderManagerImpl;
import com.qykj.finmaster.order.manager.impl.RefundOrderInfoManagerImpl;
import com.qykj.finmaster.order.mapper.OrderDetailMapper;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.dict.service.SysDictDataServiceImpl;
import com.qykj.finmaster.system.product.domain.Product;
import com.qykj.finmaster.system.product.manager.impl.ProductManagerImpl;
import com.qykj.finmaster.task.domain.CommonTask;
import com.qykj.finmaster.task.domain.CommonTaskDetail;
import com.qykj.finmaster.task.manager.impl.CommonTaskDetailManagerImpl;
import com.qykj.finmaster.task.manager.impl.CommonTaskManagerImpl;
import com.qykj.finmaster.contract.domain.Contract;
import com.qykj.finmaster.contract.domain.ContractDetail;
import com.qykj.finmaster.contract.manager.impl.ContractDetailManagerImpl;
import com.qykj.finmaster.contract.manager.impl.ContractManagerImpl;
import com.qykj.finmaster.contract.service.ContractServiceImpl;
import com.qykj.finmaster.system.bussiness.domain.SysBusiness;
import com.qykj.finmaster.system.bussiness.domain.SysBusinessProduct;
import com.qykj.finmaster.system.bussiness.manager.impl.SysBusinessManagerImpl;
import com.qykj.finmaster.system.bussiness.manager.impl.SysBusinessProductManagerImpl;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import com.qykj.finmaster.order.mapper.OrderMapper;
import com.qykj.finmaster.order.domain.Order;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * 订单Service业务层处理
 */
@Service
@lombok.extern.slf4j.Slf4j
public class OrderServiceImpl {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ProductManagerImpl productManager;
    @Autowired
    private CustomerInfoManagerImpl customerInfoManager;
    @Autowired
    private OrderLogServiceImpl orderLogService;
    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;
    @Autowired
    private com.qykj.finmaster.customer.company.service.CustomerCompanyBroadcaster customerCompanyBroadcaster;
    @Autowired
    private LeadManagerImpl leadManager;
    @Autowired
    private OrderManagerImpl orderManager;
    @Autowired
    private OrderDetailManagerImpl orderDetailManager;
    @Autowired
    private OrderLogManagerImpl orderLogManager;
    @Autowired
    private SysDictDataServiceImpl sysDictDataService;
    @Autowired
    private RefundOrderInfoManagerImpl refundOrderInfoManager;
    @Autowired
    private CommonTaskManagerImpl commonTaskManager;
    @Autowired
    private CommonTaskDetailManagerImpl commonTaskDetailManager;
    @Autowired
    private CompanyServiceManagerImpl companyServiceManager;
    @Autowired
    private CompanyServiceRenewManagerImpl companyServiceRenewManager;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private SysBusinessManagerImpl sysBusinessManager;
    @Autowired
    private SysBusinessProductManagerImpl sysBusinessProductManager;
    @Autowired
    private ContractManagerImpl contractManager;
    @Autowired
    private ContractDetailManagerImpl contractDetailManager;
    @Lazy
    @Autowired
    private ContractServiceImpl contractService;

    /** 退款订单创建后自动派发 REFUND 工单（避免循环依赖用 @Lazy） */
    @Lazy
    @Autowired
    private com.qykj.finmaster.workorder.service.WorkOrderService refundWorkOrderService;

    /** 按 refund_order_info.id 查询（详情页/工单详情用） */
    public com.qykj.finmaster.order.domain.RefundOrderInfo getRefundOrderInfo(Long id) {
        return refundOrderInfoManager.getById(id);
    }

    /** 工单审核节点完成时同步退款金额到 Order */
    public void updateReviewedRefundAmount(Long orderId, java.math.BigDecimal amount) {
        if (orderId == null || amount == null) return;
        orderManager.lambdaUpdate()
                .set(Order::getTotalAmount, amount)
                .set(Order::getFinalAmount, amount)
                .set(Order::getPaidAmount, amount)
                .eq(Order::getId, orderId)
                .update();
    }

    /** 工单详情"取消办税"节点的 4 个开关实时保存（is_cancel_tax / is_cancel_task / is_delete_company / is_cancel_account） */
    public void saveRefundCancelFlags(Long id, java.util.Map<String, Object> flags) {
        if (id == null || flags == null) return;
        com.qykj.finmaster.order.domain.RefundOrderInfo upd = new com.qykj.finmaster.order.domain.RefundOrderInfo();
        upd.setId(id);
        java.util.function.Function<String, Integer> pick = k -> {
            Object v = flags.get(k);
            if (v == null) return null;
            if (v instanceof Boolean) return ((Boolean) v) ? 1 : 0;
            try { return Integer.parseInt(v.toString()); } catch (Exception e) { return null; }
        };
        Integer cancelTax = pick.apply("isCancelTax");
        Integer cancelTask = pick.apply("isCancelTask");
        Integer deleteCompany = pick.apply("isDeleteCompany");
        Integer cancelAccount = pick.apply("isCancelAccount");
        if (cancelTax != null)     upd.setIsCancelTax(cancelTax);
        if (cancelTask != null)    upd.setIsCancelTask(cancelTask);
        if (deleteCompany != null) upd.setIsDeleteCompany(deleteCompany);
        if (cancelAccount != null) upd.setIsCancelAccount(cancelAccount);
        refundOrderInfoManager.updateById(upd);
    }

    /**
     * 查询订单
     *
     * @param id 订单ID
     * @return 订单
     */
    public GetOrderInfoResp selectOrderById(Long id, Long tenantId) {
        GetOrderInfoResp resp = new GetOrderInfoResp();
        Order order = orderManager.lambdaQuery()
                .eq(Order::getId, id)
                .one();
        if(ObjectUtils.isEmpty(order)){
            return resp;
        }
        resp = CopierCopyUtils.convertTo(order, GetOrderInfoResp.class);
        List<OrderDetail> orderDetailList = orderDetailManager.lambdaQuery()
                .eq(OrderDetail::getOrderId, order.getId())
                .list();
        if(CollUtil.isNotEmpty(orderDetailList)) {
            Set<Long> companyIdSet = orderDetailList.stream().filter(f -> ObjectUtils.isNotEmpty(f.getCompanyId())).map(OrderDetail::getCompanyId).collect(Collectors.toSet());
            Set<Long> businessIdSet = orderDetailList.stream().filter(f -> ObjectUtils.isNotEmpty(f.getBusinessId())).map(OrderDetail::getBusinessId).collect(Collectors.toSet());
            Map<Long, CustomerCompany> customerCompanyMap = getCompanyInfo(companyIdSet);
            Map<Long, SysBusiness> businessMap = getBusinessMap(businessIdSet);
            List<OrderDetailInfoResp> orderDetailInfoList = orderDetailList.stream().map(item -> {
                OrderDetailInfoResp orderDetailInfoResp = CopierCopyUtils.convertTo(item, OrderDetailInfoResp.class);
                if (CollUtil.isNotEmpty(customerCompanyMap) && ObjectUtils.isNotEmpty(customerCompanyMap.get(item.getCompanyId()))) {
                    CustomerCompany customerCompany = customerCompanyMap.get(item.getCompanyId());
                    orderDetailInfoResp.setCompanyName(customerCompany.getCompanyName());
                    orderDetailInfoResp.setCompanyTaxNumber(customerCompany.getTaxNumber());
                    orderDetailInfoResp.setCompanyType(customerCompany.getCompanyType());
                }
                if (ObjectUtils.isNotEmpty(item.getBusinessId()) && CollUtil.isNotEmpty(businessMap) && businessMap.containsKey(item.getBusinessId())) {
                    orderDetailInfoResp.setBusinessName(businessMap.get(item.getBusinessId()).getBusinessName());
                }
                return orderDetailInfoResp;
            }).toList();
            resp.setOrderDetails(orderDetailInfoList);
        }
        return resp;

    }

    /**
     * 查询订单列表
     *
     * @return 订单
     */
    public List<GetOrderListResp> selectOrderList(GetOrderListInfoReq getOrderListInfoReq, Long tenantId) {
        getOrderListInfoReq.startPage();
        Map<String, Object> params = getOrderListInfoReq.toBeanMap();
        params.put("orderType", Arrays.asList(DictConstants.ORDER_TYPE_NORMAL, DictConstants.ORDER_TYPE_BALANCE));
        // 确保租户ID被设置
        if (ObjectUtil.isEmpty(params.get("tenantId"))) {
            params.put("tenantId", tenantId);
        }
        if(ObjectUtils.isNotEmpty(getOrderListInfoReq.getStart())){
            params.put("startTime", getOrderListInfoReq.getStart().atStartOfDay());
        }
        if(ObjectUtils.isNotEmpty(getOrderListInfoReq.getEnd())){
            params.put("endTime", getOrderListInfoReq.getEnd().atTime(23, 59, 59, 999));
        }
        List<GetOrderListResp> orderListResp = orderMapper.selectOrderList(params);
        if(CollUtil.isEmpty(orderListResp)){
            return Collections.emptyList();
        }
        Set<Long> orderIdList = orderListResp.stream().map(GetOrderListResp::getId).collect(Collectors.toSet());

        List<OrderDetail> orderDetailList = orderDetailManager.lambdaQuery()
                .in(OrderDetail::getOrderId, orderIdList)
                .list();

        if(CollUtil.isNotEmpty(orderDetailList)){
            Set<Long> companyIdSet = orderDetailList.stream().filter(f -> ObjectUtils.isNotEmpty(f.getCompanyId())).map(OrderDetail::getCompanyId).collect(Collectors.toSet());
            Set<Long> businessIdSet = orderDetailList.stream().filter(f -> ObjectUtils.isNotEmpty(f.getBusinessId())).map(OrderDetail::getBusinessId).collect(Collectors.toSet());
            Map<Long, CustomerCompany> customerCompanyMap = getCompanyInfo(companyIdSet);
            Map<Long, SysBusiness> businessMap = getBusinessMap(businessIdSet);
            List<OrderDetailInfoResp> orderDetailInfoList = orderDetailList.stream().map(item -> {
                OrderDetailInfoResp orderDetailInfoResp = CopierCopyUtils.convertTo(item, OrderDetailInfoResp.class);
                if (CollUtil.isNotEmpty(customerCompanyMap) && ObjectUtils.isNotEmpty(customerCompanyMap.get(item.getCompanyId()))) {
                    CustomerCompany customerCompany = customerCompanyMap.get(item.getCompanyId());
                    orderDetailInfoResp.setCompanyName(customerCompany.getCompanyName());
                    orderDetailInfoResp.setCompanyTaxNumber(customerCompany.getTaxNumber());
                    orderDetailInfoResp.setCompanyType(customerCompany.getCompanyType());
                }
                if (ObjectUtils.isNotEmpty(item.getBusinessId()) && CollUtil.isNotEmpty(businessMap) && businessMap.containsKey(item.getBusinessId())) {
                    orderDetailInfoResp.setBusinessName(businessMap.get(item.getBusinessId()).getBusinessName());
                }
                return orderDetailInfoResp;
            }).toList();
            Map<Long, List<OrderDetailInfoResp>> orderDetailInfoListMap = orderDetailInfoList.stream().collect(Collectors.groupingBy(OrderDetailInfoResp::getOrderId));

            //字典（同时加载PAYMENT_TYPE和PAYMENT_TYPE_LMSS，兼容不同部门的支付方式）
            Map<String, String> dictDataMap = sysDictDataService.getDictDataMapByTypeCode(
                    Arrays.asList(DictConstants.PAYMENT_TYPE, DictConstants.PAYMENT_TYPE_LMSS));
            orderListResp.forEach(item ->{
                if(CollUtil.isNotEmpty(orderDetailInfoListMap) && CollUtil.isNotEmpty(orderDetailInfoListMap.get(item.getId()))){
                    item.setOrderDetails(orderDetailInfoListMap.get(item.getId()));
                }
                if(StringUtils.isNotEmpty(item.getPaymentType())) {
                    if (CollUtil.isNotEmpty(dictDataMap) && StringUtils.isNotEmpty(dictDataMap.get(item.getPaymentType()))) {
                        item.setPaymentTypeName(dictDataMap.get(item.getPaymentType()));
                    }
                }
            });
        }

        // 批量标记是否已建过合同（排除作废状态），用于前端隐藏「建合同」按钮
        markContractCreated(orderListResp);

        return orderListResp;
    }

    /**
     * 续费任务详情专用：按 companyId 查公司全部订单（绕过数据权限 / 不分页）。
     * 与 {@link #selectOrderList} 同源，只是不应用 dataScopeUserIds，便于在不属于 owner/butler 的用户也能看到上下文。
     */
    public List<GetOrderListResp> selectOrderListBypassDataScope(GetOrderListInfoReq req, Long tenantId) {
        Map<String, Object> params = req.toBeanMap();
        // 显式清除数据权限过滤
        params.remove("dataScopeUserIds");
        params.put("orderType", Arrays.asList(DictConstants.ORDER_TYPE_NORMAL, DictConstants.ORDER_TYPE_BALANCE));
        if (ObjectUtil.isEmpty(params.get("tenantId"))) {
            params.put("tenantId", tenantId);
        }
        List<GetOrderListResp> orderListResp = orderMapper.selectOrderList(params);
        if (CollUtil.isEmpty(orderListResp)) {
            return Collections.emptyList();
        }
        Set<Long> orderIdList = orderListResp.stream().map(GetOrderListResp::getId).collect(Collectors.toSet());
        List<OrderDetail> orderDetailList = orderDetailManager.lambdaQuery()
                .in(OrderDetail::getOrderId, orderIdList)
                .list();
        if (CollUtil.isNotEmpty(orderDetailList)) {
            Set<Long> companyIdSet = orderDetailList.stream().filter(f -> ObjectUtils.isNotEmpty(f.getCompanyId())).map(OrderDetail::getCompanyId).collect(Collectors.toSet());
            Set<Long> businessIdSet = orderDetailList.stream().filter(f -> ObjectUtils.isNotEmpty(f.getBusinessId())).map(OrderDetail::getBusinessId).collect(Collectors.toSet());
            Map<Long, CustomerCompany> customerCompanyMap = getCompanyInfo(companyIdSet);
            Map<Long, SysBusiness> businessMap = getBusinessMap(businessIdSet);
            List<OrderDetailInfoResp> orderDetailInfoList = orderDetailList.stream().map(item -> {
                OrderDetailInfoResp orderDetailInfoResp = CopierCopyUtils.convertTo(item, OrderDetailInfoResp.class);
                if (CollUtil.isNotEmpty(customerCompanyMap) && ObjectUtils.isNotEmpty(customerCompanyMap.get(item.getCompanyId()))) {
                    CustomerCompany customerCompany = customerCompanyMap.get(item.getCompanyId());
                    orderDetailInfoResp.setCompanyName(customerCompany.getCompanyName());
                    orderDetailInfoResp.setCompanyTaxNumber(customerCompany.getTaxNumber());
                    orderDetailInfoResp.setCompanyType(customerCompany.getCompanyType());
                }
                if (ObjectUtils.isNotEmpty(item.getBusinessId()) && CollUtil.isNotEmpty(businessMap) && businessMap.containsKey(item.getBusinessId())) {
                    orderDetailInfoResp.setBusinessName(businessMap.get(item.getBusinessId()).getBusinessName());
                }
                return orderDetailInfoResp;
            }).toList();
            Map<Long, List<OrderDetailInfoResp>> orderDetailInfoListMap = orderDetailInfoList.stream().collect(Collectors.groupingBy(OrderDetailInfoResp::getOrderId));
            Map<String, String> dictDataMap = sysDictDataService.getDictDataMapByTypeCode(
                    Arrays.asList(DictConstants.PAYMENT_TYPE, DictConstants.PAYMENT_TYPE_LMSS));
            orderListResp.forEach(item -> {
                if (CollUtil.isNotEmpty(orderDetailInfoListMap) && CollUtil.isNotEmpty(orderDetailInfoListMap.get(item.getId()))) {
                    item.setOrderDetails(orderDetailInfoListMap.get(item.getId()));
                }
                if (StringUtils.isNotEmpty(item.getPaymentType())) {
                    if (CollUtil.isNotEmpty(dictDataMap) && StringUtils.isNotEmpty(dictDataMap.get(item.getPaymentType()))) {
                        item.setPaymentTypeName(dictDataMap.get(item.getPaymentType()));
                    }
                }
            });
        }
        markContractCreated(orderListResp);
        return orderListResp;
    }

    /**
     * 批量给订单列表打 contractCreated 标记：
     * 通过 contract_detail.order_no 反查 → 关联 contract.status != DISUSED 即视为"已建过合同"
     */
    private void markContractCreated(List<GetOrderListResp> orderListResp) {
        if (CollUtil.isEmpty(orderListResp)) return;
        Set<String> orderNos = orderListResp.stream()
                .map(GetOrderListResp::getOrderNo)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        if (orderNos.isEmpty()) {
            orderListResp.forEach(o -> o.setContractCreated(Boolean.FALSE));
            return;
        }
        List<ContractDetail> details = contractDetailManager.lambdaQuery()
                .in(ContractDetail::getOrderNo, orderNos)
                .list();
        if (CollUtil.isEmpty(details)) {
            orderListResp.forEach(o -> o.setContractCreated(Boolean.FALSE));
            return;
        }
        Set<Long> contractIds = details.stream()
                .map(ContractDetail::getContractId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> contractStatusMap = contractIds.isEmpty()
                ? Collections.emptyMap()
                : contractManager.listByIds(contractIds).stream()
                    .collect(Collectors.toMap(Contract::getId, Contract::getStatus, (a, b) -> a));
        // 把"有效"（非作废）的合同对应的 orderNo 收集起来
        Set<String> orderNosWithContract = details.stream()
                .filter(cd -> {
                    String st = contractStatusMap.get(cd.getContractId());
                    return st != null && !DictConstants.CONTRACT_STATUS_DISUSED.equals(st);
                })
                .map(ContractDetail::getOrderNo)
                .filter(StringUtils::isNotEmpty)
                .collect(Collectors.toSet());
        orderListResp.forEach(o -> o.setContractCreated(orderNosWithContract.contains(o.getOrderNo())));
    }

    /**
     * 获取字典数据
     * @param code
     * @return
     */
    private Map<String, String> getDictDataMap(String code){
        Map<String, String> dataMap = sysDictDataService.getDictDataMapByTypeCode(code);
        return dataMap;
    }

    /**
     * 获取公司名称map集合
     * @param companyIdSet
     * @return
     */
    private Map<Long, CustomerCompany> getCompanyInfo(Set<Long> companyIdSet){
        Map<Long, CustomerCompany> customerCompanyNameMap = new HashMap<>();
        if (CollUtil.isNotEmpty(companyIdSet)) {
            List<CustomerCompany> customerCompanyList = customerCompanyManager.lambdaQuery()
                    .in(CustomerCompany::getId, companyIdSet)
                    .list();
            customerCompanyNameMap = customerCompanyList.stream().collect(Collectors.toMap(CustomerCompany::getId, item -> item));
        }
        return customerCompanyNameMap;
    }

    private Map<Long, SysBusiness> getBusinessMap(Set<Long> businessIdSet) {
        if (CollUtil.isEmpty(businessIdSet)) {
            return Collections.emptyMap();
        }
        List<SysBusiness> list = sysBusinessManager.lambdaQuery()
                .in(SysBusiness::getId, businessIdSet)
                .list();
        return list.stream().collect(Collectors.toMap(SysBusiness::getId, item -> item));
    }

    /**
     * 转换格式，将订单和明细合并在一起导出
     */
    public List<GetOrderListExportResp> orderAndDetailExport(List<GetOrderListResp> list) {
        // 创建展平的数据列表
        List<GetOrderListExportResp> exportList = new ArrayList<>();

        for (GetOrderListResp order : list) {
            if (order.getOrderDetails() != null && !order.getOrderDetails().isEmpty()) {
                // 如果有明细，为每个明细创建一行
                for (OrderDetailInfoResp detail : order.getOrderDetails()) {
                    GetOrderListExportResp exportItem = new GetOrderListExportResp();
                    // 复制订单信息
                    exportItem.setOrderNo(order.getOrderNo());
                    exportItem.setCustomerName(order.getCustomerName());
                    exportItem.setTaxNumber(detail.getCompanyName());
                    exportItem.setSalesPerson(order.getSalesPerson());
                    exportItem.setTotalAmount(order.getTotalAmount());
                    exportItem.setDiscount(order.getDiscount());
                    exportItem.setFinalAmount(order.getFinalAmount());
                    exportItem.setTaxAmount(order.getTaxAmount());
                    exportItem.setAfterTaxAmount(order.getAfterTaxAmount());
                    exportItem.setPaidAmount(order.getPaidAmount());
                    exportItem.setStatus(order.getStatus());
                    exportItem.setPayTime(order.getPayTime());
                    exportItem.setLeadMonth(order.getLeadMonth());
                    exportItem.setCreateByName(order.getCreateByName());
                    exportItem.setUpdateByName(order.getUpdateByName());
                    // 复制明细信息
                    exportItem.setProductId(detail.getProductId());
                    exportItem.setProductName(detail.getProductName());
                    exportItem.setUnitPrice(detail.getUnitPrice());
                    exportItem.setQuantity(detail.getQuantity());
                    exportItem.setDetailDiscount(detail.getDiscount());
                    exportItem.setTaxRate(detail.getTaxRate());
                    exportItem.setAmount(detail.getAmount());
                    exportItem.setSpecification(detail.getSpecification());
                    exportItem.setUnit(detail.getUnit());
                    exportItem.setEffectiveDate(detail.getEffectiveDate());
                    exportItem.setEndDate(detail.getEndDate());
                    exportItem.setProductNumber(detail.getProductNumber());
                    exportItem.setCompanyName(detail.getCompanyName());
                    exportList.add(exportItem);
                }
            } else {
                // 如果没有明细，创建一行只有订单信息的数据
                GetOrderListExportResp exportItem = new GetOrderListExportResp();
                // 复制订单信息
                exportItem.setOrderNo(order.getOrderNo());
                exportItem.setCustomerName(order.getCustomerName());
                exportItem.setSalesPerson(order.getSalesPerson());
                exportItem.setTotalAmount(order.getTotalAmount());
                exportItem.setDiscount(order.getDiscount());
                exportItem.setFinalAmount(order.getFinalAmount());
                exportItem.setTaxAmount(order.getTaxAmount());
                exportItem.setAfterTaxAmount(order.getAfterTaxAmount());
                exportItem.setPaidAmount(order.getPaidAmount());
                exportItem.setStatus(order.getStatus());
                exportItem.setPayTime(order.getPayTime());
                exportItem.setLeadMonth(order.getLeadMonth());
                exportItem.setCreateByName(order.getCreateByName());
                exportItem.setUpdateByName(order.getUpdateByName());

                exportList.add(exportItem);
            }
        }
        return exportList;
    }

    /**
     * 新增订单
     *
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean insertOrder(AddOrderInfoReq addOrderInfoReq, Long tenantId, LoginUser loginUser) {
        Order order = CopierCopyUtils.convertTo(addOrderInfoReq, Order.class);
        // 生成订单号
        order.setOrderNo(generateOrderNo());
        order.setTenantId(tenantId);
        order.setCreateTime(new Date());
        order.setOrderType(DictConstants.ORDER_TYPE_NORMAL);
        if (ObjectUtils.isNotEmpty(loginUser)) {
            order.setCreateBy(loginUser.getUsername());
            order.setCreateByName(loginUser.getRealName());
        }
        // 添加订单
        addOrder(order, addOrderInfoReq.getOrderDetails(), tenantId, loginUser);
        return Boolean.TRUE;
    }

    /**
     * 修改订单
     *
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateOrder(UpdateOrderInfoReq updateOrderInfoReq, LoginUser loginUser, Long tenantId) {
        Order order = CopierCopyUtils.convertTo(updateOrderInfoReq, Order.class);
        // 获取当前用户信息
        if (ObjectUtils.isNotEmpty(loginUser)) {
            order.setUpdateBy(loginUser.getUsername());
            order.setUpdateByName(loginUser.getRealName());
        }
        order.setUpdateTime(new Date());
        // 更新订单
        int rows = orderManager.getBaseMapper().updateById(order);
        // 查询原有明细，保存其 orderDetailType 映射（id → type），更新时保留原值
        List<OrderDetail> oldDetails = orderDetailManager.lambdaQuery()
                .eq(OrderDetail::getOrderId, order.getId())
                .list();
        Map<Long, String> oldDetailTypeMap = new HashMap<>();
        if (CollUtil.isNotEmpty(oldDetails)) {
            for (OrderDetail od : oldDetails) {
                if (od.getId() != null && od.getOrderDetailType() != null) {
                    oldDetailTypeMap.put(od.getId(), od.getOrderDetailType());
                }
            }
        }
        // 删除原有明细
        LambdaQueryWrapper<OrderDetail> wrapper = Wrappers.lambdaQuery(OrderDetail.class);
        wrapper.eq(OrderDetail::getOrderId, order.getId());
        orderDetailManager.getBaseMapper().delete(wrapper);
        // 更新订单明细
        if (CollUtil.isNotEmpty(updateOrderInfoReq.getOrderDetails())) {
            Order existingOrder = orderManager.getById(order.getId());
            String orderType = existingOrder != null ? existingOrder.getOrderType() : null;
            // 插入新明细
            List<OrderDetail> orderDetailList = updateOrderInfoReq.getOrderDetails().stream().map(item -> {
                // 保存原始ID用于匹配旧明细类型
                Long originalId = item.getId();
                OrderDetail orderDetail = CopierCopyUtils.convertTo(item, OrderDetail.class);
                orderDetail.setId(null); // 清空ID，使用数据库自增生成新ID
                orderDetail.setOrderId(order.getId());
                orderDetail.setCreateTime(new Date());
                orderDetail.setTenantId(tenantId);
                if (ObjectUtils.isNotEmpty(loginUser)) {
                    orderDetail.setUpdateBy(loginUser.getUsername());
                    orderDetail.setUpdateByName(loginUser.getRealName());
                }
                if (DictConstants.ORDER_TYPE_NORMAL.equals(orderType)) {
                    // 已有明细保留原来的 orderDetailType，新增明细才计算
                    if (originalId != null && oldDetailTypeMap.containsKey(originalId)) {
                        orderDetail.setOrderDetailType(oldDetailTypeMap.get(originalId));
                    } else {
                        String detailType = resolveOrderDetailType(existingOrder, orderDetail, tenantId);
                        orderDetail.setOrderDetailType(detailType);
                    }
                }
                return orderDetail;
            }).toList();
            orderDetailManager.saveBatch(orderDetailList);
        }
        // 记录操作日志
        recordOrderLog(order.getId(), GlobalConstant.Str.TWO, LogMessage.UPDATE_ORDER_LOG, tenantId);
        return Boolean.TRUE;
    }

    /**
     * 批量删除订单
     *
     * @param ids 需要删除的订单ID
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteOrderByIds(Long[] ids, Long tenantId) {
        // 删除订单明细
        for (Long id : ids) {
            LambdaQueryWrapper<OrderDetail> wrapper = Wrappers.lambdaQuery(OrderDetail.class);
            wrapper.eq(OrderDetail::getOrderId, id);
            orderDetailManager.getBaseMapper().delete(wrapper);
            // 记录操作日志
            recordOrderLog(id, GlobalConstant.Str.THREE, MessageFormat.format(LogMessage.DELETE_ORDER_LOG, id), tenantId);
        }
        // 删除订单
        int result = orderManager.getBaseMapper().deleteBatchIds(Arrays.asList(ids));
        return result;
    }

    /**
     * 删除订单信息
     *
     * @param id 订单ID
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public int deleteOrderById(Long id, Long tenantId) {
        // 验证租户权限
        List<Order> existingOrder = orderMapper.selectOrderById(id);
        if (existingOrder == null || !tenantId.equals(existingOrder.get(0).getTenantId())) {
            throw new RuntimeException("无权限删除此订单");
        }

        // 删除订单明细
        LambdaQueryWrapper<OrderDetail> wrapper = Wrappers.lambdaQuery(OrderDetail.class);
        wrapper.eq(OrderDetail::getOrderId, id);
        orderDetailManager.getBaseMapper().delete(wrapper);
        // 记录操作日志
        recordOrderLog(id, GlobalConstant.Str.THREE, MessageFormat.format(LogMessage.DELETE_ORDER_LOG, id), tenantId);
        // 删除订单
        int result = orderMapper.deleteOrderById(id);

        return result;
    }

    /**
     * 更新订单状态
     *
     * @param id         订单ID
     * @param status     订单状态
     * @param paidAmount 已付金额（可为空，为空则不更新已付金额）
     * @return 结果
     */
    public int updateOrderStatus(Long id, String status, BigDecimal paidAmount, Long tenantId, LoginUser loginUser) {
        // 验证租户权限
        List<Order> existingOrder = orderMapper.selectOrderById(id);
        if (existingOrder == null || !tenantId.equals(existingOrder.get(0).getTenantId())) {
            throw new RuntimeException("无权限修改此订单");
        }

        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        // 如果前端传了新的已付金额，则一并更新
        if (paidAmount != null) {
            order.setPaidAmount(paidAmount);
        }
        order.setUpdateTime(new Date());

        // 获取当前用户信息
        if (ObjectUtils.isNotEmpty(loginUser)) {
            order.setUpdateBy(loginUser.getUsername());
            order.setUpdateByName(loginUser.getRealName());
        }

        // 记录操作日志
        recordOrderLog(id, GlobalConstant.Str.FOUR, MessageFormat.format(LogMessage.UPDATE_ORDER_STATUS_LOG, status), tenantId);
        int result = orderMapper.updateOrder(order);

        return result;
    }

    /**
     * 生成订单号
     */
    private String generateOrderNo() {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // 获取当前用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        String username = loginUser.getUsername();
        // 生成6位随机数
        int random = (int) (Math.random() * 10000);
        return String.format("SO%s%06d", dateStr, random);
    }

    /**
     * 记录订单操作日志
     */
    private void recordOrderLog(Long orderId, String logType, String content, Long tenantId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();

        OrderLog log = new OrderLog();
        log.setLogType(logType);
        log.setContent(content);
        log.setOperator(loginUser.getUsername());
        log.setOperatorName(loginUser.getRealName());
        log.setCreateTime(new Date());
        log.setTenantId(tenantId);
        orderLogManager.save(log);
    }

    /**
     * 创建订单
     *
     * @param openId
     * @param companyId
     * @param productId
     * @return
     */
    public Boolean createOrder(String orderNumber, String openId, Long companyId, Long productId) {
        Order order = new Order();
        order.setOrderNo(orderNumber);
        order.setPaymentType(DictConstants.PAYMENT_TYPE_WX_APPLET_HZZ);
        order.setOrderType(DictConstants.ORDER_TYPE_NORMAL);
        Product product = productManager.lambdaQuery()
                .eq(Product::getId, productId)
                .one();
        if (ObjectUtils.isNotEmpty(product)) {
            order.setTotalAmount(product.getUnitPrice());
            order.setDiscount(GlobalConstant.Number.ZERO_INT);
            order.setFinalAmount(product.getUnitPrice());
            order.setTaxAmount(new BigDecimal(GlobalConstant.Number.ZERO_INT));
            order.setAfterTaxAmount(product.getUnitPrice());
            order.setPaidAmount(new BigDecimal(GlobalConstant.Number.ZERO_INT));
            order.setStatus(DictConstants.ORDER_STATUS_PENDING);
        }
        CustomerInfo customerInfo = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getOpenId, openId)
                .one();
        // 订单租户赋值 首先在客户信息上获取，如果没有客户在线索上获取
        if (ObjectUtils.isNotEmpty(customerInfo)) {
            order.setTenantId(customerInfo.getTenantId());
            order.setCustomerId(customerInfo.getId());
            order.setCustomerName(customerInfo.getCustomerName());
        } else {
            Lead lead = leadManager.lambdaQuery()
                    .eq(Lead::getOpenId, openId)
                    .one();
            if (ObjectUtils.isNotEmpty(lead)) {
                order.setTenantId(lead.getTenantId());
            }
        }
        orderManager.save(order);

        OrderDetail detail = new OrderDetail();
        detail.setOrderId(order.getId());
        detail.setCompanyId(companyId);
        detail.setProductId(productId);
        detail.setProductName(product.getProductName());
        detail.setProductNumber(product.getProductNumber());
        detail.setSpecification(product.getSpecification());
        detail.setUnit(product.getUnit()); // 设置单位信息
        detail.setUnitPrice(product.getUnitPrice());
        detail.setQuantity(GlobalConstant.Number.ONE_INT);
        detail.setDiscount(GlobalConstant.Number.ZERO_INT);
        detail.setTaxRate(new BigDecimal(GlobalConstant.Number.ZERO_INT));
        detail.setAmount(product.getUnitPrice());
        detail.setTenantId(order.getTenantId());
        detail.setCreateTime(new Date());
        detail.setUpdateTime(new Date());
        // 微信小程序订单：按产品+租户解析业务ID，并与后端一致计算订单明细类型
        Long tenantId = order.getTenantId();
        SysBusinessProduct sbProduct = null;
        if (tenantId != null) {
            sbProduct = sysBusinessProductManager.lambdaQuery()
                    .eq(SysBusinessProduct::getProductId, productId)
                    .eq(SysBusinessProduct::getTenantId, tenantId)
                    .last("LIMIT 1")
                    .one();
        } else {
            sbProduct = sysBusinessProductManager.lambdaQuery()
                    .eq(SysBusinessProduct::getProductId, productId)
                    .last("LIMIT 1")
                    .one();
        }
        if (sbProduct != null) {
            detail.setBusinessId(sbProduct.getBusinessId());
        }
        String detailType = DictConstants.ORDER_DETAIL_TYPE_NEW;
        if (tenantId != null) {
            detailType = resolveOrderDetailType(order, detail, tenantId);
        }
        detail.setOrderDetailType(detailType);
        orderDetailManager.save(detail);
        // 记录操作日志
        orderLogService.createLog(order.getId(), GlobalConstant.Str.ONE, LogMessage.CREATE_ORDER_LOG);
        return Boolean.TRUE;
    }

    public int updateOrderDetail(OrderDetailInfo orderDetailInfo) {
        OrderDetail orderDetail = CopierCopyUtils.convertTo(orderDetailInfo, OrderDetail.class);
        return orderDetailManager.getBaseMapper().updateById(orderDetail);
    }

    public Boolean updateOrderCompany(Long id, Long companyId, LoginUser loginUser) {
        OrderDetail orderDetail = orderDetailManager.getById(id);
        if (ObjectUtils.isEmpty(orderDetail)) {
            throw new BusinessException(ResultMsg.ORDER_DETAIL_NOT_DATA);
        }
        orderDetailManager.lambdaUpdate()
                .set(OrderDetail::getCompanyId, companyId)
                .set(OrderDetail::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), OrderDetail::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), OrderDetail::getUpdateByName, loginUser.getRealName())
                .eq(OrderDetail::getId, id)
                .update();
        // 记录操作日志
        orderLogService.createLog(id, GlobalConstant.Str.TWO,
                MessageFormat.format(LogMessage.UPDATE_ORDER_DETAIL_COMPANY_LOG,
                        orderDetail.getOrderId(), orderDetail.getCompanyId(), companyId));
        return Boolean.TRUE;
    }

    public Boolean updateOrderLeadMonth(Long id, String leadMonth, LoginUser loginUser) {
        Order order = orderManager.getById(id);
        if (ObjectUtils.isEmpty(order)) {
            throw new BusinessException(ResultMsg.ORDER_NOT_DATA);
        }
        orderManager.lambdaUpdate()
                .set(Order::getLeadMonth, leadMonth)
                .set(Order::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), Order::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), Order::getUpdateByName, loginUser.getRealName())
                .eq(Order::getId, id)
                .update();
        // 记录操作日志
        orderLogService.createLog(id, GlobalConstant.Str.TWO,
                MessageFormat.format(LogMessage.UPDATE_ORDER_LEAD_MONTH_LOG,
                        order.getId(), order.getLeadMonth(), leadMonth));
        return Boolean.TRUE;
    }

    /**
     * 新增退款订单
     *
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addRefundOrder(AddRefundOrderReq req, Long tenantId, LoginUser loginUser) {
        if(StringUtils.isEmpty(req.getSourceOrderNo())){
            return Boolean.FALSE;
        }
        // 先查原销售订单：用来取 companyId，并修改其状态为退款中
        Order sourceOrder = orderManager.lambdaQuery()
                .eq(Order::getOrderNo, req.getSourceOrderNo())
                .last("LIMIT 1")
                .one();
        orderManager.lambdaUpdate()
                .set(Order::getStatus, DictConstants.ORDER_STATUS_REFUNDING)
                .eq(Order::getOrderNo, req.getSourceOrderNo())
                .update();

        // 生成订单参数（退款订单）
        Order order = CopierCopyUtils.convertTo(req, Order.class);
        // 生成退款订单号
        order.setOrderNo(generateRefundOrderNo());
        order.setOrderType(DictConstants.ORDER_TYPE_REFUND);
        // companyId 必填：AddRefundOrderReq 没这个字段，从原销售订单继承
        // （财务系统停用账套等钩子依赖该字段）
        if (sourceOrder != null && sourceOrder.getCompanyId() != null) {
            order.setCompanyId(sourceOrder.getCompanyId());
        }
        // 退款订单默认数据：均视为待付款，已付金额为0
        order.setStatus(DictConstants.ORDER_STATUS_PENDING);
        order.setDiscount(GlobalConstant.Number.ZERO_INT);
        order.setPaidAmount(BigDecimal.ZERO);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setAfterTaxAmount(BigDecimal.ZERO);
        order.setTenantId(tenantId);
        order.setCreateTime(new Date());
        if (ObjectUtils.isNotEmpty(loginUser)) {
            order.setCreateBy(loginUser.getUsername());
            order.setCreateByName(loginUser.getRealName());
        }
        // 添加订单
        addOrder(order, req.getOrderDetails(), tenantId, loginUser);
        // 添加退款订单信息
        addRefundOrderInfo(order, req);

        // 自动派发 REFUND 工单（白名单按 step 分配负责人；前端再确认/调整）
        try {
            com.qykj.finmaster.order.domain.RefundOrderInfo info = refundOrderInfoManager.lambdaQuery()
                    .eq(com.qykj.finmaster.order.domain.RefundOrderInfo::getOrderNo, order.getOrderNo())
                    .last("LIMIT 1")
                    .one();
            com.qykj.finmaster.workorder.domain.req.CreateWorkOrderReq cr =
                    new com.qykj.finmaster.workorder.domain.req.CreateWorkOrderReq();
            cr.setTaskType(com.qykj.finmaster.workorder.constants.WorkOrderTaskType.REFUND.name());
            cr.setSourceType(com.qykj.finmaster.workorder.constants.WorkOrderSourceType.MANUAL.name());
            cr.setSourceId(info != null ? info.getId() : null);   // refund_order_info.id
            cr.setSourceNumber(order.getOrderNo());                // 退款订单号
            cr.setOrderId(order.getId());
            cr.setCustomerId(order.getCustomerId());
            // companyId 取退款订单的；为空则按原销售订单 sourceOrderNo 反查 / 再回退到 customer 第一家公司
            Long companyId = order.getCompanyId();
            if (companyId == null && StringUtils.isNotBlank(req.getSourceOrderNo())) {
                com.qykj.finmaster.order.domain.Order srcOrder = orderManager.lambdaQuery()
                        .eq(com.qykj.finmaster.order.domain.Order::getOrderNo, req.getSourceOrderNo())
                        .last("LIMIT 1")
                        .one();
                if (srcOrder != null) companyId = srcOrder.getCompanyId();
            }
            if (companyId == null && order.getCustomerId() != null) {
                // 客户名下任一家有税号的公司
                com.qykj.finmaster.customer.company.domain.CustomerCompany cc = customerCompanyManager.lambdaQuery()
                        .eq(com.qykj.finmaster.customer.company.domain.CustomerCompany::getCustomerId, order.getCustomerId())
                        .isNotNull(com.qykj.finmaster.customer.company.domain.CustomerCompany::getTaxNumber)
                        .last("LIMIT 1")
                        .one();
                if (cc != null) companyId = cc.getId();
            }
            cr.setCompanyId(companyId);
            cr.setTitle("退款 - " + StringUtils.defaultString(order.getCustomerName(), "客户 #" + order.getCustomerId()));
            String desc = "退款金额 ¥" + order.getFinalAmount() + "，来源订单 " + req.getSourceOrderNo();
            if (StringUtils.isNotBlank(req.getRefundReason())) {
                desc += "\n退款原因：" + req.getRefundReason();
            }
            cr.setDescription(desc);
            cr.setRemark(req.getRemark());
            // 退款凭证 / 退款原因 / 退款方式：透传到 metaJson，工单详情可直接读取
            try {
                java.util.Map<String, Object> meta = new java.util.HashMap<>();
                if (StringUtils.isNotBlank(req.getRefundReason())) meta.put("refundReason", req.getRefundReason());
                if (StringUtils.isNotBlank(req.getRefundMethod())) meta.put("refundMethod", req.getRefundMethod());
                if (StringUtils.isNotBlank(req.getRefundImages())) {
                    try { meta.put("refundImages", com.alibaba.fastjson.JSON.parseArray(req.getRefundImages())); }
                    catch (Exception ex) { meta.put("refundImages", java.util.Collections.emptyList()); }
                }
                if (!meta.isEmpty()) cr.setMetaJson(com.alibaba.fastjson.JSON.toJSONString(meta));
            } catch (Exception ignore) { /* 不阻断 */ }
            // 退款工单第一节点（REFUND_REVIEW）持有人 = 前端选定的审核人；未指定时回退到当前用户兜底
            String reviewer = StringUtils.isNotBlank(req.getReviewerUser())
                    ? req.getReviewerUser()
                    : (loginUser != null ? loginUser.getUsername() : null);
            cr.setAssignedOwnerUser(reviewer);
            refundWorkOrderService.createWorkOrder(cr, loginUser, tenantId);
        } catch (Exception e) {
            // 派工失败不影响退款订单本身的创建
            log.error("[退款] 自动派发 REFUND 工单失败，请在工单页手动新建", e);
        }
        return Boolean.TRUE;
    }

    // 服务转移已迁移到合同驱动版本：com.qykj.finmaster.order.service.ServiceTransferService
    // 入口端点：POST /contract/serviceTransfer

    /**
     * 新增补款订单
     * 补款订单类型 BALANCE，记录原订单号 source_order_no，明细记录原明细ID source_detail_id，业务强制为补款
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addBalanceOrder(AddBalanceOrderReq req, Long tenantId, LoginUser loginUser) {
        if (StringUtils.isEmpty(req.getSourceOrderNo()) || CollUtil.isEmpty(req.getOrderDetails())) {
            return Boolean.FALSE;
        }
        SysBusiness balanceBusiness = sysBusinessManager.lambdaQuery()
                .eq(SysBusiness::getBusinessCode, DictConstants.BUSINESS_CODE_BALANCE)
                .last("LIMIT 1")
                .one();
        if (balanceBusiness == null) {
            throw new BusinessException("未配置补款业务（业务编码BALANCE），无法创建补款订单");
        }
        Long balanceBusinessId = balanceBusiness.getId();
        for (OrderDetailInfoReq d : req.getOrderDetails()) {
            d.setBusinessId(balanceBusinessId);
        }
        Order order = CopierCopyUtils.convertTo(req, Order.class);
        order.setId(null);
        order.setOrderNo(generateBalanceOrderNo());
        order.setOrderType(DictConstants.ORDER_TYPE_BALANCE);
        order.setSourceOrderNo(req.getSourceOrderNo());
        order.setStatus(StringUtils.isNotEmpty(req.getStatus()) ? req.getStatus() : DictConstants.ORDER_STATUS_PENDING);
        if (StringUtils.isNotEmpty(req.getPaymentType())) {
            order.setPaymentType(req.getPaymentType());
        }
        if (StringUtils.isNotEmpty(req.getPayTime())) {
            try {
                order.setPayTime(LocalDateTime.parse(req.getPayTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (Exception ignored) {}
        }
        order.setDiscount(GlobalConstant.Number.ZERO_INT);
        order.setTaxAmount(BigDecimal.ZERO);
        order.setAfterTaxAmount(BigDecimal.ZERO);
        // 补款订单：已付款时已付金额等于最终金额，待付款时为0
        if (DictConstants.ORDER_STATUS_PAID.equals(order.getStatus())) {
            order.setPaidAmount(order.getFinalAmount() != null ? order.getFinalAmount() : BigDecimal.ZERO);
        } else {
            order.setPaidAmount(BigDecimal.ZERO);
        }
        order.setTenantId(tenantId);
        order.setCreateTime(new Date());
        if (ObjectUtils.isNotEmpty(loginUser)) {
            order.setCreateBy(loginUser.getUsername());
            order.setCreateByName(loginUser.getRealName());
        }
        addOrder(order, req.getOrderDetails(), tenantId, loginUser);
        return Boolean.TRUE;
    }

    /**
     * 添加退款订单信息
     * @param order
     * @param req
     */
    @Transactional(rollbackFor = Exception.class)
    public void addRefundOrderInfo(Order order, AddRefundOrderReq req) {
        RefundOrderInfo info = CopierCopyUtils.convertTo(order, RefundOrderInfo.class);
        info.setId(null);
        info.setBankName(req.getBankName());
        info.setBankAccount(req.getBankAccount());
        info.setBankAccountName(req.getBankAccountName());
        info.setStatus(DictConstants.ORDER_REFUND_STATUS_REVIEW);
        refundOrderInfoManager.save(info);
    }

    /**
     * 新增订单
     * @param order
     * @param orderDetails
     * @param tenantId
     * @param loginUser
     */
    @Transactional(rollbackFor = Exception.class)
    public void addOrder(Order order, List<OrderDetailInfoReq> orderDetails, Long tenantId, LoginUser loginUser){
        // 插入订单
        orderManager.save(order);
        // 插入订单明细
        if (CollUtil.isNotEmpty(orderDetails)) {
            List<OrderDetail> orderDetailList = orderDetails.stream().map(item -> {
                OrderDetail orderDetail = CopierCopyUtils.convertTo(item, OrderDetail.class);
                orderDetail.setTenantId(tenantId);
                orderDetail.setOrderId(order.getId());
                orderDetail.setCreateTime(new Date());
                if (ObjectUtils.isNotEmpty(loginUser)) {
                    orderDetail.setCreateBy(loginUser.getUsername());
                    orderDetail.setCreateByName(loginUser.getRealName());
                }
                // 仅正常订单计算订单明细类型；补款/续费/新增规则；补款订单明细类型固定为 BALANCE；退款订单明细类型固定为 BALANCE
                if (DictConstants.ORDER_TYPE_NORMAL.equals(order.getOrderType())) {
                    String detailType = resolveOrderDetailType(order, orderDetail, tenantId);
                    orderDetail.setOrderDetailType(detailType);
                } else if (DictConstants.ORDER_TYPE_BALANCE.equals(order.getOrderType())) {
                    orderDetail.setOrderDetailType(DictConstants.ORDER_DETAIL_TYPE_BALANCE);
                } else if (DictConstants.ORDER_TYPE_REFUND.equals(order.getOrderType())) {
                    orderDetail.setOrderDetailType(DictConstants.ORDER_DETAIL_TYPE_REFUND);
                }
                return orderDetail;
            }).toList();
            orderDetailManager.saveBatch(orderDetailList);
        }
        // 记录操作日志
        recordOrderLog(order.getId(), GlobalConstant.Str.ONE, LogMessage.CREATE_ORDER_LOG, tenantId);
    }

    /**
     * 计算订单明细类型：NEW-新增 RENEW-续费 BALANCE-补款
     * 规则：补款业务(business_code=BALANCE)直接BALANCE；否则按同公司同业务正常订单明细数量与check_renewal_count比较；
     * 数量=0时再查是否存在同公司同业务非废弃合同，存在则RENEW否则NEW。
     */
    private String resolveOrderDetailType(Order order, OrderDetail detail, Long tenantId) {
        Long companyId = detail.getCompanyId();
        Long businessId = detail.getBusinessId();
        if (businessId == null) {
            return DictConstants.ORDER_DETAIL_TYPE_NEW;
        }
        SysBusiness business = sysBusinessManager.getById(businessId);
        if (business == null) {
            return DictConstants.ORDER_DETAIL_TYPE_NEW;
        }
        if (DictConstants.BUSINESS_CODE_BALANCE.equals(business.getBusinessCode())) {
            return DictConstants.ORDER_DETAIL_TYPE_BALANCE;
        }
        int checkRenewalCount = business.getCheckRenewalCount() != null ? business.getCheckRenewalCount() : GlobalConstant.Number.ZERO_INT;
        if (companyId == null) {
            return DictConstants.ORDER_DETAIL_TYPE_NEW;
        }
        List<Long> companyIds = customerCompanyManager.getCompanyIdsBySameTaxNumber(companyId, tenantId);
        if (CollUtil.isEmpty(companyIds)) {
            companyIds = Collections.singletonList(companyId);
        }
        Long balanceBusinessId = getBalanceBusinessId();
        long count = orderDetailMapper.countNormalOrderDetailsByCompanyAndBusiness(tenantId, companyIds, businessId, balanceBusinessId);
        if(count > GlobalConstant.Number.ZERO_INT) {
            if (count >= checkRenewalCount) {
                return DictConstants.ORDER_DETAIL_TYPE_RENEW;
            }
        }
        if (count == GlobalConstant.Number.ZERO_INT) {
            boolean existsContract = contractService.existsNonDisusedContractByCompanyAndBusiness(tenantId, companyId, businessId);
            return existsContract ? DictConstants.ORDER_DETAIL_TYPE_RENEW : DictConstants.ORDER_DETAIL_TYPE_NEW;
        }
        return DictConstants.ORDER_DETAIL_TYPE_NEW;
    }

    /**
     * 将指定订单明细中当前为 RENEW 的更新为 NEW（供合同完成时调用）
     *
     * @param orderDetailIds 订单明细ID列表
     * @return 更新条数
     */
    public int updateOrderDetailTypeToNewIfRenew(List<Long> orderDetailIds) {
        if (CollUtil.isEmpty(orderDetailIds)) {
            return GlobalConstant.Number.ZERO_INT;
        }
        LambdaUpdateWrapper<OrderDetail> wrapper = Wrappers.lambdaUpdate(OrderDetail.class)
                .set(OrderDetail::getOrderDetailType, DictConstants.ORDER_DETAIL_TYPE_NEW)
                .set(OrderDetail::getUpdateTime, new Date())
                .in(OrderDetail::getId, orderDetailIds)
                .eq(OrderDetail::getOrderDetailType, DictConstants.ORDER_DETAIL_TYPE_RENEW);
        return orderDetailManager.getBaseMapper().update(null, wrapper);
    }

    /**
     * 根据公司获取「不可新增」的业务ID列表（用于新增订单时排除不可选业务）
     * 规则：业务A的 check_add_business_id 表示「存在这些业务时，不可新增本业务A」。
     * 即：该公司下已存在某业务X时，若某业务A的 check_add_business_id 包含 X，则业务A不可选。
     *
     * @param companyId 公司ID
     * @param tenantId  租户ID
     * @return 不可选的业务ID列表（即「本业务」不可选的那些业务的ID）
     */
    public List<Long> getBlockedBusinessIdsByCompany(Long companyId, Long tenantId) {
        if (companyId == null || tenantId == null) {
            return Collections.emptyList();
        }
        List<Long> companyIds = customerCompanyManager.getCompanyIdsBySameTaxNumber(companyId, tenantId);
        if (CollUtil.isEmpty(companyIds)) {
            companyIds = Collections.singletonList(companyId);
        }
        Set<Long> existingBusinessIds = new HashSet<>();
        // 1. 该公司（同税号所有公司ID）下正常订单明细中的业务ID
        List<OrderDetail> details = orderDetailManager.lambdaQuery()
                .in(OrderDetail::getCompanyId, companyIds)
                .eq(OrderDetail::getTenantId, tenantId)
                .list();
        if (CollUtil.isNotEmpty(details)) {
            Set<Long> orderIds = details.stream().map(OrderDetail::getOrderId).filter(Objects::nonNull).collect(Collectors.toSet());
            if (!orderIds.isEmpty()) {
                List<Order> orders = orderManager.listByIds(orderIds);
                Set<Long> normalOrderIds = orders.stream()
                        .filter(o -> DictConstants.ORDER_TYPE_NORMAL.equals(o.getOrderType()) &&
                                !DictConstants.ORDER_STATUS_CANCELLED.equals(o.getStatus()))
                        .map(Order::getId)
                        .collect(Collectors.toSet());
                details.stream()
                        .filter(d -> normalOrderIds.contains(d.getOrderId()) && d.getBusinessId() != null)
                        .map(OrderDetail::getBusinessId)
                        .forEach(existingBusinessIds::add);
            }
        }
        // 2. 该公司（同税号所有公司ID）下非废弃合同的业务ID
        contractManager.lambdaQuery()
                .in(Contract::getCompanyId, companyIds)
                .eq(Contract::getTenantId, tenantId)
                .ne(Contract::getStatus, DictConstants.CONTRACT_STATUS_DISUSED)
                .list()
                .stream()
                .map(Contract::getBusinessId)
                .filter(Objects::nonNull)
                .forEach(existingBusinessIds::add);

        // 补款业务不参与：从已有业务集合中移除补款业务ID，不作为「存在业务」参与不可选判断
        Long balanceBusinessId = getBalanceBusinessId();
        if (balanceBusinessId != null) {
            existingBusinessIds.remove(balanceBusinessId);
        }
        if (existingBusinessIds.isEmpty()) {
            return Collections.emptyList();
        }
        // 3. 找出所有「本业务」不可选的业务：若业务B的 check_add_business_id 与该公司已有业务有交集，则业务B不可选
        Set<Long> blocked = new HashSet<>();
        List<SysBusiness> allWithCheck = sysBusinessManager.lambdaQuery()
                .isNotNull(SysBusiness::getCheckAddBusinessId)
                .ne(SysBusiness::getCheckAddBusinessId, "")
                .list();
        for (SysBusiness b : allWithCheck) {
            String checkAdd = b.getCheckAddBusinessId();
            if (StringUtils.isEmpty(checkAdd)) continue;
            Set<Long> checkIds = new HashSet<>();
            for (String s : checkAdd.split(",")) {
                s = s.trim();
                if (s.isEmpty()) continue;
                try {
                    checkIds.add(Long.parseLong(s));
                } catch (NumberFormatException ignored) {
                }
            }
            if (CollUtil.isEmpty(checkIds)) continue;
            checkIds.retainAll(existingBusinessIds);
            if (!checkIds.isEmpty()) {
                blocked.add(b.getId());
            }
        }
        return new ArrayList<>(blocked);
    }

    /** 补款业务不参与合同订单查询和订单明细类型校验：返回补款业务ID，未配置则 null */
    private Long getBalanceBusinessId() {
        SysBusiness b = sysBusinessManager.lambdaQuery()
                .eq(SysBusiness::getBusinessCode, DictConstants.BUSINESS_CODE_BALANCE)
                .last("LIMIT 1")
                .one();
        return b != null ? b.getId() : null;
    }

    /**
     * 生成退款订单号
     */
    private String generateRefundOrderNo() {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = (int) (Math.random() * 10000);
        return String.format("REFUND%s%06d", dateStr, random);
    }

    private String generateBalanceOrderNo() {
        LocalDate today = LocalDate.now();
        String dateStr = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int random = (int) (Math.random() * 10000);
        return String.format("BALANCE%s%06d", dateStr, random);
    }

    /**
     * 查询退款订单
     * @param req
     * @return
     */
    public List<GetRefundOrderListResp> getRefundOrderPage(GetRefundOrderPageReq req, Long tenantId, AtomicLong total) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        params.put("orderType", Arrays.asList(DictConstants.ORDER_TYPE_REFUND));

        if(ObjectUtils.isNotEmpty(req.getStart())){
            params.put("startTime", req.getStart().atStartOfDay());
        }
        if(ObjectUtils.isNotEmpty(req.getEnd())){
            params.put("endTime", req.getEnd().atTime(23, 59, 59, 999));
        }
        // 确保租户ID被设置
        if (ObjectUtil.isEmpty(params.get("tenantId"))) {
            params.put("tenantId", tenantId);
        }
        List<GetOrderListResp> orderListResp = orderMapper.selectOrderList(params);
        // pagehelper的total映射在sql查询中映射，需赋值
        total.set(new PageInfo<>(orderListResp).getTotal());
        if(CollUtil.isEmpty(orderListResp)){
            return Collections.emptyList();
        }
        List<GetRefundOrderListResp> respList = CopierCopyUtils.convertTo(orderListResp, GetRefundOrderListResp.class);
        if(CollUtil.isNotEmpty(respList)){
            // 订单明细
            Set<Long> orderIdList = respList.stream().map(GetRefundOrderListResp::getId).collect(Collectors.toSet());
            Map<Long, List<OrderDetailInfoResp>> orderDetailInfoListMap = new HashMap<>();
            if (CollUtil.isNotEmpty(orderIdList)) {
                List<OrderDetail> orderDetailList = orderDetailManager.lambdaQuery()
                        .in(OrderDetail::getOrderId, orderIdList)
                        .list();
                if (CollUtil.isNotEmpty(orderDetailList)) {
                    // 提取公司ID并查询公司信息
                    Set<Long> companyIdSet = orderDetailList.stream()
                            .map(OrderDetail::getCompanyId)
                            .filter(ObjectUtils::isNotEmpty)
                            .collect(Collectors.toSet());

                    Map<Long, CustomerCompany> customerCompanyMap = getCompanyInfo(companyIdSet);
                    // 转换订单明细并分组
                    orderDetailInfoListMap = orderDetailList.stream()
                            .map(item -> convertToOrderDetailInfoResp(item, customerCompanyMap))
                            .collect(Collectors.groupingBy(OrderDetailInfoResp::getOrderId));
                }
            }
            // 退款订单信息
            List<String> orderNoList = respList.stream().map(GetRefundOrderListResp::getOrderNo).toList();
            Map<String, RefundOrderInfo> refundOrderInfoMap = new HashMap<>();
            if (CollUtil.isNotEmpty(orderNoList)) {
                refundOrderInfoMap = refundOrderInfoManager.lambdaQuery()
                        .in(RefundOrderInfo::getOrderNo, orderNoList)
                        .list()
                        .stream()
                        .collect(Collectors.toMap(RefundOrderInfo::getOrderNo, v -> v));
            }
            Map<Long, List<OrderDetailInfoResp>> finalOrderDetailInfoListMap = orderDetailInfoListMap;
            Map<String, RefundOrderInfo> finalRefundOrderInfoMap = refundOrderInfoMap;
            respList.forEach(item -> {
                // 设置订单明细
                List<OrderDetailInfoResp> details = finalOrderDetailInfoListMap.get(item.getId());
                if (CollUtil.isNotEmpty(details)) {
                    item.setOrderDetails(details);
                }
                // 设置退款订单信息
                RefundOrderInfo refundOrderInfo = finalRefundOrderInfoMap.get(item.getOrderNo());
                if (refundOrderInfo != null) {
                    setRefundOrderInfo(item, refundOrderInfo);
                }
            });
        }
        return respList;
    }

    /**
     * 转换订单明细为响应对象
     * @param orderDetail
     * @param customerCompanyMap
     * @return
     */
    private OrderDetailInfoResp convertToOrderDetailInfoResp(OrderDetail orderDetail, Map<Long, CustomerCompany> customerCompanyMap) {
        OrderDetailInfoResp resp = CopierCopyUtils.convertTo(orderDetail, OrderDetailInfoResp.class);
        CustomerCompany customerCompany = customerCompanyMap.get(orderDetail.getCompanyId());
        if (customerCompany != null) {
            resp.setCompanyName(customerCompany.getCompanyName());
            resp.setCompanyTaxNumber(customerCompany.getTaxNumber());
            resp.setCompanyType(customerCompany.getCompanyType());
        }

        return resp;
    }

    /**
     * 设置退款订单信息
     * @param target
     * @param source
     */
    private void setRefundOrderInfo(GetRefundOrderListResp target, RefundOrderInfo source) {
        target.setBankName(source.getBankName());
        target.setBankAccount(source.getBankAccount());
        target.setBankAccountName(source.getBankAccountName());
        target.setIsTransfer(source.getIsTransfer());
        target.setTransferImg(source.getTransferImg());
        target.setIsCancelTax(source.getIsCancelTax());
        target.setCancelTaxImg(source.getCancelTaxImg());
        target.setIsCancelTask(source.getIsCancelTask());
        target.setIsCancelAccount(source.getIsCancelAccount());
        target.setIsDeleteCompany(source.getIsDeleteCompany());
        target.setRefundStatus(source.getStatus());
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean reviewRefundOrder(ReviewRefundOrderReq req){
        if(StringUtils.isEmpty(req.getOrderNo())){
            return Boolean.FALSE;
        }
        Order order = orderManager.lambdaQuery()
                .eq(Order::getOrderNo, req.getOrderNo())
                .one();
        // 审核通过
        if(req.getResult()){
            List<OrderDetail> orderDetailList = orderDetailManager.lambdaQuery()
                    .eq(OrderDetail::getOrderId, order.getId())
                    .list();
            if (CollUtil.isNotEmpty(orderDetailList)) {
                List<Long> orderDetailIdList = orderDetailList.stream().map(OrderDetail::getSourceDetailId).toList();
                List<Long> companyIdList = orderDetailList.stream().map(OrderDetail::getCompanyId).toList();
                //强行取消任务(直接删除)
                delCommonTask(order, orderDetailIdList);
                //删除公司服务
                delCompanyService(order, orderDetailIdList);
                if (CollUtil.isNotEmpty(companyIdList)) {
                    //取消小程序授权
                    cancelVerityTax(companyIdList);
                    List<CustomerCompany> customerCompanyList = customerCompanyManager.lambdaQuery()
                            .in(CustomerCompany::getId, companyIdList)
                            .list();
                    List<String> taxNumList = customerCompanyList.stream().map(CustomerCompany::getTaxNumber).toList();
                    //账套停用
                    deactivatedAccountSets(taxNumList);
                }
            }
            //修改原订单状态
            orderManager.lambdaUpdate()
                    .set(Order::getStatus, DictConstants.ORDER_STATUS_REFUNDED)
                    .eq(Order::getOrderNo, order.getSourceOrderNo())
                    .update();
            //修改现订单
            orderManager.lambdaUpdate()
                    .set(Order::getRemark, req.getRemark())
                    .eq(Order::getOrderNo, order.getOrderNo())
                    .update();
            //修改退款信息
            refundOrderInfoManager.lambdaUpdate()
                    .set(RefundOrderInfo::getIsCancelTask, GlobalConstant.Number.ONE_INT)
                    .set(RefundOrderInfo::getIsDeleteCompany, GlobalConstant.Number.ONE_INT)
                    .set(RefundOrderInfo::getIsCancelAccount, GlobalConstant.Number.ONE_INT)
                    .set(RefundOrderInfo::getStatus, DictConstants.ORDER_REFUND_STATUS_PASS)
                    .eq(RefundOrderInfo::getOrderNo, order.getOrderNo())
                    .update();

        }else{
            //修改原订单状态
            orderManager.lambdaUpdate()
                    .set(Order::getStatus, DictConstants.ORDER_STATUS_PAID)
                    .eq(Order::getOrderNo, order.getSourceOrderNo())
                    .update();
            //修改现订单
            orderManager.lambdaUpdate()
                    .set(Order::getStatus, DictConstants.ORDER_STATUS_CANCELLED)
                    .set(Order::getRemark, req.getRemark())
                    .eq(Order::getOrderNo, order.getOrderNo())
                    .update();

            //修改退款信息
            refundOrderInfoManager.lambdaUpdate()
                    .set(RefundOrderInfo::getStatus, DictConstants.ORDER_REFUND_STATUS_REJECT)
                    .eq(RefundOrderInfo::getOrderNo, order.getOrderNo())
                    .update();
        }
        return Boolean.TRUE;
    }

    /**
     * 删除任务
     * @param order
     * @param orderDetailIdList
     */
    @Transactional(rollbackFor = Exception.class)
    public void delCommonTask(Order order, List<Long> orderDetailIdList){
        List<CommonTask> commonTaskList = commonTaskManager.lambdaQuery()
                .eq(CommonTask::getSourceNumber, order.getSourceOrderNo())
                .in(CommonTask::getSourceId, orderDetailIdList)
                .list();
        if(CollUtil.isNotEmpty(commonTaskList)) {
            List<Long> taskIdList = commonTaskList.stream().map(CommonTask::getId).toList();
            commonTaskManager.lambdaUpdate()
                    .set(CommonTask::getDeleted, GlobalConstant.Number.ONE_INT)
                    .in(CommonTask::getId, taskIdList)
                    .update();

            commonTaskDetailManager.lambdaUpdate()
                    .set(CommonTaskDetail::getDeleted, GlobalConstant.Number.ONE_INT)
                    .in(CommonTaskDetail::getTaskId, taskIdList)
                    .update();
        }
    }

    /**
     * 删除公司服务（通过合同间接关联：contract_detail.order_detail_id in orderDetailIdList → contract_id → company_service.source_id）
     * @param order
     * @param orderDetailIdList
     */
    @Transactional(rollbackFor = Exception.class)
    public void delCompanyService(Order order, List<Long> orderDetailIdList){
        if (CollUtil.isEmpty(orderDetailIdList)) {
            return;
        }
        List<ContractDetail> contractDetails = contractDetailManager.lambdaQuery()
                .in(ContractDetail::getOrderDetailId, orderDetailIdList)
                .list();
        if (CollUtil.isEmpty(contractDetails)) {
            return;
        }
        Set<Long> contractIds = contractDetails.stream().map(ContractDetail::getContractId).filter(Objects::nonNull).collect(Collectors.toSet());
        List<CompanyService> companyServiceList = companyServiceManager.lambdaQuery()
                .in(CompanyService::getSourceId, contractIds)
                .list();
        if (CollUtil.isNotEmpty(companyServiceList)) {
            List<Long> companyServiceIdList = companyServiceList.stream().map(CompanyService::getId).toList();
            companyServiceManager.lambdaUpdate()
                    .set(CompanyService::getDeleted, GlobalConstant.Number.ONE_INT)
                    .in(CompanyService::getId, companyServiceIdList)
                    .update();

            companyServiceRenewManager.lambdaUpdate()
                    .set(CompanyServiceRenew::getDeleted, GlobalConstant.Number.ONE_INT)
                    .in(CompanyServiceRenew::getCompanyServiceId, companyServiceIdList)
                    .update();
        }
    }

    /**
     * 强制终止合同， 暂时先不加入流程，由管家自行处理
     * @param contractIds
     */
    @Transactional(rollbackFor = Exception.class)
    public void terminatedContract(Order order, Set<Long> contractIds){
        if(CollUtil.isEmpty(contractIds)){
            return;
        }
        contractIds.forEach(item -> {
            Contract contract = contractManager.lambdaQuery()
                    .eq(Contract::getId, item)
                    .one();
            StringBuffer sb = new StringBuffer();
            if(StringUtils.isNotEmpty(contract.getRemark())){
                sb.append(contract.getRemark()).append(GlobalConstant.Symbol.COMMA);

            }
            sb.append(order.getOrderNo()).append("退款，强制终止合同。");
            contractManager.lambdaUpdate()
                    .set(Contract::getStatus, DictConstants.CONTRACT_STATUS_TERMINATED)
                    .set(Contract::getUpdateTime, new Date())
                    .eq(Contract::getId, item)
                    .update();
        });
    }

    /**
     * 取消小程序授权（退款级联）。
     * 1) 主表把 ids 置 0
     * 2) 走 broadcaster 在家族同税号范围内降级广播；守门规则：家族其它 CC 上仍有 PAID NORMAL 订单时不广播
     */
    public void cancelVerityTax(List<Long> companyIdList){
        if (CollUtil.isEmpty(companyIdList)) return;
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getIsVerifyTax, GlobalConstant.Number.ZERO_INT)
                .set(CustomerCompany::getUpdateTime, new Date())
                .in(CustomerCompany::getId, companyIdList)
                .update();
        for (Long id : companyIdList) {
            try {
                customerCompanyBroadcaster.broadcastSingleField(id, "isVerifyTax", GlobalConstant.Number.ZERO_INT, "refund");
            } catch (Exception e) {
                // 广播失败不影响主流程
            }
        }
    }

    /**
     * 账套停用
     * @param creditCodeList
     * @return
     */
    public void deactivatedAccountSets(List<String> creditCodeList){
        FinancialUtils.deactivatedAccountSets(creditCodeList);
    }

    /**
     * 退款转账
     * @param req
     * @param file
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean refundTransfer(RefundTransferReq req, MultipartFile file) {
        if(StringUtils.isEmpty(req.getOrderNo())){
            return Boolean.FALSE;
        }
        String saveFileName = null;
        if(ObjectUtils.isNotEmpty(file)){
            String originalFileName = file.getOriginalFilename();
            //文件后缀
            String fileSuffix = FileNameUtil.getSuffix(originalFileName);
            // 获取文件保存路径
            String savePath = FileUtils.getSavePath(FileConstants.FILE_TYPE_ORDER_REFUND);
            // 获取文件保存名
            saveFileName = "Transfer" + System.currentTimeMillis() + "." + fileSuffix;
            //文件上传至服务器
            FileUtils.uploadFile(file, savePath, saveFileName);
        }
        //修改订单数据
        orderManager.lambdaUpdate()
                .set(Order::getPayTime, req.getPayTime())
                .set(Order::getPaidAmount, req.getPaidAmount())
                .set(Order::getPaymentType, DictConstants.PAYMENT_TYPE_OTHER)
                .set(Order::getStatus, DictConstants.ORDER_STATUS_PAID)
                .eq(Order::getOrderNo, req.getOrderNo())
                .update();
        //修改退款信息
        refundOrderInfoManager.lambdaUpdate()
                .set(RefundOrderInfo::getIsTransfer, GlobalConstant.Number.ONE_INT)
                .set(StringUtils.isNotEmpty(saveFileName), RefundOrderInfo::getTransferImg, FileConstants.FILE_GET_ORDER_REFUND + saveFileName)
                .eq(RefundOrderInfo::getOrderNo, req.getOrderNo())
                .update();

        return Boolean.TRUE;
    }

    /**
     * 取消办税
     * @param req
     * @param file
     * @return
     */
    public Boolean refundCancelTax(RefundCancelTaxReq req, MultipartFile file) {
        if(StringUtils.isEmpty(req.getOrderNo())){
            return Boolean.FALSE;
        }
        String originalFileName = file.getOriginalFilename();
        //文件后缀
        String fileSuffix = FileNameUtil.getSuffix(originalFileName);
        // 获取文件保存路径
        String savePath = FileUtils.getSavePath(FileConstants.FILE_TYPE_ORDER_REFUND);
        // 获取文件保存名
        String saveFileName = "CancelTax" + System.currentTimeMillis()+"."+fileSuffix;
        //文件上传至服务器
        FileUtils.uploadFile(file, savePath, saveFileName);
        //修改退款信息
        refundOrderInfoManager.lambdaUpdate()
                .set(RefundOrderInfo::getIsCancelTax, GlobalConstant.Number.ONE_INT)
                .set(RefundOrderInfo::getCancelTaxImg, FileConstants.FILE_GET_ORDER_REFUND + saveFileName)
                .eq(RefundOrderInfo::getOrderNo, req.getOrderNo())
                .update();
        return Boolean.TRUE;
    }
}
