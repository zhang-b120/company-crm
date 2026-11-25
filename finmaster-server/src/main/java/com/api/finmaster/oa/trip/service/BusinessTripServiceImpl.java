package com.qykj.finmaster.oa.trip.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.oa.approval.service.ApprovalServiceImpl;
import com.qykj.finmaster.oa.trip.domain.OaBusinessTrip;
import com.qykj.finmaster.oa.trip.domain.OaBusinessTripLeg;
import com.qykj.finmaster.oa.trip.domain.req.AddBusinessTripReq;
import com.qykj.finmaster.oa.trip.domain.req.GetBusinessTripListReq;
import com.qykj.finmaster.oa.trip.domain.req.TripLegReq;
import com.qykj.finmaster.oa.trip.manager.impl.OaBusinessTripManagerImpl;
import com.qykj.finmaster.oa.trip.manager.impl.OaBusinessTripLegManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 出差服务实现类
 */
@Slf4j
@Service
public class BusinessTripServiceImpl {

    @Autowired
    private OaBusinessTripManagerImpl tripManager;

    @Autowired
    private OaBusinessTripLegManagerImpl tripLegManager;

    @Autowired
    private ApprovalServiceImpl approvalService;

    /**
     * 新增出差申请
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addTrip(AddBusinessTripReq req, Long tenantId, LoginUser loginUser) {
        // 生成出差单号
        String tripNo = generateTripNo(tenantId);

        // 创建出差申请
        OaBusinessTrip trip = new OaBusinessTrip();
        trip.setTenantId(tenantId);
        trip.setTripNo(tripNo);
        trip.setApplicant(loginUser.getUsername());
        trip.setApplicantName(loginUser.getRealName());
        trip.setDestination(req.getDestination());
        trip.setStartDate(parseDate(req.getStartDate(), "yyyy-MM-dd"));
        trip.setEndDate(parseDate(req.getEndDate(), "yyyy-MM-dd"));
        trip.setDurationDays(req.getDurationDays());
        trip.setPurpose(req.getPurpose());
        trip.setTransportation(req.getTransportation());
        trip.setEstimatedCost(req.getEstimatedCost());
        trip.setStatus("DRAFT");
        trip.setCreateBy(loginUser.getUsername());
        trip.setCreateByName(loginUser.getRealName());
        trip.setCreateTime(new Date());
        tripManager.save(trip);

        // 创建行程段
        saveLegs(trip.getId(), req.getLegs());

        log.info("新增出差申请成功，ID: {}，单号: {}，申请人: {}", trip.getId(), tripNo, loginUser.getUsername());
        return trip.getId();
    }

    /**
     * 修改出差申请
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTrip(AddBusinessTripReq req, Long tenantId, LoginUser loginUser) {
        if (req.getId() == null) {
            throw new BusinessException("出差申请ID不能为空");
        }
        OaBusinessTrip trip = tripManager.getById(req.getId());
        if (trip == null) {
            throw new BusinessException("出差申请不存在");
        }
        if (!"DRAFT".equals(trip.getStatus())) {
            throw new BusinessException("只有草稿状态的申请才能修改");
        }

        trip.setDestination(req.getDestination());
        trip.setStartDate(parseDate(req.getStartDate(), "yyyy-MM-dd"));
        trip.setEndDate(parseDate(req.getEndDate(), "yyyy-MM-dd"));
        trip.setDurationDays(req.getDurationDays());
        trip.setPurpose(req.getPurpose());
        trip.setTransportation(req.getTransportation());
        trip.setEstimatedCost(req.getEstimatedCost());
        trip.setUpdateBy(loginUser.getUsername());
        trip.setUpdateByName(loginUser.getRealName());
        trip.setUpdateTime(new Date());
        tripManager.updateById(trip);

        // 删除旧行程段，插入新行程段
        tripLegManager.remove(Wrappers.lambdaQuery(OaBusinessTripLeg.class)
                .eq(OaBusinessTripLeg::getTripId, trip.getId()));
        saveLegs(trip.getId(), req.getLegs());

        log.info("修改出差申请成功，ID: {}，操作人: {}", trip.getId(), loginUser.getUsername());
    }

    /**
     * 删除出差申请
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTrip(Long id) {
        OaBusinessTrip trip = tripManager.getById(id);
        if (trip == null) {
            throw new BusinessException("出差申请不存在");
        }
        if (!"DRAFT".equals(trip.getStatus())) {
            throw new BusinessException("只有草稿状态的申请才能删除");
        }

        tripManager.removeById(id);
        tripLegManager.remove(Wrappers.lambdaQuery(OaBusinessTripLeg.class)
                .eq(OaBusinessTripLeg::getTripId, id));

        log.info("删除出差申请成功，ID: {}", id);
    }

    /**
     * 提交出差申请（发起审批）
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submitTrip(Long id, Long tenantId, LoginUser loginUser) {
        OaBusinessTrip trip = tripManager.getById(id);
        if (trip == null) {
            throw new BusinessException("出差申请不存在");
        }
        if (!"DRAFT".equals(trip.getStatus())) {
            throw new BusinessException("只有草稿状态的申请才能提交");
        }

        // 更新状态为PENDING
        trip.setStatus("PENDING");
        trip.setUpdateBy(loginUser.getUsername());
        trip.setUpdateByName(loginUser.getRealName());
        trip.setUpdateTime(new Date());

        // 提交审批
        String title = loginUser.getRealName() + "的出差申请（" + trip.getTripNo() + "）";
        Long instanceId = approvalService.submit("TRIP", trip.getId(), title, tenantId, loginUser);

        trip.setApprovalInstanceId(instanceId);
        tripManager.updateById(trip);

        log.info("出差申请提交审批成功，出差ID: {}，审批实例ID: {}", id, instanceId);
        return instanceId;
    }

    /**
     * 更新出差状态（由审批引擎回调）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateTripStatus(Long tripId, String status) {
        OaBusinessTrip trip = tripManager.getById(tripId);
        if (trip == null) {
            throw new BusinessException("出差申请不存在");
        }
        trip.setStatus(status);
        trip.setUpdateTime(new Date());
        tripManager.updateById(trip);

        log.info("出差申请状态更新，ID: {}，新状态: {}", tripId, status);
    }

    /**
     * 查询出差列表
     */
    public List<OaBusinessTrip> getList(GetBusinessTripListReq req, Long tenantId, LoginUser loginUser) {
        LambdaQueryWrapper<OaBusinessTrip> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaBusinessTrip::getTenantId, tenantId);

