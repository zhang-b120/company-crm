package com.qykj.finmaster.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.impl.CustomerInfoManagerImpl;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.manager.impl.LeadManagerImpl;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.order.domain.OrderDetail;
import com.qykj.finmaster.order.domain.req.QuickOrderReq;
import com.qykj.finmaster.order.domain.resp.QuickOrderResp;
import com.qykj.finmaster.order.manager.impl.OrderDetailManagerImpl;
import com.qykj.finmaster.order.manager.impl.OrderManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class QuickOrderService {

    private static final AtomicLong ORDER_SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    @Autowired
    private CustomerInfoManagerImpl customerInfoManager;

    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;

    @Autowired
    private LeadManagerImpl leadManager;

    @Autowired
    private OrderManagerImpl orderManager;

    @Autowired
    private OrderDetailManagerImpl orderDetailManager;

    @Transactional(rollbackFor = Exception.class)
    public QuickOrderResp quickCreate(QuickOrderReq req, Long tenantId, LoginUser loginUser) {
        // Step 1: 解析/创建客户
        Long customerId = resolveCustomer(req, tenantId, loginUser);

        // Step 2: 查找或创建公司
        Long companyId = resolveCompany(req.getCompanyName(), customerId, tenantId, loginUser);

        // Step 3: 创建订单
        Long orderId = createOrder(req, customerId, companyId, tenantId, loginUser);

        return new QuickOrderResp(orderId, customerId, companyId);
    }

    // ─── 客户解析 ────────────────────────────────────────────────────────────

    private Long resolveCustomer(QuickOrderReq req, Long tenantId, LoginUser loginUser) {
        if (req.getCustomerId() != null) {
            return req.getCustomerId();
        }
        if (req.getLeadId() != null) {
            return convertLeadToCustomer(req, tenantId, loginUser);
        }
        return createNewCustomer(req, tenantId, loginUser);
    }

    private Long convertLeadToCustomer(QuickOrderReq req, Long tenantId, LoginUser loginUser) {
        Lead lead = leadManager.getById(req.getLeadId());
        if (lead == null) {
            throw new RuntimeException("线索不存在");
        }
        CustomerInfo customer = new CustomerInfo();
        customer.setCustomerName(req.getCompanyName());
        customer.setContactName(lead.getContactName());
        customer.setPhone(lead.getPhone());
        customer.setSource(lead.getSource());
        customer.setLeadId(lead.getId());
        customer.setLeadName(lead.getLeadName());
        customer.setCustomerType(1);
        customer.setStatus(2); // 跟进中
        customer.setTenantId(tenantId);
        customer.setCreateBy(loginUser.getUsername());
        customer.setCreateByName(loginUser.getRealName());
        customer.setOwnerUser(loginUser.getUsername());
        customer.setOwnerUserName(loginUser.getRealName());
        customerInfoManager.save(customer);
        // 更新线索状态为已转化
        lead.setStatus(3);
        leadManager.updateById(lead);
        return customer.getId();
    }

    private Long createNewCustomer(QuickOrderReq req, Long tenantId, LoginUser loginUser) {
        CustomerInfo customer = new CustomerInfo();
        customer.setCustomerName(req.getCompanyName());
        customer.setContactName(req.getContactName());
        customer.setPhone(req.getPhone());
        customer.setCustomerType(1);
        customer.setStatus(2); // 跟进中
        customer.setTenantId(tenantId);
        customer.setCreateBy(loginUser.getUsername());
        customer.setCreateByName(loginUser.getRealName());
        customer.setOwnerUser(loginUser.getUsername());
        customer.setOwnerUserName(loginUser.getRealName());
        customerInfoManager.save(customer);
        return customer.getId();
    }

    // ─── 公司解析 ────────────────────────────────────────────────────────────

    private Long resolveCompany(String companyName, Long customerId, Long tenantId, LoginUser loginUser) {
        // 先查是否已存在同名公司
        CustomerCompany existing = customerCompanyManager.getOne(
                new LambdaQueryWrapper<CustomerCompany>()
                        .eq(CustomerCompany::getCustomerId, customerId)
                        .eq(CustomerCompany::getCompanyName, companyName)
                        .eq(CustomerCompany::getTenantId, tenantId)
                        .last("LIMIT 1")
        );
        if (existing != null) {
            return existing.getId();
        }
        CustomerCompany company = new CustomerCompany();
        company.setCustomerId(customerId);
        company.setCompanyName(companyName);
        company.setTenantId(tenantId);
        company.setEnabled(1);
        company.setCreateBy(loginUser.getUsername());
        company.setCreateByName(loginUser.getRealName());
        customerCompanyManager.save(company);
        return company.getId();
    }

    // ─── 订单创建 ────────────────────────────────────────────────────────────

    private Long createOrder(QuickOrderReq req, Long customerId, Long companyId,
                             Long tenantId, LoginUser loginUser) {
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setCustomerId(customerId);
        order.setCustomerName(req.getCompanyName());
        order.setTotalAmount(req.getTotalAmount());
        order.setFinalAmount(req.getFinalAmount());
        order.setTaxAmount(req.getTaxAmount());
        order.setAfterTaxAmount(req.getAfterTaxAmount());
        order.setPaidAmount(req.getPaidAmount());
        order.setDiscount(req.getDiscount());
        order.setStatus(req.getStatus() != null ? req.getStatus() : "PENDING");
        order.setPaymentType(req.getPaymentType());
        order.setOrderType("NORMAL");
        order.setRemark(req.getRemark());
        order.setTenantId(tenantId);
        order.setCreateBy(loginUser.getUsername());
        order.setCreateByName(loginUser.getRealName());
        order.setCreateTime(new Date());
        orderManager.save(order);

        // 订单明细
        if (ObjectUtils.isNotEmpty(req.getOrderDetails())) {
            List<OrderDetail> details = req.getOrderDetails().stream().map(d -> {
                OrderDetail detail = CopierCopyUtils.convertTo(d, OrderDetail.class);
                detail.setId(null);
                detail.setOrderId(order.getId());
                detail.setCompanyId(companyId);
                detail.setTenantId(tenantId);
                detail.setCreateBy(loginUser.getUsername());
                detail.setCreateByName(loginUser.getRealName());
                return detail;
            }).collect(Collectors.toList());
            orderDetailManager.saveBatch(details);
        }

        return order.getId();
    }

    private String generateOrderNo() {
        String date = new SimpleDateFormat("yyyyMMdd").format(new Date());
        long seq = ORDER_SEQ.incrementAndGet() % 100000;
        return "ORD" + date + String.format("%05d", seq);
    }
}
