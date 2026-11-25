package com.qykj.finmaster.oa.expense.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.oa.approval.service.ApprovalServiceImpl;
import com.qykj.finmaster.oa.expense.domain.OaExpense;
import com.qykj.finmaster.oa.expense.domain.OaExpenseItem;
import com.qykj.finmaster.oa.expense.domain.req.AddExpenseReq;
import com.qykj.finmaster.oa.expense.domain.req.ExpenseItemReq;
import com.qykj.finmaster.oa.expense.domain.req.GetExpenseListReq;
import com.qykj.finmaster.oa.expense.manager.impl.OaExpenseManagerImpl;
import com.qykj.finmaster.oa.expense.manager.impl.OaExpenseItemManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报销服务实现类
 */
@Slf4j
@Service
public class ExpenseServiceImpl {

    @Autowired
    private OaExpenseManagerImpl expenseManager;

    @Autowired
    private OaExpenseItemManagerImpl expenseItemManager;

    @Autowired
    private ApprovalServiceImpl approvalService;

    /**
     * 新增报销申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addExpense(AddExpenseReq req, Long tenantId, LoginUser loginUser) {
        // 生成报销单号
        String expenseNo = generateExpenseNo(tenantId);

        // 创建报销申请
        OaExpense expense = new OaExpense();
        expense.setTenantId(tenantId);
        expense.setExpenseNo(expenseNo);
        expense.setApplicant(loginUser.getUsername());
        expense.setApplicantName(loginUser.getRealName());
        expense.setTotalAmount(req.getTotalAmount());
        expense.setAttachments(req.getAttachments());
        expense.setStatus("DRAFT");
        expense.setCreateBy(loginUser.getUsername());
        expense.setCreateByName(loginUser.getRealName());
        expense.setCreateTime(new Date());
        expenseManager.save(expense);

        // 创建报销明细
        if (req.getItems() != null && !req.getItems().isEmpty()) {
            List<OaExpenseItem> items = new ArrayList<>();
            for (ExpenseItemReq itemReq : req.getItems()) {
                OaExpenseItem item = new OaExpenseItem();
                item.setExpenseId(expense.getId());
                item.setExpenseType(itemReq.getExpenseType());
                item.setAmount(itemReq.getAmount());
                if (itemReq.getExpenseDate() != null) {
                    item.setExpenseDate(itemReq.getExpenseDate().toInstant()
                            .atZone(ZoneId.systemDefault()).toLocalDate());
                }
                item.setDescription(itemReq.getDescription());
                items.add(item);
            }
            expenseItemManager.saveBatch(items);
        }

        log.info("新增报销申请成功，ID: {}，单号: {}，申请人: {}", expense.getId(), expenseNo, loginUser.getUsername());
        return expense.getId();
    }

    /**
     * 提交报销申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submitExpense(Long id, Long tenantId, LoginUser loginUser) {
        OaExpense expense = expenseManager.getById(id);
        if (expense == null) {
            throw new BusinessException("报销申请不存在");
        }
        if (!"DRAFT".equals(expense.getStatus())) {
            throw new BusinessException("只有草稿状态的申请才能提交");
        }

        // 更新状态为PENDING
        expense.setStatus("PENDING");
        expense.setUpdateBy(loginUser.getUsername());
        expense.setUpdateByName(loginUser.getRealName());
        expense.setUpdateTime(new Date());

        // 提交审批
        String title = loginUser.getRealName() + "的报销申请（" + expense.getExpenseNo() + "）";
        Long instanceId = approvalService.submit("EXPENSE", expense.getId(), title, tenantId, loginUser);

        expense.setApprovalInstanceId(instanceId);
        expenseManager.updateById(expense);

        log.info("报销申请提交审批成功，报销ID: {}，审批实例ID: {}", id, instanceId);
        return instanceId;
    }

    /**
     * 更新报销状态（由审批引擎回调）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateExpenseStatus(Long expenseId, String status) {
        OaExpense expense = expenseManager.getById(expenseId);
        if (expense == null) {
            throw new BusinessException("报销申请不存在");
        }
        expense.setStatus(status);
        expense.setUpdateTime(new Date());
        expenseManager.updateById(expense);

        log.info("报销申请状态更新，ID: {}，新状态: {}", expenseId, status);
    }

    /**
     * 查询报销列表
     */
    public List<OaExpense> getList(GetExpenseListReq req, Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<OaExpense> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaExpense::getTenantId, tenantId);

