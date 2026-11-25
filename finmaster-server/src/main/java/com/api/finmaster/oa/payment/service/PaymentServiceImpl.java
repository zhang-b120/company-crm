package com.qykj.finmaster.oa.payment.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.oa.approval.service.ApprovalServiceImpl;
import com.qykj.finmaster.oa.payment.domain.OaPayment;
import com.qykj.finmaster.oa.payment.domain.req.AddPaymentReq;
import com.qykj.finmaster.oa.payment.domain.req.GetPaymentListReq;
import com.qykj.finmaster.oa.payment.manager.impl.OaPaymentManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 付款服务实现类
 */
@Slf4j
@Service("oaPaymentServiceImpl")
public class PaymentServiceImpl {

    @Autowired
    private OaPaymentManagerImpl paymentManager;

    @Autowired
    private ApprovalServiceImpl approvalService;

    /**
     * 新增付款申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addPayment(AddPaymentReq req, Long tenantId, LoginUser loginUser) {
        // 生成付款单号
        String paymentNo = generatePaymentNo(tenantId);

        OaPayment payment = new OaPayment();
        payment.setTenantId(tenantId);
        payment.setPaymentNo(paymentNo);
        payment.setApplicant(loginUser.getUsername());
        payment.setApplicantName(loginUser.getRealName());
        payment.setPayeeName(req.getPayeeName());
        payment.setPayeeBank(req.getPayeeBank());
        payment.setPayeeAccount(req.getPayeeAccount());
        payment.setAmount(req.getAmount());
        payment.setPurpose(req.getPurpose());
        payment.setAttachments(req.getAttachments());
        payment.setStatus("DRAFT");
        payment.setCreateBy(loginUser.getUsername());
        payment.setCreateByName(loginUser.getRealName());
        payment.setCreateTime(new Date());
        paymentManager.save(payment);

        log.info("新增付款申请成功，ID: {}，单号: {}，申请人: {}", payment.getId(), paymentNo, loginUser.getUsername());
        return payment.getId();
    }

    /**
     * 提交付款申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submitPayment(Long id, Long tenantId, LoginUser loginUser) {
        OaPayment payment = paymentManager.getById(id);
        if (payment == null) {
            throw new BusinessException("付款申请不存在");
        }
        if (!"DRAFT".equals(payment.getStatus())) {
            throw new BusinessException("只有草稿状态的申请才能提交");
        }

        // 更新状态为PENDING
        payment.setStatus("PENDING");
        payment.setUpdateBy(loginUser.getUsername());
        payment.setUpdateByName(loginUser.getRealName());
        payment.setUpdateTime(new Date());

        // 提交审批
        String title = loginUser.getRealName() + "的付款申请（" + payment.getPaymentNo() + "）";
        Long instanceId = approvalService.submit("PAYMENT", payment.getId(), title, tenantId, loginUser);

        payment.setApprovalInstanceId(instanceId);
        paymentManager.updateById(payment);

        log.info("付款申请提交审批成功，付款ID: {}，审批实例ID: {}", id, instanceId);
        return instanceId;
    }

    /**
     * 更新付款状态（由审批引擎回调）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePaymentStatus(Long paymentId, String status) {
        OaPayment payment = paymentManager.getById(paymentId);
        if (payment == null) {
            throw new BusinessException("付款申请不存在");
        }
        payment.setStatus(status);
        payment.setUpdateTime(new Date());
        paymentManager.updateById(payment);

        log.info("付款申请状态更新，ID: {}，新状态: {}", paymentId, status);
    }

    /**
     * 查询付款列表
     */
    public List<OaPayment> getList(GetPaymentListReq req, Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<OaPayment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaPayment::getTenantId, tenantId);

        // 非管理员只能看自己的
        if (!loginUser.isAdmin()) {
            wrapper.eq(OaPayment::getApplicant, loginUser.getUsername());
        } else if (StringUtils.isNotEmpty(req.getApplicant())) {
            wrapper.eq(OaPayment::getApplicant, req.getApplicant());
        }

        if (StringUtils.isNotEmpty(req.getStatus())) {
            wrapper.eq(OaPayment::getStatus, req.getStatus());
        }

        wrapper.orderByDesc(OaPayment::getCreateTime);
        return paymentManager.list(wrapper);
    }

    /**
     * 获取付款详情
     */
    public OaPayment getDetail(Long id) {
        OaPayment payment = paymentManager.getById(id);
        if (payment == null) {
            throw new BusinessException("付款申请不存在");
        }
        return payment;
    }

    /**
     * 确认打款（APPROVED → PAID）
     */
    @Transactional(rollbackFor = Exception.class)
    public void confirmPaid(Long id, LoginUser loginUser) {
        OaPayment payment = paymentManager.getById(id);
        if (payment == null) {
            throw new BusinessException("付款申请不存在");
        }
        if (!"APPROVED".equals(payment.getStatus())) {
            throw new BusinessException("只有审批通过的申请才能确认打款");
        }
        payment.setStatus("PAID");
        payment.setUpdateBy(loginUser.getUsername());
        payment.setUpdateByName(loginUser.getRealName());
        payment.setUpdateTime(new Date());
        paymentManager.updateById(payment);

        log.info("付款申请确认打款，ID: {}，操作人: {}", id, loginUser.getUsername());
    }

    /**
     * 批量确认打款（只处理 APPROVED 状态）
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> batchConfirmPaid(List<Long> ids, LoginUser loginUser) {
        int success = 0, skipped = 0;
        for (Long id : ids) {
            try {
                OaPayment payment = paymentManager.getById(id);
                if (payment == null || !"APPROVED".equals(payment.getStatus())) {
                    skipped++;
                    continue;
                }
                payment.setStatus("PAID");
                payment.setUpdateBy(loginUser.getUsername());
                payment.setUpdateByName(loginUser.getRealName());
                payment.setUpdateTime(new Date());
                paymentManager.updateById(payment);
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
     * 生成付款单号：PAY + yyyyMMdd + 3位序号
     */
    private String generatePaymentNo(Long tenantId) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "PAY" + dateStr;

        // 查询当天最大序号
        LambdaQueryWrapper<OaPayment> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaPayment::getTenantId, tenantId);
        wrapper.likeRight(OaPayment::getPaymentNo, prefix);
        wrapper.orderByDesc(OaPayment::getPaymentNo);
        wrapper.last("LIMIT 1");
        OaPayment lastPayment = paymentManager.getOne(wrapper);

        int sequence = 1;
        if (lastPayment != null && lastPayment.getPaymentNo() != null) {
            String lastNo = lastPayment.getPaymentNo();
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
