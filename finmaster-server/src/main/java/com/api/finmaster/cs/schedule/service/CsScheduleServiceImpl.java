package com.qykj.finmaster.cs.schedule.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.cs.schedule.domain.CsScheduleConfig;
import com.qykj.finmaster.cs.schedule.domain.CsScheduleSlot;
import com.qykj.finmaster.cs.schedule.domain.req.GetAvailableSlotsReq;
import com.qykj.finmaster.cs.schedule.domain.req.SaveScheduleConfigReq;
import com.qykj.finmaster.cs.schedule.domain.resp.AvailableSlotResp;
import com.qykj.finmaster.cs.schedule.manager.impl.CsScheduleConfigManagerImpl;
import com.qykj.finmaster.cs.schedule.manager.impl.CsScheduleSlotManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CsScheduleServiceImpl {

    @Autowired
    private CsScheduleConfigManagerImpl configManager;
    @Autowired
    private CsScheduleSlotManagerImpl slotManager;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    /** 新增排班配置并自动生成号源 */
    @Transactional(rollbackFor = Exception.class)
    public Boolean saveScheduleConfig(SaveScheduleConfigReq req, Long tenantId, LoginUser loginUser) {
        if (req.getStartDate() == null || req.getEndDate() == null) {
            throw new BusinessException("排班日期不能为空");
        }
        if (req.getStartDate().isAfter(req.getEndDate())) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        int interval = req.getSlotInterval() != null ? req.getSlotInterval() : 30;
        int capacity = req.getMaxCapacity() != null ? req.getMaxCapacity() : 1;

        // 保存配置
        CsScheduleConfig config = new CsScheduleConfig();
        config.setTenantId(tenantId);
        config.setUserId(req.getUserId());
        config.setUserName(req.getUserName());
        config.setUserAccount(req.getUserAccount());
        config.setStartDate(req.getStartDate());
        config.setEndDate(req.getEndDate());
        config.setShiftType(req.getShiftType());
        config.setWorkStartTime(req.getWorkStartTime());
        config.setWorkEndTime(req.getWorkEndTime());
        config.setSlotInterval(interval);
        config.setMaxCapacity(capacity);
        config.setStatus("ACTIVE");
        config.setCreateBy(loginUser.getUsername());
        config.setCreateByName(loginUser.getRealName());
        config.setCreateTime(new Date());
        config.setUpdateTime(new Date());
        configManager.save(config);

        // 生成号源：日期范围内每天（排除周日）
        LocalTime startTime = LocalTime.parse(req.getWorkStartTime(), TIME_FMT);
        LocalTime endTime = LocalTime.parse(req.getWorkEndTime(), TIME_FMT);
        List<CsScheduleSlot> slots = new ArrayList<>();
        LocalDate current = req.getStartDate();
        while (!current.isAfter(req.getEndDate())) {
            // 排除周日
            if (current.getDayOfWeek() != DayOfWeek.SUNDAY) {
                LocalTime t = startTime;
                while (t.isBefore(endTime)) {
                    CsScheduleSlot slot = new CsScheduleSlot();
                    slot.setTenantId(tenantId);
                    slot.setScheduleConfigId(config.getId());
                    slot.setUserId(req.getUserId());
                    slot.setUserName(req.getUserName());
                    slot.setUserAccount(req.getUserAccount());
                    slot.setWorkDate(current);
                    slot.setShiftType(req.getShiftType());
                    slot.setSlotTime(t.format(TIME_FMT));
                    slot.setMaxCapacity(capacity);
                    slot.setBookedCount(0);
                    slot.setCreateBy(loginUser.getUsername());
                    slot.setCreateByName(loginUser.getRealName());
                    slot.setCreateTime(new Date());
                    slot.setUpdateTime(new Date());
                    slots.add(slot);
                    t = t.plusMinutes(interval);
                }
            }
            current = current.plusDays(1);
        }
        if (!slots.isEmpty()) {
            slotManager.saveBatch(slots);
        }
        return Boolean.TRUE;
    }

    /** 排班配置列表 */
    public Page<CsScheduleConfig> getConfigPage(Integer pageNum, Integer pageSize, Long tenantId) {
        return configManager.lambdaQuery()
                .eq(CsScheduleConfig::getTenantId, tenantId)
                .orderByDesc(CsScheduleConfig::getCreateTime)
                .page(new Page<>(pageNum, pageSize));
    }

    /** 删除排班配置及其号源（仅未被预约的可删） */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteScheduleConfig(Long configId) {
        // 检查是否有已预约的号源
        long booked = slotManager.lambdaQuery()
                .eq(CsScheduleSlot::getScheduleConfigId, configId)
                .gt(CsScheduleSlot::getBookedCount, 0)
                .count();
        if (booked > 0) {
            throw new BusinessException("该排班下已有预约任务，无法删除");
        }
        slotManager.lambdaUpdate()
                .eq(CsScheduleSlot::getScheduleConfigId, configId)
                .remove();
        configManager.removeById(configId);
        return Boolean.TRUE;
    }

    /** 查询某日可用号源（按管家+班次分组） */
    public List<AvailableSlotResp> getAvailableSlots(GetAvailableSlotsReq req, Long tenantId) {
        List<CsScheduleSlot> slots = slotManager.lambdaQuery()
                .eq(CsScheduleSlot::getTenantId, tenantId)
                .eq(CsScheduleSlot::getWorkDate, req.getWorkDate())
                .orderByAsc(CsScheduleSlot::getUserId)
                .orderByAsc(CsScheduleSlot::getShiftType)
                .orderByAsc(CsScheduleSlot::getSlotTime)
                .list();

        // 按 userId + shiftType 分组
        Map<String, List<CsScheduleSlot>> grouped = slots.stream()
                .collect(Collectors.groupingBy(s -> s.getUserId() + "_" + s.getShiftType()));

        List<AvailableSlotResp> result = new ArrayList<>();
        for (Map.Entry<String, List<CsScheduleSlot>> entry : grouped.entrySet()) {
            List<CsScheduleSlot> group = entry.getValue();
            CsScheduleSlot first = group.get(0);

            AvailableSlotResp resp = new AvailableSlotResp();
            resp.setUserId(first.getUserId());
            resp.setUserName(first.getUserName());
            resp.setUserAccount(first.getUserAccount());
            resp.setWorkDate(first.getWorkDate());
            resp.setShiftType(first.getShiftType());
            resp.setShiftTypeName("AM".equals(first.getShiftType()) ? "上午班" : "下午班");

            List<AvailableSlotResp.SlotItem> items = group.stream().map(s -> {
                AvailableSlotResp.SlotItem item = new AvailableSlotResp.SlotItem();
                item.setSlotId(s.getId());
                item.setSlotTime(s.getSlotTime());
                item.setMaxCapacity(s.getMaxCapacity());
                item.setBookedCount(s.getBookedCount());
                item.setRemainCount(s.getMaxCapacity() - s.getBookedCount());
                item.setIsFull(s.getBookedCount() >= s.getMaxCapacity());
                return item;
            }).collect(Collectors.toList());
            resp.setSlots(items);
            result.add(resp);
        }
        return result;
    }

    /** 预约号源（转任务单时调用） */
    public CsScheduleSlot bookSlot(Long slotId) {
        CsScheduleSlot slot = slotManager.getById(slotId);
        if (slot == null) throw new BusinessException("号源不存在");
        if (slot.getBookedCount() >= slot.getMaxCapacity()) throw new BusinessException("该时间段已满，请选择其他时间");
        slot.setBookedCount(slot.getBookedCount() + 1);
        slot.setUpdateTime(new Date());
        slotManager.updateById(slot);
        return slot;
    }

    /** 释放号源（取消任务时调用） */
    public void releaseSlot(Long slotId) {
        if (slotId == null) return;
        CsScheduleSlot slot = slotManager.getById(slotId);
        if (slot == null) return;
        if (slot.getBookedCount() > 0) {
            slot.setBookedCount(slot.getBookedCount() - 1);
            slot.setUpdateTime(new Date());
            slotManager.updateById(slot);
        }
    }
}
