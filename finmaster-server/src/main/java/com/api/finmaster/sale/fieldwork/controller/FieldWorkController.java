package com.qykj.finmaster.sale.fieldwork.controller;

import cn.hutool.core.io.file.FileNameUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qykj.finmaster.common.core.domain.constansts.FileConstants;
import com.qykj.finmaster.common.core.domain.resp.Result;
import com.qykj.finmaster.sale.fieldwork.domain.req.AddFieldWorkTaskReq;
import com.qykj.finmaster.sale.fieldwork.domain.req.GetFieldWorkTaskListReq;
import com.qykj.finmaster.sale.fieldwork.domain.req.UpdateStatusReq;
import com.qykj.finmaster.sale.fieldwork.domain.resp.FieldWorkTaskResp;
import com.qykj.finmaster.sale.fieldwork.entity.FieldWorkTask;
import com.qykj.finmaster.sale.fieldwork.entity.FieldWorkTaskImage;
import com.qykj.finmaster.sale.fieldwork.entity.FieldWorkProgressImage;
import com.qykj.finmaster.sale.fieldwork.entity.FieldWorkProgressRecord;
import com.qykj.finmaster.sale.fieldwork.manager.FieldWorkProgressImageManager;
import com.qykj.finmaster.sale.fieldwork.manager.FieldWorkProgressRecordManager;
import com.qykj.finmaster.sale.fieldwork.manager.FieldWorkTaskImageManager;
import com.qykj.finmaster.sale.fieldwork.manager.FieldWorkTaskManager;
import com.qykj.finmaster.sale.fieldwork.service.FieldWorkNotificationService;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/field-work")
public class FieldWorkController {

    @Autowired
    private FieldWorkTaskManager fieldWorkTaskManager;

    @Autowired
    private FieldWorkTaskImageManager fieldWorkTaskImageManager;

    @Autowired
    private FieldWorkProgressRecordManager progressRecordManager;

    @Autowired
    private FieldWorkProgressImageManager progressImageManager;

    @Autowired
    private FieldWorkNotificationService fieldWorkNotificationService;