        // 非管理员只能看自己的
        if (!loginUser.isAdmin()) {
            wrapper.eq(OaExpense::getApplicant, loginUser.getUsername());
        } else if (StringUtils.isNotEmpty(req.getApplicant())) {
            wrapper.eq(OaExpense::getApplicant, req.getApplicant());
        }

        if (StringUtils.isNotEmpty(req.getStatus())) {
            wrapper.eq(OaExpense::getStatus, req.getStatus());
        }

        wrapper.orderByDesc(OaExpense::getCreateTime);
        return expenseManager.list(wrapper);
    }

    /**
     * 获取报销详情（含明细）
     */
    public Map<String, Object> getDetail(Long id) {
        OaExpense expense = expenseManager.getById(id);
        if (expense == null) {
            throw new BusinessException("报销申请不存在");
        }

        LambdaQueryWrapper<OaExpenseItem> itemWrapper = Wrappers.lambdaQuery();
        itemWrapper.eq(OaExpenseItem::getExpenseId, id);
        itemWrapper.orderByAsc(OaExpenseItem::getId);
        List<OaExpenseItem> items = expenseItemManager.list(itemWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("expense", expense);
        result.put("items", items);
        return result;
    }

    /**
     * 确认打款（APPROVED → PAID）
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmPaid(Long id, LoginUser loginUser) {
        OaExpense expense = expenseManager.getById(id);
        if (expense == null) {
            throw new BusinessException("报销申请不存在");
        }
        if (!"APPROVED".equals(expense.getStatus())) {
            throw new BusinessException("只有审批通过的申请才能确认打款");
        }
        expense.setStatus("PAID");
        expense.setUpdateBy(loginUser.getUsername());
        expense.setUpdateByName(loginUser.getRealName());
        expense.setUpdateTime(new Date());
        expenseManager.updateById(expense);

        log.info("报销申请确认打款，ID: {}，操作人: {}", id, loginUser.getUsername());
    }

    /**
     * 批量确认打款（只处理 APPROVED 状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchConfirmPaid(List<Long> ids, LoginUser loginUser) {
        int success = 0, skipped = 0;
        for (Long id : ids) {
            try {
                OaExpense expense = expenseManager.getById(id);
                if (expense == null || !"APPROVED".equals(expense.getStatus())) {
                    skipped++;
                    continue;
                }
                expense.setStatus("PAID");
                expense.setUpdateBy(loginUser.getUsername());
                expense.setUpdateByName(loginUser.getRealName());
                expense.setUpdateTime(new Date());
                expenseManager.updateById(expense);
                success++;
            } catch (Exception e) {
                log.error("批量打款失败，id: {}", id, e);
                skipped++;
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("skipped", skipped);
        return result;
    }

    /**
     * 生成报销单号：EXP + yyyyMMdd + 3位序号
     */
    private String generateExpenseNo(Long tenantId) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "EXP" + dateStr;

        // 查询当天最大序号
        LambdaQueryWrapper<OaExpense> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaExpense::getTenantId, tenantId);
        wrapper.likeRight(OaExpense::getExpenseNo, prefix);
        wrapper.orderByDesc(OaExpense::getExpenseNo);
        wrapper.last("LIMIT 1");
        OaExpense lastExpense = expenseManager.getOne(wrapper);

        int sequence = 1;
        if (lastExpense != null && lastExpense.getExpenseNo() != null) {
            String lastNo = lastExpense.getExpenseNo();
            String seqStr = lastNo.substring(prefix.length());
            try {
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }

        return prefix + String.format("%03d", sequence);
    }
}