        // 非管理员只能看自己的
        if (!loginUser.isAdmin()) {
            wrapper.eq(OaBusinessTrip::getApplicant, loginUser.getUsername());
        } else if (StringUtils.isNotEmpty(req.getApplicant())) {
            wrapper.eq(OaBusinessTrip::getApplicant, req.getApplicant());
        }

        if (StringUtils.isNotEmpty(req.getStatus())) {
            wrapper.eq(OaBusinessTrip::getStatus, req.getStatus());
        }

        wrapper.orderByDesc(OaBusinessTrip::getCreateTime);
        return tripManager.list(wrapper);
    }

    /**
     * 获取出差详情（含行程段）
     */
    public Map<String, Object> getDetail(Long id) {
        OaBusinessTrip trip = tripManager.getById(id);
        if (trip == null) {
            throw new BusinessException("出差申请不存在");
        }

        LambdaQueryWrapper<OaBusinessTripLeg> legWrapper = Wrappers.lambdaQuery();
        legWrapper.eq(OaBusinessTripLeg::getTripId, id);
        legWrapper.orderByAsc(OaBusinessTripLeg::getLegOrder);
        List<OaBusinessTripLeg> legs = tripLegManager.list(legWrapper);

        Map<String, Object> result = new HashMap<>();
        result.put("trip", trip);
        result.put("legs", legs);
        return result;
    }

    /**
     * 关联报销单
     */
    @Transactional(rollbackFor = Exception.class)
    public void linkExpense(Long tripId, Long expenseId, LoginUser loginUser) {
        OaBusinessTrip trip = tripManager.getById(tripId);
        if (trip == null) {
            throw new BusinessException("出差申请不存在");
        }
        trip.setLinkedExpenseId(expenseId);
        trip.setUpdateBy(loginUser.getUsername());
        trip.setUpdateByName(loginUser.getRealName());
        trip.setUpdateTime(new Date());
        tripManager.updateById(trip);

        log.info("出差申请关联报销单，出差ID: {}，报销ID: {}，操作人: {}", tripId, expenseId, loginUser.getUsername());
    }

    /**
     * 生成出差单号：BT + yyyyMMdd + 3位序号
     */
    private String generateTripNo(Long tenantId) {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String prefix = "BT" + dateStr;

        // 查询当天最大序号
        LambdaQueryWrapper<OaBusinessTrip> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(OaBusinessTrip::getTenantId, tenantId);
        wrapper.likeRight(OaBusinessTrip::getTripNo, prefix);
        wrapper.orderByDesc(OaBusinessTrip::getTripNo);
        wrapper.last("LIMIT 1");
        OaBusinessTrip lastTrip = tripManager.getOne(wrapper);

        int sequence = 1;
        if (lastTrip != null && lastTrip.getTripNo() != null) {
            String lastNo = lastTrip.getTripNo();
            String seqStr = lastNo.substring(prefix.length());
            try {
                sequence = Integer.parseInt(seqStr) + 1;
            } catch (NumberFormatException e) {
                sequence = 1;
            }
        }

        return prefix + String.format("%03d", sequence);
    }

    /**
     * 保存行程段列表
     */
    private void saveLegs(Long tripId, List<TripLegReq> legReqs) {
        if (legReqs != null && !legReqs.isEmpty()) {
            List<OaBusinessTripLeg> legs = new ArrayList<>();
            for (TripLegReq legReq : legReqs) {
                OaBusinessTripLeg leg = new OaBusinessTripLeg();
                leg.setTripId(tripId);
                leg.setLegOrder(legReq.getLegOrder());
                leg.setDeparture(legReq.getDeparture());
                leg.setArrival(legReq.getArrival());
                leg.setDepartureTime(parseDate(legReq.getDepartureTime(), "yyyy-MM-dd HH:mm:ss"));
                leg.setArrivalTime(parseDate(legReq.getArrivalTime(), "yyyy-MM-dd HH:mm:ss"));
                leg.setTransportation(legReq.getTransportation());
                leg.setEstimatedCost(legReq.getEstimatedCost());
                leg.setRemark(legReq.getRemark());
                legs.add(leg);
            }
            tripLegManager.saveBatch(legs);
        }
    }

    /**
     * 解析日期字符串
     */
    private Date parseDate(String dateStr, String pattern) {
        if (StringUtils.isEmpty(dateStr)) {
            return null;
        }
        try {
            return new SimpleDateFormat(pattern).parse(dateStr);
        } catch (ParseException e) {
            throw new BusinessException("日期格式错误: " + dateStr);
        }
    }
}
