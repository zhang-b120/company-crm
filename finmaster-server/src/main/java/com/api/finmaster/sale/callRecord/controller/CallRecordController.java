package com.qykj.finmaster.sale.callRecord.controller;

import cn.hutool.core.io.file.FileNameUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qykj.finmaster.common.core.domain.constansts.FileConstants;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.config.MobileWebSocketRegistry;
import com.qykj.finmaster.sale.callRecord.entity.CallRecord;
import com.qykj.finmaster.sale.callRecord.manager.CallRecordManager;
import com.qykj.finmaster.sale.callRecord.service.CallRecordQualityService;
import com.qykj.finmaster.sale.callRecord.service.CallRecordTranscriptionService;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.third.baidu.service.BaiduSpeechService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/call-record")
public class CallRecordController {

    @Autowired
    private CallRecordManager callRecordManager;

    @Autowired
    private MobileWebSocketRegistry mobileWebSocketRegistry;

    @Autowired
    private CallRecordTranscriptionService callRecordTranscriptionService;

    @Autowired
    private BaiduSpeechService baiduSpeechService;

    @Autowired
    private CallRecordQualityService callRecordQualityService;

    private LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (LoginUser) auth.getPrincipal();
    }

    /**
     * PC 端发起通话
     * 创建通话记录后，通过 WebSocket 向员工手机推送拨号指令
     */
    @PostMapping("/initiate")
    public Result<?> initiate(@RequestBody Map<String, Object> req) {
        LoginUser loginUser = getLoginUser();

        String phoneNumber = (String) req.get("phoneNumber");
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return Result.error("电话号码不能为空");
        }

        CallRecord record = new CallRecord();
        record.setTenantId(loginUser.getTenantId());
        record.setCallerUserId(loginUser.getUserId());
        record.setCallerUserName(loginUser.getRealName());
        record.setPhoneNumber(phoneNumber);
        record.setCallStatus(0); // 待拨出

        Object leadId = req.get("leadId");
        if (leadId != null) {
            record.setLeadId(Long.valueOf(leadId.toString()));
        }
        Object customerId = req.get("customerId");
        if (customerId != null) {
            record.setCustomerId(Long.valueOf(customerId.toString()));
        }
        Object companyId = req.get("companyId");
        if (companyId != null) {
            record.setCompanyId(Long.valueOf(companyId.toString()));
        }
        Object contactName = req.get("contactName");
        if (contactName != null) {
            record.setContactName(contactName.toString());
        }

        callRecordManager.save(record);

        Map<String, Object> resp = new HashMap<>();
        resp.put("callRecordId", record.getId());
        resp.put("mobileSent", false);

        // 移动端自发起时跳过 WS 推送（避免手机收到自己发出的拨号指令）
        Object fromMobile = req.get("fromMobile");
        boolean skipWsPush = Boolean.TRUE.equals(fromMobile);

        if (!skipWsPush) {
            // 推送拨号指令到移动端
            Map<String, Object> wsMsg = new HashMap<>();
            wsMsg.put("type", "CALL");
            Map<String, Object> payload = new HashMap<>();
            payload.put("phoneNumber", phoneNumber);
            payload.put("callRecordId", record.getId());
            payload.put("leadId", record.getLeadId());
            payload.put("customerId", record.getCustomerId());
            payload.put("name", record.getContactName());
            wsMsg.put("payload", payload);

            boolean sent = mobileWebSocketRegistry.sendToUser(loginUser.getUserId(), wsMsg);
            if (sent) {
                CallRecord update = new CallRecord();
                update.setId(record.getId());
                update.setCallStatus(1); // 拨出中
                callRecordManager.updateById(update);
            }
            resp.put("mobileSent", sent);
        }

        return Result.ok(resp);
    }

    /**
     * 移动端上传录音文件
     */
    @PostMapping("/{id}/upload-recording")
    public Result<?> uploadRecording(@PathVariable Long id,
                                     @RequestParam("file") MultipartFile file,
                                     @RequestParam(value = "duration", required = false) Integer duration) {
        CallRecord record = callRecordManager.getById(id);
        if (record == null) {
            return Result.error("通话记录不存在");
        }
        if (file.isEmpty()) {
            return Result.error("录音文件不能为空");
        }

        try {
            String suffix = FileNameUtil.getSuffix(file.getOriginalFilename());
            String fileName = "CR_" + id + "_" + System.currentTimeMillis() + "." + suffix;
            File dir = new File(FileConstants.FILE_URL_CALL_RECORDING);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String localPath = FileConstants.FILE_URL_CALL_RECORDING + File.separator + fileName;
            file.transferTo(new File(localPath));

            String accessUrl = FileConstants.FILE_GET_CALL_RECORDING + fileName;

            CallRecord update = new CallRecord();
            update.setId(id);
            update.setRecordingUrl(accessUrl);
            update.setRecordingLocalPath(localPath);
            update.setCallStatus(2); // 已完成
            update.setCallEndTime(new Date());
            if (duration != null && duration > 0) {
                update.setCallDuration(duration);
            }
            callRecordManager.updateById(update);

            // 上传成功后自动触发语音转写
            try {
                callRecordTranscriptionService.transcribeAsync(id);
            } catch (Exception ex) {
                log.warn("自动转写触发失败, callRecordId={}, 稍后可手动重试", id, ex);
            }

            return Result.ok(accessUrl);
        } catch (Exception e) {
            log.error("录音文件上传失败, callRecordId={}", id, e);
            return Result.error("录音上传失败：" + e.getMessage());
        }
    }

    /**
     * 触发语音转写
     */
    @PostMapping("/{id}/transcribe")
    public Result<?> transcribe(@PathVariable Long id) {
        CallRecord record = callRecordManager.getById(id);
        if (record == null) {
            return Result.error("通话记录不存在");
        }
        if (record.getRecordingLocalPath() == null && record.getRecordingUrl() == null) {
            return Result.error("该通话记录暂无录音文件");
        }
        if (record.getTranscriptionStatus() != null && record.getTranscriptionStatus() == 1) {
            return Result.error("该录音正在转写中，请稍后查看");
        }
        callRecordTranscriptionService.transcribeAsync(id);
        return Result.ok("转写任务已提交");
    }

    /**
     * 手动触发通话质量分析
     */
    @PostMapping("/{id}/analyze-quality")
    public Result<?> analyzeQuality(@PathVariable Long id) {
        CallRecord record = callRecordManager.getById(id);
        if (record == null) {
            return Result.error("通话记录不存在");
        }
        if (record.getTranscriptionStatus() == null || record.getTranscriptionStatus() != 2) {
            return Result.error("请先完成语音转写后再进行质量分析");
        }
        if (Integer.valueOf(1).equals(record.getQualityStatus())) {
            return Result.error("质量分析正在进行中，请稍后查看");
        }
        callRecordQualityService.analyzeAsync(id);
        return Result.ok("质量分析任务已提交");
    }

    /**
     * 分页查询通话记录列表
     */
    @GetMapping("/list")
    public Result<?> list(@RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "20") Integer pageSize,
                          @RequestParam(required = false) Long leadId,
                          @RequestParam(required = false) Long customerId,
                          @RequestParam(required = false) Integer callStatus) {
        LoginUser loginUser = getLoginUser();
        LambdaQueryWrapper<CallRecord> wrapper = new LambdaQueryWrapper<CallRecord>()
                .eq(loginUser.getTenantId() != null, CallRecord::getTenantId, loginUser.getTenantId())
                .eq(leadId != null, CallRecord::getLeadId, leadId)
                .eq(customerId != null, CallRecord::getCustomerId, customerId)
                .eq(callStatus != null, CallRecord::getCallStatus, callStatus)
                .orderByDesc(CallRecord::getCreateTime);

        Page<CallRecord> page = callRecordManager.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.ok(page.getRecords(), page.getTotal());
    }

    /**
     * 录音文件列表（分页，仅有录音的记录，按数据权限过滤）
     */
    @GetMapping("/recording-list")
    public Result<?> recordingList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String contactName,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String callerUserName,
            @RequestParam(required = false) Integer transcriptionStatus) {
        LoginUser loginUser = getLoginUser();
        LambdaQueryWrapper<CallRecord> wrapper = new LambdaQueryWrapper<CallRecord>()
                .eq(loginUser.getTenantId() != null, CallRecord::getTenantId, loginUser.getTenantId())
                .isNotNull(CallRecord::getRecordingUrl)
                .ne(CallRecord::getRecordingUrl, "")
                .like(contactName != null && !contactName.isBlank(), CallRecord::getContactName, contactName)
                .like(phoneNumber != null && !phoneNumber.isBlank(), CallRecord::getPhoneNumber, phoneNumber)
                .like(callerUserName != null && !callerUserName.isBlank(), CallRecord::getCallerUserName, callerUserName)
                .eq(transcriptionStatus != null, CallRecord::getTranscriptionStatus, transcriptionStatus)
                .orderByDesc(CallRecord::getCallEndTime);

        // 数据权限过滤：只能看到自己权限范围内的录音
        List<Long> dataScopeUserIds = loginUser.getDataScopeUserList();
        if (dataScopeUserIds != null && !dataScopeUserIds.isEmpty()) {
            wrapper.in(CallRecord::getCallerUserId, dataScopeUserIds);
        }

        Page<CallRecord> page = callRecordManager.page(new Page<>(pageNum, pageSize), wrapper);
        return Result.ok(page.getRecords(), page.getTotal());
    }

    /**
     * 查询线索下的通话记录
     */
    @GetMapping("/by-lead/{leadId}")
    public Result<?> byLead(@PathVariable Long leadId) {
        List<CallRecord> list = callRecordManager.list(
                new LambdaQueryWrapper<CallRecord>()
                        .eq(CallRecord::getLeadId, leadId)
                        .orderByDesc(CallRecord::getCallEndTime));
        return Result.ok(list);
    }

    /**
     * 查询客户下的通话记录
     */
    @GetMapping("/by-customer/{customerId}")
    public Result<?> byCustomer(@PathVariable Long customerId) {
        List<CallRecord> list = callRecordManager.list(
                new LambdaQueryWrapper<CallRecord>()
                        .eq(CallRecord::getCustomerId, customerId)
                        .orderByDesc(CallRecord::getCallEndTime));
        return Result.ok(list);
    }

    /**
     * 查询公司下的通话记录
     */
    @GetMapping("/by-company/{companyId}")
    public Result<?> byCompany(@PathVariable Long companyId) {
        List<CallRecord> list = callRecordManager.list(
                new LambdaQueryWrapper<CallRecord>()
                        .eq(CallRecord::getCompanyId, companyId)
                        .orderByDesc(CallRecord::getCallEndTime));
        return Result.ok(list);
    }

    /**
     * 移动端标记通话未接通（跳过录音）
     */
    @PostMapping("/{id}/missed")
    public Result<?> missed(@PathVariable Long id) {
        CallRecord update = new CallRecord();
        update.setId(id);
        update.setCallStatus(3); // 未接通
        update.setCallEndTime(new Date());
        callRecordManager.updateById(update);
        return Result.ok();
    }

    /**
     * 通话数据统计
     * 返回：员工通话时长排名（含占比）、通话次数 vs 订单数对比
     *
     * @param startDate 开始日期 yyyy-MM-dd，可空
     * @param endDate   结束日期 yyyy-MM-dd，可空
     */
    @GetMapping("/stats")
    public Result<?> stats(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        LoginUser loginUser = getLoginUser();
        Long tenantId = loginUser.getTenantId();

        // ---- 通话时长 & 次数（已完成的通话） ----
        QueryWrapper<CallRecord> callWrapper = new QueryWrapper<CallRecord>()
                .select("caller_user_id AS callerUserId", "caller_user_name AS callerUserName",
                        "COALESCE(SUM(call_duration), 0) AS totalDuration",
                        "COUNT(*) AS callCount")
                .eq("tenant_id", tenantId)
                .eq("deleted", 0)
                .eq("call_status", 2);
        if (startDate != null && !startDate.isBlank()) {
            callWrapper.ge("call_end_time", startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isBlank()) {
            callWrapper.le("call_end_time", endDate + " 23:59:59");
        }
        callWrapper.groupBy("caller_user_id", "caller_user_name")
                .orderByDesc("totalDuration");

        List<Map<String, Object>> durationRanking = callRecordManager.listMaps(callWrapper);

        // 计算总时长，用于占比
        long totalDuration = durationRanking.stream()
                .mapToLong(m -> m.get("totalDuration") == null ? 0L : ((Number) m.get("totalDuration")).longValue())
                .sum();
        durationRanking.forEach(m -> {
            long dur = m.get("totalDuration") == null ? 0L : ((Number) m.get("totalDuration")).longValue();
            double pct = totalDuration > 0 ? Math.round(dur * 1000.0 / totalDuration) / 10.0 : 0.0;
            m.put("durationPercent", pct);
        });

        // ---- 通话客户关联的订单数（线索→转化客户，私海→直接客户）----
        String startStr = (startDate != null && !startDate.isBlank()) ? startDate + " 00:00:00" : null;
        String endStr = (endDate != null && !endDate.isBlank()) ? endDate + " 23:59:59" : null;
        List<Map<String, Object>> relatedOrderStats = callRecordManager.getBaseMapper()
                .selectRelatedOrderCountByUser(tenantId, startStr, endStr);

        // ---- 按销售负责人汇总：通话次数 + 通话时长 + 通话客户关联订单数 ----
        Map<String, Map<String, Object>> mergedMap = new LinkedHashMap<>();
        for (Map<String, Object> callStat : durationRanking) {
            String name = (String) callStat.get("callerUserName");
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("userName", name);
            entry.put("callCount", callStat.get("callCount"));
            entry.put("totalDuration", callStat.get("totalDuration"));
            entry.put("relatedOrderCount", 0L);
            mergedMap.put(name == null ? "" : name, entry);
        }
        for (Map<String, Object> stat : relatedOrderStats) {
            String name = (String) stat.get("callerUserName");
            String key = name == null ? "" : name;
            if (mergedMap.containsKey(key)) {
                mergedMap.get(key).put("relatedOrderCount", stat.get("relatedOrderCount"));
            } else {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("userName", name);
                entry.put("callCount", 0L);
                entry.put("totalDuration", 0L);
                entry.put("relatedOrderCount", stat.get("relatedOrderCount"));
                mergedMap.put(key, entry);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("durationRanking", durationRanking);
        result.put("userStats", new ArrayList<>(mergedMap.values()));
        return Result.ok(result);
    }
}
