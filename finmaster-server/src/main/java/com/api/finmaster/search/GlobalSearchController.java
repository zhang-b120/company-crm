package com.qykj.finmaster.search;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.contract.domain.Contract;
import com.qykj.finmaster.contract.manager.impl.ContractManagerImpl;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.impl.CustomerInfoManagerImpl;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.manager.impl.LeadManagerImpl;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.order.manager.impl.OrderManagerImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

/**
 * 全局搜索（Sprint 4 H8）
 *
 * 用户在 Header 输入框 Ctrl+K 搜索任意关键词，跨 4 类实体返回匹配结果
 * 字段策略：
 *   线索 Lead：leadName / contactName / phone / wechat / companyName 模糊
 *   客户 CustomerInfo：customerName / contactName / phone
 *   订单 Order：orderNo / customerName
 *   合同 Contract：contractNo / companyName
 * 每类返回 top 10
 */
@RestController
@RequestMapping("/search")
public class GlobalSearchController extends BaseController {

    @Autowired private LeadManagerImpl leadManager;
    @Autowired private CustomerInfoManagerImpl customerInfoManager;
    @Autowired private OrderManagerImpl orderManager;
    @Autowired private ContractManagerImpl contractManager;

    private static final int LIMIT_PER_TYPE = 10;

    @PreAuthorize("@ss.hasPermi('customer:lead:list')")
    @GetMapping("/global")
    public AjaxResult globalSearch(@RequestParam String q,
                                   @RequestParam(required = false) String types) {
        if (StringUtils.isBlank(q) || q.trim().length() < 1) {
            return AjaxResult.success(emptyResult());
        }
        String kw = q.trim();
        Set<String> typeSet = parseTypes(types);
        Map<String, Object> result = new LinkedHashMap<>();

        if (typeSet.contains("lead")) {
            List<Lead> leads = leadManager.lambdaQuery()
                    .and(qw -> qw
                            .like(Lead::getLeadName, kw)
                            .or().like(Lead::getContactName, kw)
                            .or().like(Lead::getPhone, kw)
                            .or().like(Lead::getWechat, kw)
                            .or().like(Lead::getCompanyName, kw))
                    .orderByDesc(Lead::getUpdateTime)
                    .last("LIMIT " + LIMIT_PER_TYPE)
                    .list();
            result.put("leads", leads.stream().map(this::leadToBrief).toList());
        }

        if (typeSet.contains("customer")) {
            List<CustomerInfo> customers = customerInfoManager.lambdaQuery()
                    .and(qw -> qw
                            .like(CustomerInfo::getCustomerName, kw)
                            .or().like(CustomerInfo::getContactName, kw)
                            .or().like(CustomerInfo::getPhone, kw))
                    .orderByDesc(CustomerInfo::getUpdateTime)
                    .last("LIMIT " + LIMIT_PER_TYPE)
                    .list();
            result.put("customers", customers.stream().map(this::customerToBrief).toList());
        }

        if (typeSet.contains("order")) {
            List<Order> orders = orderManager.lambdaQuery()
                    .and(qw -> qw
                            .like(Order::getOrderNo, kw)
                            .or().like(Order::getCustomerName, kw))
                    .orderByDesc(Order::getCreateTime)
                    .last("LIMIT " + LIMIT_PER_TYPE)
                    .list();
            result.put("orders", orders.stream().map(this::orderToBrief).toList());
        }

        if (typeSet.contains("contract")) {
            List<Contract> contracts = contractManager.lambdaQuery()
                    .like(Contract::getContractNo, kw)
                    .orderByDesc(Contract::getCreateTime)
                    .last("LIMIT " + LIMIT_PER_TYPE)
                    .list();
            result.put("contracts", contracts.stream().map(this::contractToBrief).toList());
        }

        result.put("query", kw);
        return AjaxResult.success(result);
    }

    private Set<String> parseTypes(String types) {
        if (StringUtils.isBlank(types)) {
            return new HashSet<>(Arrays.asList("lead", "customer", "order", "contract"));
        }
        Set<String> set = new HashSet<>();
        for (String t : types.split(",")) {
            String tr = t.trim();
            if (!tr.isEmpty()) set.add(tr);
        }
        return set;
    }

    private Map<String, Object> leadToBrief(Lead l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "lead");
        m.put("id", l.getId());
        m.put("title", l.getLeadName());
        m.put("subtitle", String.join(" · ",
                nullSafe(l.getContactName()),
                nullSafe(l.getPhone()),
                nullSafe(l.getCompanyName())));
        m.put("status", l.getStatus());
        m.put("link", "/admin/customer/lead?searchLeadName=" + nullSafe(l.getLeadName()));
        return m;
    }

    private Map<String, Object> customerToBrief(CustomerInfo c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "customer");
        m.put("id", c.getId());
        m.put("title", c.getCustomerName());
        m.put("subtitle", String.join(" · ",
                nullSafe(c.getContactName()),
                nullSafe(c.getPhone())));
        m.put("status", c.getStatus());
        // 私海/公海路径自动选择
        m.put("link", c.getCustomerType() != null && c.getCustomerType() == 2
                ? "/admin/customer/public-sea"
                : "/admin/customer/private-sea");
        return m;
    }

    private Map<String, Object> orderToBrief(Order o) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "order");
        m.put("id", o.getId());
        m.put("title", o.getOrderNo());
        m.put("subtitle", nullSafe(o.getCustomerName()) +
                " · " + nullSafe(o.getStatus()) +
                " · ¥" + (o.getFinalAmount() != null ? o.getFinalAmount() : ""));
        m.put("link", "/order/list");
        return m;
    }

    private Map<String, Object> contractToBrief(Contract ct) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", "contract");
        m.put("id", ct.getId());
        m.put("title", ct.getContractNo());
        m.put("subtitle", nullSafe(ct.getContractType()) + " · " + nullSafe(ct.getStatus()));
        m.put("link", "/order/contract");
        return m;
    }

    private static String nullSafe(Object s) {
        return s == null ? "" : String.valueOf(s);
    }

    private Map<String, Object> emptyResult() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("leads", Collections.emptyList());
        r.put("customers", Collections.emptyList());
        r.put("orders", Collections.emptyList());
        r.put("contracts", Collections.emptyList());
        return r;
    }
}