    @InitBinder
    protected void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setLenient(true);
        binder.registerCustomEditor(Date.class, new CustomDateEditor(dateFormat, true));
    }

    private LoginUser getLoginUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (LoginUser) auth.getPrincipal();
    }

    /**
     * 我下发的任务列表
     */
    @GetMapping("/dispatched")
    public Result<?> getDispatched(GetFieldWorkTaskListReq req) {
        LoginUser loginUser = getLoginUser();
        int pageNum = req.getPageNum() == null ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null ? 10 : req.getPageSize();

        LambdaQueryWrapper<FieldWorkTask> wrapper = new LambdaQueryWrapper<FieldWorkTask>()
                .eq(FieldWorkTask::getDispatcherId, loginUser.getUserId())
                .eq(FieldWorkTask::getTenantId, loginUser.getTenantId());
        applyFilters(wrapper, req);
        wrapper.orderByDesc(FieldWorkTask::getCreateTime);

        Page<FieldWorkTask> page = fieldWorkTaskManager.page(new Page<>(pageNum, pageSize), wrapper);
        List<FieldWorkTaskResp> respList = page.getRecords().stream()
                .map(this::toResp).collect(Collectors.toList());
        return Result.ok(respList, page.getTotal());
    }

    /**
     * 我收到的任务列表
     */
    @GetMapping("/received")
    public Result<?> getReceived(GetFieldWorkTaskListReq req) {
        LoginUser loginUser = getLoginUser();
        int pageNum = req.getPageNum() == null ? 1 : req.getPageNum();
        int pageSize = req.getPageSize() == null ? 10 : req.getPageSize();

        LambdaQueryWrapper<FieldWorkTask> wrapper = new LambdaQueryWrapper<FieldWorkTask>()
                .eq(FieldWorkTask::getAssigneeId, loginUser.getUserId())
                .eq(FieldWorkTask::getTenantId, loginUser.getTenantId());
        applyFilters(wrapper, req);
        wrapper.orderByDesc(FieldWorkTask::getCreateTime);

        Page<FieldWorkTask> page = fieldWorkTaskManager.page(new Page<>(pageNum, pageSize), wrapper);
        List<FieldWorkTaskResp> respList = page.getRecords().stream()
                .map(this::toResp).collect(Collectors.toList());
        return Result.ok(respList, page.getTotal());
    }

    /**
     * 任务详情（含图片）
     */
    @GetMapping("/{id}")
    public Result<?> getDetail(@PathVariable Long id) {
        FieldWorkTask task = fieldWorkTaskManager.getById(id);
        if (task == null) {
            return Result.error("外勤任务不存在");
        }
        FieldWorkTaskResp resp = toResp(task);

        List<FieldWorkTaskImage> images = fieldWorkTaskImageManager.list(
                new LambdaQueryWrapper<FieldWorkTaskImage>()
                        .eq(FieldWorkTaskImage::getTaskId, id)
                        .orderByAsc(FieldWorkTaskImage::getUploadTime));
        resp.setImages(images);

        return Result.ok(resp);
    }

    /**
     * 创建外勤任务
     */
    @PostMapping
    public Result<?> create(@RequestBody AddFieldWorkTaskReq req) {
        LoginUser loginUser = getLoginUser();

        FieldWorkTask task = new FieldWorkTask();
        task.setTaskNo(generateTaskNo());
        task.setCustomerId(req.getCustomerId());
        task.setCustomerName(req.getCustomerName());
        task.setOrderId(req.getOrderId());
        task.setOrderNo(req.getOrderNo());
        task.setTaskType(req.getTaskType());
        task.setAssigneeId(req.getAssigneeId());
        task.setAssigneeName(req.getAssigneeName());
        task.setDispatcherId(loginUser.getUserId());
        task.setDispatcherName(loginUser.getRealName());
        task.setPlannedDuration(req.getPlannedDuration());
        task.setPlannedStartTime(req.getPlannedStartTime());
        task.setDescription(req.getDescription());
        task.setStatus("PENDING");
        task.setTenantId(loginUser.getTenantId());

        fieldWorkTaskManager.save(task);
        return Result.ok(task.getId());
    }

    /**
     * 更新任务状态
     * IN_PROGRESS（开始外勤）/ COMPLETED（完成外勤）/ CANCELLED（取消）
     */
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestBody UpdateStatusReq req) {
        LoginUser loginUser = getLoginUser();
        FieldWorkTask task = fieldWorkTaskManager.getById(id);
        if (task == null) {
            return Result.error("外勤任务不存在");
        }

        String newStatus = req.getStatus();
        // 取消：只有下发人可操作
        if ("CANCELLED".equals(newStatus)) {
            if (!loginUser.getUserId().equals(task.getDispatcherId())) {
                return Result.error("只有下发人可取消任务");
            }
        } else {
            // 开始/完成：只有负责人可操作
            if (!loginUser.getUserId().equals(task.getAssigneeId())) {
                return Result.error("只有外勤负责人可处理任务");
            }
        }

        if ("COMPLETED".equals(newStatus)) {
            if (req.getCompletionNotes() == null || req.getCompletionNotes().isBlank()) {
                return Result.error("请填写完工情况说明");
            }
        }

        FieldWorkTask update = new FieldWorkTask();
        update.setId(id);
        update.setStatus(newStatus);

        if ("IN_PROGRESS".equals(newStatus)) {
            update.setActualStartTime(new Date());
        } else if ("COMPLETED".equals(newStatus)) {
            Date now = new Date();
            update.setActualEndTime(now);
            update.setCompletionNotes(req.getCompletionNotes());
            if (task.getActualStartTime() != null) {
                long durationMs = now.getTime() - task.getActualStartTime().getTime();
                update.setActualDuration((int) (durationMs / 60000));
            }
        }

        fieldWorkTaskManager.updateById(update);
        return Result.ok();
    }

    /**
     * 移动端上传外勤图片
     */
    @PostMapping("/{taskId}/image")
    public Result<?> uploadImage(@PathVariable Long taskId,
                                 @RequestParam("file") MultipartFile file) {
        FieldWorkTask task = fieldWorkTaskManager.getById(taskId);
        if (task == null) {
            return Result.error("外勤任务不存在");
        }
        if (file.isEmpty()) {
            return Result.error("图片文件不能为空");
        }

        LoginUser loginUser = getLoginUser();
        try {
            String suffix = FileNameUtil.getSuffix(file.getOriginalFilename());
            String fileName = "FW_" + taskId + "_" + System.currentTimeMillis() + "." + suffix;
            File dir = new File(FileConstants.FILE_URL_FIELD_WORK_IMAGE);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            file.transferTo(new File(FileConstants.FILE_URL_FIELD_WORK_IMAGE + File.separator + fileName));

            String accessUrl = FileConstants.FILE_GET_FIELD_WORK_IMAGE + fileName;

            FieldWorkTaskImage image = new FieldWorkTaskImage();
            image.setTaskId(taskId);
            image.setImageUrl(accessUrl);
            image.setImageName(fileName);
            image.setUploaderId(loginUser.getUserId());
            image.setUploaderName(loginUser.getRealName());
            image.setUploadTime(new Date());
            image.setTenantId(loginUser.getTenantId());
            fieldWorkTaskImageManager.save(image);

            return Result.ok(image);
        } catch (Exception e) {
            log.error("外勤图片上传失败, taskId={}", taskId, e);
            return Result.error("图片上传失败：" + e.getMessage());
        }
    }

    /**
     * 删除外勤图片
     */
    @DeleteMapping("/image/{imageId}")
    public Result<?> deleteImage(@PathVariable Long imageId) {
        fieldWorkTaskImageManager.removeById(imageId);
        return Result.ok();
    }

    // ===== 过程记录接口 =====

    /**
     * 提交过程记录（含图片，multipart）
     */
    @PostMapping("/{taskId}/progress")
    public Result<?> addProgress(@PathVariable Long taskId,
                                  @RequestParam("content") String content,
                                  @RequestParam(value = "files", required = false) MultipartFile[] files) {
        LoginUser loginUser = getLoginUser();
        FieldWorkTask task = fieldWorkTaskManager.getById(taskId);
        if (task == null) {
            return Result.error("外勤任务不存在");
        }
        if (!"IN_PROGRESS".equals(task.getStatus())) {
            return Result.error("只有进行中的任务才能提交过程记录");
        }
        if (!loginUser.getUserId().equals(task.getAssigneeId())) {
            return Result.error("只有外勤负责人可提交过程记录");
        }

        Date now = new Date();
        FieldWorkProgressRecord record = new FieldWorkProgressRecord();
        record.setTaskId(taskId);
        record.setContent(content);
        record.setSubmitTime(now);
        record.setSubmitterId(loginUser.getUserId());
        record.setSubmitterName(loginUser.getRealName());
        record.setTenantId(loginUser.getTenantId());
        progressRecordManager.save(record);

        // 保存图片
        if (files != null) {
            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;
                try {
                    String suffix = FileNameUtil.getSuffix(file.getOriginalFilename());
                    String fileName = "FWP_" + taskId + "_" + System.currentTimeMillis() + "_"
                            + new Random().nextInt(1000) + "." + suffix;
                    File dir = new File(FileConstants.FILE_URL_FIELD_WORK_IMAGE);
                    if (!dir.exists()) dir.mkdirs();
                    file.transferTo(new File(FileConstants.FILE_URL_FIELD_WORK_IMAGE + File.separator + fileName));

                    FieldWorkProgressImage img = new FieldWorkProgressImage();
                    img.setProgressId(record.getId());
                    img.setTaskId(taskId);
                    img.setImageUrl(FileConstants.FILE_GET_FIELD_WORK_IMAGE + fileName);
                    img.setImageName(fileName);
                    img.setTenantId(loginUser.getTenantId());
                    progressImageManager.save(img);
                } catch (Exception e) {
                    log.warn("过程记录图片上传失败, taskId={}", taskId, e);
                }
            }
        }

        // 查询图片填充到 record，用于推送通知
        List<FieldWorkProgressImage> images = progressImageManager.list(
                new LambdaQueryWrapper<FieldWorkProgressImage>()
                        .eq(FieldWorkProgressImage::getProgressId, record.getId()));
        record.setImages(images);

        // 推送通知给下发人
        try {
            fieldWorkNotificationService.notifyDispatcher(task, record);
        } catch (Exception e) {
            log.warn("外勤过程通知推送失败, taskId={}", taskId, e);
        }

        return Result.ok(record);
    }

    /**
     * 查询某任务的过程记录列表（含图片）
     */
    @GetMapping("/{taskId}/progress")
    public Result<?> listProgress(@PathVariable Long taskId) {
        List<FieldWorkProgressRecord> records = progressRecordManager.list(
                new LambdaQueryWrapper<FieldWorkProgressRecord>()
                        .eq(FieldWorkProgressRecord::getTaskId, taskId)
                        .orderByAsc(FieldWorkProgressRecord::getSubmitTime));

        if (!records.isEmpty()) {
            List<Long> recordIds = records.stream()
                    .map(FieldWorkProgressRecord::getId).collect(Collectors.toList());
            List<FieldWorkProgressImage> allImages = progressImageManager.list(
                    new LambdaQueryWrapper<FieldWorkProgressImage>()
                            .in(FieldWorkProgressImage::getProgressId, recordIds));
            // 按 progressId 分组填充
            java.util.Map<Long, List<FieldWorkProgressImage>> imgMap = allImages.stream()
                    .collect(Collectors.groupingBy(FieldWorkProgressImage::getProgressId));
            records.forEach(r -> r.setImages(imgMap.getOrDefault(r.getId(), List.of())));
        }

        return Result.ok(records);
    }

    // ---- 私有辅助方法 ----

    private void applyFilters(LambdaQueryWrapper<FieldWorkTask> wrapper, GetFieldWorkTaskListReq req) {
        if (req.getCustomerName() != null && !req.getCustomerName().isBlank()) {
            wrapper.like(FieldWorkTask::getCustomerName, req.getCustomerName());
        }
        if (req.getTaskType() != null && !req.getTaskType().isBlank()) {
            wrapper.eq(FieldWorkTask::getTaskType, req.getTaskType());
        }
        if (req.getStatus() != null && !req.getStatus().isBlank()) {
            wrapper.eq(FieldWorkTask::getStatus, req.getStatus());
        }
        if (req.getStart() != null) {
            wrapper.ge(FieldWorkTask::getPlannedStartTime, req.getStart());
        }
        if (req.getEnd() != null) {
            wrapper.le(FieldWorkTask::getPlannedStartTime, req.getEnd());
        }
    }

    private FieldWorkTaskResp toResp(FieldWorkTask task) {
        FieldWorkTaskResp resp = new FieldWorkTaskResp();
        BeanUtils.copyProperties(task, resp);
        resp.setCreateTime(task.getCreateTime());
        return resp;
    }

    private String generateTaskNo() {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        int rand = new Random().nextInt(9000) + 1000;
        return "FW" + timestamp + rand;
    }
}
