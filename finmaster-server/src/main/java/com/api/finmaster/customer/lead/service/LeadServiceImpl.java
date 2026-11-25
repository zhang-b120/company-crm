package com.qykj.finmaster.customer.lead.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.LogMessage;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.customer.customerInfo.manager.impl.CustomerInfoManagerImpl;
import com.qykj.finmaster.customer.customerInfo.service.CustomerInfoServiceImpl;
import com.qykj.finmaster.customer.customerInfoLog.service.CustomerLogServiceImpl;
import com.qykj.finmaster.customer.lead.constants.LeadRecycleConstants;
import com.qykj.finmaster.customer.lead.entity.Lead;
import com.qykj.finmaster.customer.lead.entity.req.*;
import com.qykj.finmaster.customer.lead.entity.resp.GetLeadExportResp;
import com.qykj.finmaster.customer.lead.entity.resp.GetLeadInfoResp;
import com.qykj.finmaster.customer.lead.entity.resp.GetLeadPageResp;
import com.qykj.finmaster.customer.lead.manager.impl.LeadManagerImpl;
import com.qykj.finmaster.customer.lead.mapper.LeadMapper;
import com.qykj.finmaster.system.user.mapper.UserMapper;
import com.qykj.finmaster.customer.leadLog.entity.LeadLog;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.leadLog.service.LeadLogServiceImpl;
import com.qykj.finmaster.customer.leadLog.manager.impl.LeadLogManagerImpl;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.appletUser.domain.AppletUser;
import com.qykj.finmaster.system.appletUser.manager.impl.AppletUserManagerImpl;
import com.qykj.finmaster.system.appletUser.service.AppletUserServiceImpl;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.manager.impl.UserManagerImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 线索管理Service业务层处理
 */
@Slf4j
@Service
public class LeadServiceImpl extends ServiceImpl<LeadMapper, Lead>{

    private static final Logger logger = LoggerFactory.getLogger(LeadServiceImpl.class);

    @Autowired
    private LeadLogServiceImpl leadLogService;
    @Autowired
    private AppletUserServiceImpl appletUserService;
    @Autowired
    private AppletUserManagerImpl appletUserManager;
    @Autowired
    private CustomerInfoManagerImpl customerInfoManager;
    @Autowired
    private UserManagerImpl userManager;
    @Autowired
    private LeadManagerImpl leadManager;
    @Autowired
    private LeadLogManagerImpl leadLogManager;
    @Autowired
    private CustomerInfoServiceImpl customerInfoService;
    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;
    @Autowired
    private CustomerLogServiceImpl customerLogService;
    @Autowired
    private LeadNotificationService leadNotificationService;
    @Autowired
    private com.qykj.finmaster.order.manager.impl.OrderManagerImpl orderManager;
    @Autowired
    private com.qykj.finmaster.system.dict.service.SysDictDataServiceImpl sysDictDataService;
    @Autowired
    private com.qykj.finmaster.system.bizConfig.service.SysBizConfigService bizConfigService;
    @Autowired
    private com.qykj.finmaster.customer.lead.manager.LeadPhoneManager leadPhoneManager;
    @Autowired
    private com.qykj.finmaster.customer.lead.mapper.LeadPhoneMapper leadPhoneMapper;
    @Autowired
    private com.qykj.finmaster.customer.lead.service.LeadMergeCoreService leadMergeCoreService;
    @Autowired
    private UserMapper userMapper;

    public List<GetLeadPageResp> selectLeadList(GetLeadPageReq req, Long tenantId) {
        Map<String, Object> params = req.toBeanMap();
        params.put("tenantId", tenantId);
        if(ObjectUtils.isNotEmpty(req.getFollowCount())){
            params.put("isTransaction", false);
        }
        // 待回收 Tab：从复合配置 lead.recycle 读取 no_follow_days，临界点 = 阈值 × 70%
        //          + 注入销售部成员（与 LeadAutoInvalidateTask 对齐的 ⑥ 指定人员过滤）
        // 注意：所有 DB 查询（bizConfig / userMapper）必须在 req.startPage() 之前完成，
        //      否则 PageHelper 的 ThreadLocal 分页标记会被这些查询消耗，导致 selectLeadList 不分页。
        if (req.getRecyclable() != null && req.getRecyclable() == 1) {
            try {
                int noFollowDays = bizConfigService.getGroupInt("lead.recycle", "no_follow_days", 14);
                int warnDays = (int) Math.ceil(noFollowDays * 0.7);
                java.time.LocalDateTime cutoff = java.time.LocalDateTime.now().minusDays(warnDays);
                params.put("recyclableThreshold",
                        java.util.Date.from(cutoff.atZone(java.time.ZoneId.systemDefault()).toInstant()));
            } catch (Exception e) {
                log.warn("[待回收 Tab] 计算阈值失败，跳过过滤: {}", e.getMessage());
            }
            params.put("salesDeptUsernames", resolveSalesDeptUsernames());
        }
        req.startPage();
        List<GetLeadPageResp> list = leadManager.getBaseMapper().selectLeadList(params);
        if(CollUtil.isEmpty(list)){
            return Collections.emptyList();
        }
        List<Long> leadIds = list.stream().map(GetLeadPageResp::getId).toList();
        List<Long> transactionLeadList = getTransactionLeadList(leadIds, tenantId);
        List<LeadLog> leadLogList = leadLogManager.lambdaQuery()
                .in(LeadLog::getLeadId, leadIds)
                .eq(LeadLog::getLogType, GlobalConstant.Number.TWO_INT)
                .list();
        Map<Long, List<LeadLog>> leadLogCollect = new HashMap<>();
        if(CollUtil.isNotEmpty(leadLogList)) {
            leadLogCollect = leadLogList.stream().collect(Collectors.groupingBy(LeadLog::getLeadId));
        }
        Map<Long, List<LeadLog>> finalLeadLogCollect = leadLogCollect;
        list.forEach(item -> {
            if(transactionLeadList.contains(item.getId())){
                item.setIsTransaction(Boolean.TRUE);
            }else {
                item.setIsTransaction(Boolean.FALSE);
                if(ObjectUtils.isNotEmpty(item.getLastFollowTime())){
                    item.setLastFollowInterval(DateUtils.getDaysDifference(LocalDateTime.now(), DateUtils.dateToLocalDateTime(item.getLastFollowTime())));
                }else{
                    item.setLastFollowInterval(DateUtils.getDaysDifference(LocalDateTime.now(), DateUtils.dateToLocalDateTime(item.getCreateTime())));
                }
                if(CollUtil.isNotEmpty(finalLeadLogCollect) && finalLeadLogCollect.containsKey(item.getId())){
                    item.setFollowCount(finalLeadLogCollect.get(item.getId()).size());
                }else{
                    item.setFollowCount(GlobalConstant.Number.ZERO_INT);
                }
            }
        });
        return list;
    }

    /** 销售部（含子部门）成员 username 列表，用于"待回收 Tab"与 LeadAutoInvalidateTask 对齐 */
    private List<String> resolveSalesDeptUsernames() {
        List<User> users = userMapper.selectUsersByDeptWithChildren(LeadRecycleConstants.SALES_DEPT_ID, null);
        if (CollUtil.isEmpty(users)) return Collections.emptyList();
        return users.stream()
                .map(User::getUsername)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 已成交成交线索
     * @param leadIds
     * @return
     */
    private List<Long> getTransactionLeadList(List<Long> leadIds, Long tenantId){
        List<Long> respList = new ArrayList<>();

        if (CollUtil.isEmpty(leadIds)) {
            return respList;
        }
        //获取线索相关客户信息
        List<CustomerInfo> customerInfoList = customerInfoManager.lambdaQuery()
                .in(CustomerInfo::getLeadId, leadIds)
                .list();
        if (CollUtil.isEmpty(customerInfoList)) {
            return respList;
        }
        //leadId分组k:leadId v:customerId
        Map<Long, List<Long>> leadCustomerIdsMap = customerInfoList.stream()
                .collect(Collectors.groupingBy(
                        CustomerInfo::getLeadId,
                        Collectors.mapping(CustomerInfo::getId, Collectors.toList())
                ));
        //查询已成交客户
        List<Long> allCustomerIds = customerInfoList.stream().map(CustomerInfo::getId).distinct().toList();
        List<Long> transactionCustomerIds = customerInfoService.getTransactionCustomerIds(allCustomerIds, tenantId);
        //已成交线索
        if(CollUtil.isNotEmpty(transactionCustomerIds)){
            leadCustomerIdsMap.forEach((k, v) -> {
                if(!Collections.disjoint(v, transactionCustomerIds)){
                    respList.add(k);
                }
            });
        }
        return respList;
    }

    public List<GetLeadExportResp> selectLeadListForExport(GetLeadPageReq req,Long tenantId) {
        Map<String, Object> params = req.toBeanMap();
        params.put("tenantId", tenantId);
        List<GetLeadPageResp> list = leadManager.getBaseMapper().selectLeadList(params);
        if(CollUtil.isEmpty(list)){
            return Collections.emptyList();
        }
        return CopierCopyUtils.convertTo(list, GetLeadExportResp.class);
    }

    public GetLeadInfoResp selectLeadById(Long id, Long tenantId) {
        return selectLeadById(id, tenantId, false);
    }

    /**
     * 查询线索详情。
     * @param includeDeleted true 时绕过 @TableLogic、连软删 lead 也返回；
     *                       false 走 lambdaQuery，自动过滤 deleted=0
     */
    public GetLeadInfoResp selectLeadById(Long id, Long tenantId, boolean includeDeleted) {
        GetLeadInfoResp resp = new GetLeadInfoResp();
        Lead lead;
        if (includeDeleted) {
            // 直接走 XML mapper，不应用 @TableLogic 过滤
            lead = leadManager.getBaseMapper().selectLeadById(id, tenantId);
        } else {
            lead = leadManager.lambdaQuery()
                    .eq(Lead::getId, id)
                    .eq(ObjectUtils.isNotEmpty(tenantId), Lead::getTenantId, tenantId)
                    .one();
        }
        if(ObjectUtils.isNotEmpty(lead)){
            resp = CopierCopyUtils.convertTo(lead, GetLeadInfoResp.class);
        }
        return resp;
    }

    /**
     * 转化漏斗 + 转化时长统计（M1）
     * 返回各状态计数 + 平均转化天数 + 各来源占比
     */
    public Map<String, Object> getLeadFunnel(String beginDate, String endDate, String source, String ownerUser, Long tenantId) {
        // 一次拉所有过滤后的线索（生产环境数据量大时应改 SQL 聚合，此处为简版）
        List<Lead> leads = leadManager.lambdaQuery()
                .eq(ObjectUtils.isNotEmpty(tenantId), Lead::getTenantId, tenantId)
                .eq(StringUtils.isNotBlank(source), Lead::getSource, source)
                .eq(StringUtils.isNotBlank(ownerUser), Lead::getOwnerUser, ownerUser)
                .ge(StringUtils.isNotBlank(beginDate), Lead::getCreateTime, beginDate)
                .le(StringUtils.isNotBlank(endDate), Lead::getCreateTime, endDate)
                .list();

        int total = leads.size();
        long unfollow = leads.stream().filter(l -> Integer.valueOf(1).equals(l.getStatus())).count();
        long following = leads.stream().filter(l -> Integer.valueOf(2).equals(l.getStatus())).count();
        long converted = leads.stream().filter(l -> Integer.valueOf(3).equals(l.getStatus())).count();
        long invalid = leads.stream().filter(l -> Integer.valueOf(4).equals(l.getStatus())).count();

        // 转化时长（仅对已转化线索：updateTime - createTime）
        OptionalDouble avgDays = leads.stream()
                .filter(l -> Integer.valueOf(3).equals(l.getStatus()) && l.getCreateTime() != null && l.getUpdateTime() != null)
                .mapToLong(l -> (l.getUpdateTime().getTime() - l.getCreateTime().getTime()) / 86_400_000L)
                .average();

        // 来源分布
        Map<String, Long> bySource = leads.stream()
                .filter(l -> StringUtils.isNotBlank(l.getSource()))
                .collect(Collectors.groupingBy(Lead::getSource, Collectors.counting()));

        // 失效原因分布（从 remark 里解析 [失效原因]xxx）
        Map<String, Long> byInvalidReason = leads.stream()
                .filter(l -> Integer.valueOf(4).equals(l.getStatus()) && l.getRemark() != null && l.getRemark().startsWith("[失效原因]"))
                .map(l -> {
                    int idx = l.getRemark().indexOf('\n');
                    String line = idx > 0 ? l.getRemark().substring(0, idx) : l.getRemark();
                    return line.replace("[失效原因]", "").trim();
                })
                .filter(s -> !s.isEmpty())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("unfollow", unfollow);
        result.put("following", following);
        result.put("converted", converted);
        result.put("invalid", invalid);
        result.put("conversionRate", total > 0 ? Math.round(converted * 10000.0 / total) / 100.0 : 0);
        result.put("avgDaysToConvert", avgDays.isPresent() ? Math.round(avgDays.getAsDouble() * 10) / 10.0 : 0);
        result.put("bySource", bySource);
        result.put("byInvalidReason", byInvalidReason);
        return result;
    }

    /**
     * 单条线索的成交概览（M2）
     * 流程：lead → customerInfo (leadId 反查) → orders (customerId 关联)
     */
    public Map<String, Object> getLeadOrdersSummary(Long leadId, Long tenantId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderCount", 0);
        result.put("totalAmount", java.math.BigDecimal.ZERO);
        result.put("paidAmount", java.math.BigDecimal.ZERO);
        result.put("firstOrderTime", null);
        result.put("lastOrderTime", null);
        result.put("orders", Collections.emptyList());

        // 找该线索转化出来的客户
        CustomerInfo customer = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getLeadId, leadId)
                .eq(ObjectUtils.isNotEmpty(tenantId), CustomerInfo::getTenantId, tenantId)
                .one();
        if (customer == null) return result;

        List<Order> orders = orderManager.lambdaQuery()
                .eq(Order::getCustomerId, customer.getId())
                .eq(ObjectUtils.isNotEmpty(tenantId), Order::getTenantId, tenantId)
                .orderByAsc(Order::getCreateTime)
                .list();
        if (CollUtil.isEmpty(orders)) return result;

        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        java.math.BigDecimal paid = java.math.BigDecimal.ZERO;
        for (Order o : orders) {
            if (o.getFinalAmount() != null) total = total.add(o.getFinalAmount());
            if (o.getPaidAmount() != null) paid = paid.add(o.getPaidAmount());
        }

        result.put("orderCount", orders.size());
        result.put("totalAmount", total);
        result.put("paidAmount", paid);
        result.put("firstOrderTime", orders.get(0).getCreateTime());
        result.put("lastOrderTime", orders.get(orders.size() - 1).getCreateTime());
        // 简要订单列表
        result.put("orders", orders.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getId());
            m.put("orderNo", o.getOrderNo());
            m.put("status", o.getStatus());
            m.put("finalAmount", o.getFinalAmount());
            m.put("payTime", o.getPayTime());
            return m;
        }).toList());
        return result;
    }

    /**
     * 生成线索导入模板 Excel（.xlsx）并写到 HttpServletResponse
     * 包含：表头 1 行 + 示例 1 行 + 表头加粗着色 + 列宽自适应 + 顶部说明合并单元
     */
    public void downloadImportTemplate(HttpServletResponse response) {
        // 取系统当前的"线索来源"字典作为下拉选项
        List<String> sourceLabels = loadDictLabels("customer_source",
                Arrays.asList("小程序", "引流", "抖音", "小红书", "官网", "客户介绍"));
        // 取系统当前的"线索类型"字典作为下拉选项
        List<String> typeLabels = loadDictLabels("lead_type", Collections.emptyList());

        String[] headers = { "线索名称", "线索类型", "联系人", "联系电话", "微信号", "电子邮箱", "客户名称", "线索来源", "备注" };
        String sampleSource = sourceLabels.contains("抖音") ? "抖音" : sourceLabels.get(0);
        String sampleType = !typeLabels.isEmpty() ? typeLabels.get(0) : "";
        Object[] sample  = { "示例：张三科技", sampleType, "张三", "13800138000", "zs_wx", "zs@example.com", "张三科技有限公司", sampleSource, "对某业务感兴趣" };
        String tipText   = "填写说明：线索名称/线索类型/联系人 必填；电话和微信至少填一项；线索类型和线索来源请从单元格下拉中选择";

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("线索导入模板");

            // 顶部说明行（合并整行）
            Row tipRow = sheet.createRow(0);
            tipRow.setHeightInPoints(28);
            Cell tipCell = tipRow.createCell(0);
            tipCell.setCellValue(tipText);
            CellStyle tipStyle = wb.createCellStyle();
            Font tipFont = wb.createFont();
            tipFont.setItalic(true);
            tipFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            tipStyle.setFont(tipFont);
            tipStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            tipStyle.setWrapText(true);
            tipCell.setCellStyle(tipStyle);
            // 说明行合并整行（headers.length 是 9）
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, headers.length - 1));

            // 表头
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(22);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // 示例行
            CellStyle sampleStyle = wb.createCellStyle();
            Font sampleFont = wb.createFont();
            sampleFont.setColor(IndexedColors.GREY_40_PERCENT.getIndex());
            sampleStyle.setFont(sampleFont);
            sampleStyle.setBorderBottom(BorderStyle.THIN);
            sampleStyle.setBorderLeft(BorderStyle.THIN);
            sampleStyle.setBorderRight(BorderStyle.THIN);

            Row sampleRow = sheet.createRow(2);
            for (int i = 0; i < sample.length; i++) {
                Cell c = sampleRow.createCell(i);
                c.setCellValue(String.valueOf(sample[i]));
                c.setCellStyle(sampleStyle);
            }

            // 列宽（POI 单位 = 1/256 字符宽度）—— 现在 9 列（多了"线索类型"）
            int[] widths = { 18, 12, 12, 16, 16, 22, 22, 14, 30 };
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }

            // ===== 数据验证下拉：线索类型（B列） + 线索来源（H列） =====
            // 把选项写到独立 VERY_HIDDEN sheet，用工作表+绝对引用作为 formula
            // 范围用 SpreadsheetVersion 取整列最大行号 → 覆盖整列（B3 到末尾、H3 到末尾）
            DataValidationHelper helper = sheet.getDataValidationHelper();
            int lastRowOfColumn = org.apache.poi.ss.SpreadsheetVersion.EXCEL2007.getLastRowIndex();

            // -- 线索类型 --
            if (!typeLabels.isEmpty()) {
                String typeSheetName = "TypeOptions";
                Sheet typeSheet = wb.createSheet(typeSheetName);
                for (int i = 0; i < typeLabels.size(); i++) {
                    typeSheet.createRow(i).createCell(0).setCellValue(typeLabels.get(i));
                }
                wb.setSheetVisibility(wb.getSheetIndex(typeSheet), org.apache.poi.ss.usermodel.SheetVisibility.VERY_HIDDEN);

                String typeRef = "'" + typeSheetName + "'!$A$1:$A$" + typeLabels.size();
                // 整列：第 3 行 (索引 2) 到 xlsx 最大行 1048575，第 2 列 (索引 1)
                DataValidation typeValidation = helper.createValidation(
                        helper.createFormulaListConstraint(typeRef),
                        new CellRangeAddressList(2, lastRowOfColumn, 1, 1));
                // ⚠️ POI 的语义反了：传 true 实际写出 showDropDown="false"，Excel/WPS 才显示下拉箭头
                typeValidation.setSuppressDropDownArrow(true);
                typeValidation.setShowErrorBox(true);
                typeValidation.createErrorBox("非法的线索类型", "请从下拉列表中选择有效的类型");
                typeValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
                sheet.addValidationData(typeValidation);
            }

            // -- 线索来源 --
            String sourceSheetName = "SourceOptions";
            Sheet sourceSheet = wb.createSheet(sourceSheetName);
            for (int i = 0; i < sourceLabels.size(); i++) {
                sourceSheet.createRow(i).createCell(0).setCellValue(sourceLabels.get(i));
            }
            wb.setSheetVisibility(wb.getSheetIndex(sourceSheet), org.apache.poi.ss.usermodel.SheetVisibility.VERY_HIDDEN);

            String sourceRef = "'" + sourceSheetName + "'!$A$1:$A$" + sourceLabels.size();
            // 整列：第 3 行到 xlsx 最大行，第 8 列 (索引 7)
            DataValidation sourceValidation = helper.createValidation(
                    helper.createFormulaListConstraint(sourceRef),
                    new CellRangeAddressList(2, lastRowOfColumn, 7, 7));
            // ⚠️ POI 语义反了，同上
            sourceValidation.setSuppressDropDownArrow(true);
            sourceValidation.setShowErrorBox(true);
            sourceValidation.createErrorBox("非法的线索来源", "请从下拉列表中选择有效的来源");
            sourceValidation.setErrorStyle(DataValidation.ErrorStyle.STOP);
            sheet.addValidationData(sourceValidation);

            // 写出 response
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            String fileName = URLEncoder.encode("线索导入模板.xlsx", StandardCharsets.UTF_8).replace("+", "%20");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);
            try (OutputStream os = response.getOutputStream()) {
                wb.write(os);
                os.flush();
            }
        } catch (IOException e) {
            log.error("[线索导入] 生成模板失败", e);
            throw new BusinessException("生成模板失败：" + e.getMessage());
        }
    }

    /**
     * 批量导入线索 Excel
     * 表头顺序（严格）：线索名称 | 联系人 | 联系电话 | 微信号 | 电子邮箱 | 客户名称 | 线索来源 | 备注
     * 返回 Map：total / successCount / failList
     *
     * 错误隔离：单条失败不影响其它
     * 查重：复用 insertLead 里的 phone/wechat 唯一性校验
     */
    public Map<String, Object> importFromExcel(MultipartFile file, Long tenantId, LoginUser loginUser) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> failList = new ArrayList<>();
        int total = 0;
        int successCount = 0;

        // 字典 label → value 映射（用户在 Excel 里看到/选择的是 label，DB 存 value）
        Map<String, String> typeMap = loadDictLabelToValueMap("lead_type");
        Map<String, String> sourceMap = loadDictLabelToValueMap("customer_source");

        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) { // 同时兼容 .xls 和 .xlsx
            Sheet sheet = workbook.getSheetAt(0);
            int lastRow = sheet.getLastRowNum();
            // 跳过模板顶部"填写说明"行（如果首列首行是说明文本而不是表头"线索名称"）
            int dataStartRow = detectHeaderRow(sheet) + 1;
            for (int rowIdx = dataStartRow; rowIdx <= lastRow; rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;
                // 整行空跳过
                String leadName = getCellString(row, 0);
                if (StringUtils.isBlank(leadName)) continue;
                total++;

                // 9 列：线索名称 | 线索类型 | 联系人 | 联系电话 | 微信号 | 电子邮箱 | 客户名称 | 线索来源 | 备注
                String typeText    = getCellString(row, 1);
                String contactName = getCellString(row, 2);
                String phone       = getCellString(row, 3);
                String wechat      = getCellString(row, 4);
                String email       = getCellString(row, 5);
                String companyName = getCellString(row, 6);
                String sourceText  = getCellString(row, 7);
                String remark      = getCellString(row, 8);

                // 必填校验
                if (StringUtils.isBlank(typeText)) {
                    failList.add(buildFailRow(rowIdx + 1, leadName, "线索类型不能为空"));
                    continue;
                }
                if (StringUtils.isBlank(contactName) || (StringUtils.isBlank(phone) && StringUtils.isBlank(wechat))) {
                    failList.add(buildFailRow(rowIdx + 1, leadName, "联系人和 电话/微信 至少一项不能为空"));
                    continue;
                }

                // 字典 label → value 转换（兼容直接填 value 的情况）
                String typeValue = typeMap.getOrDefault(typeText.trim(), typeText.trim());
                String sourceValue = StringUtils.isNotBlank(sourceText)
                        ? sourceMap.getOrDefault(sourceText.trim(), sourceText.trim())
                        : null;

                // 组 AddLeadReq 并复用 insertLead 的校验逻辑
                try {
                    AddLeadReq req = new AddLeadReq();
                    req.setLeadName(leadName);
                    req.setType(typeValue);
                    req.setContactName(contactName);
                    if (StringUtils.isNotBlank(phone)) req.setPhone(phone.trim());
                    if (StringUtils.isNotBlank(wechat)) req.setWechat(wechat.trim());
                    if (StringUtils.isNotBlank(email)) req.setEmail(email.trim());
                    if (StringUtils.isNotBlank(companyName)) req.setCompanyName(companyName.trim());
                    if (StringUtils.isNotBlank(sourceValue)) req.setSource(sourceValue);
                    if (StringUtils.isNotBlank(remark)) req.setRemark(remark.trim());
                    req.setStatus(GlobalConstant.Number.ONE_INT); // 未跟进
                    insertLead(req, tenantId, loginUser);
                    successCount++;
                } catch (BusinessException be) {
                    failList.add(buildFailRow(rowIdx + 1, leadName, be.getMessage()));
                } catch (Exception e) {
                    failList.add(buildFailRow(rowIdx + 1, leadName, "导入异常：" + e.getMessage()));
                }
            }
        } catch (Exception e) {
            log.error("[线索批量导入] Excel 解析失败", e);
            throw new BusinessException("Excel 解析失败：" + e.getMessage());
        }

        result.put("total", total);
        result.put("successCount", successCount);
        result.put("failCount", failList.size());
        result.put("failList", failList);
        return result;
    }

    /**
     * 拉取字典 label 列表，失败/为空时用兜底列表
     */
    private List<String> loadDictLabels(String dictTypeCode, List<String> fallback) {
        try {
            List<com.qykj.finmaster.system.dict.entity.resp.GetDictDataListResp> dictList =
                    sysDictDataService.getDictDataListByTypeCode(dictTypeCode);
            if (dictList != null && !dictList.isEmpty()) {
                List<String> labels = new ArrayList<>();
                for (com.qykj.finmaster.system.dict.entity.resp.GetDictDataListResp d : dictList) {
                    if (d.getLabel() != null && !d.getLabel().isEmpty()) {
                        labels.add(d.getLabel());
                    }
                }
                if (!labels.isEmpty()) return labels;
            }
        } catch (Exception e) {
            log.warn("[字典] 获取 {} 字典失败，使用兜底: {}", dictTypeCode, e.getMessage());
        }
        return new ArrayList<>(fallback);
    }

    /**
     * 字典 label → value 反向映射
     * 导入时用户填写的是 label（"代账"），后端 type/source 字段存的是 value
     */
    private Map<String, String> loadDictLabelToValueMap(String dictTypeCode) {
        Map<String, String> ret = new HashMap<>();
        try {
            List<com.qykj.finmaster.system.dict.entity.resp.GetDictDataListResp> dictList =
                    sysDictDataService.getDictDataListByTypeCode(dictTypeCode);
            if (dictList != null) {
                for (com.qykj.finmaster.system.dict.entity.resp.GetDictDataListResp d : dictList) {
                    if (d.getLabel() != null && d.getValue() != null) {
                        ret.put(d.getLabel(), d.getValue());
                    }
                }
            }
        } catch (Exception ignored) {}
        return ret;
    }

    /**
     * 自动识别表头所在行（兼容用模板下载的"说明行 + 表头 + 数据行"和裸表头两种格式）
     * 找到第一列等于"线索名称"的行视作表头行；找不到则默认 0
     */
    private int detectHeaderRow(Sheet sheet) {
        int last = Math.min(sheet.getLastRowNum(), 5); // 最多前 6 行内
        for (int i = 0; i <= last; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String first = getCellString(row, 0);
            if ("线索名称".equals(first)) return i;
        }
        return 0;
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        String v;
        switch (cell.getCellType()) {
            case STRING: v = cell.getStringCellValue(); break;
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    v = cell.getDateCellValue().toString();
                } else {
                    // 避免科学计数法（手机号场景常见）
                    double d = cell.getNumericCellValue();
                    if (d == Math.floor(d)) v = String.valueOf((long) d);
                    else v = String.valueOf(d);
                }
                break;
            case BOOLEAN: v = String.valueOf(cell.getBooleanCellValue()); break;
            case FORMULA:
                try { v = cell.getStringCellValue(); }
                catch (Exception e) { v = String.valueOf(cell.getNumericCellValue()); }
                break;
            default: return null;
        }
        return v == null ? null : v.trim();
    }

    private Map<String, Object> buildFailRow(int rowNo, String leadName, String reason) {
        Map<String, Object> m = new HashMap<>();
        m.put("row", rowNo);
        m.put("leadName", leadName);
        m.put("reason", reason);
        return m;
    }

    /**
     * 线索查重：新增线索时前端实时调用，任一字段命中即返回可能重复的线索摘要。
     * phone / wechat 走"主表 ∪ 子表"并集；companyName 仅匹配主表（无对应子表字段）。
     */
    public List<GetLeadInfoResp> duplicateCheck(String phone, String wechat, String companyName, Long tenantId) {
        if (StringUtils.isBlank(phone) && StringUtils.isBlank(wechat) && StringUtils.isBlank(companyName)) {
            return Collections.emptyList();
        }
        // 通过子表 mapper 取 phone / wechat 撞库的 leadId 列表
        List<Long> hitLeadIds = new ArrayList<>();
        if (StringUtils.isNotBlank(phone)) {
            List<Long> ids = leadPhoneMapper.selectLeadIdsByContact(phone.trim(), null, null, tenantId);
            if (CollUtil.isNotEmpty(ids)) hitLeadIds.addAll(ids);
        }
        if (StringUtils.isNotBlank(wechat)) {
            List<Long> ids = leadPhoneMapper.selectLeadIdsByContact(null, null, wechat.trim(), tenantId);
            if (CollUtil.isNotEmpty(ids)) hitLeadIds.addAll(ids);
        }
        List<Lead> list = leadManager.lambdaQuery()
                .eq(ObjectUtils.isNotEmpty(tenantId), Lead::getTenantId, tenantId)
                .and(qw -> {
                    boolean anyHit = false;
                    if (CollUtil.isNotEmpty(hitLeadIds)) {
                        qw.or(w -> w.in(Lead::getId, hitLeadIds));
                        anyHit = true;
                    }
                    if (StringUtils.isNotBlank(companyName)) {
                        qw.or(w -> w.like(Lead::getCompanyName, companyName.trim()));
                        anyHit = true;
                    }
                    if (!anyHit) {
                        qw.eq(Lead::getId, -1L); // 防止 query 退化为全表
                    }
                })
                .orderByDesc(Lead::getUpdateTime)
                .last("LIMIT 5")
                .list();
        return CopierCopyUtils.convertTo(list, GetLeadInfoResp.class);
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean insertLead(AddLeadReq req , Long tenantId, LoginUser loginUser)  {
        Lead lead = CopierCopyUtils.convertTo(req, Lead.class);
        // 设置租户ID
        lead.setTenantId(tenantId);
        lead.setDeleted(GlobalConstant.Number.ZERO_INT);
        // 默认设置为有效
        lead.setIsValid(GlobalConstant.Number.ONE_INT); // 默认设置为有效
        // 检查手机号是否已存在（主表 ∪ 子表）
        if (StringUtils.isNotEmpty(req.getPhone())) {
            lead.setPhone(lead.getPhone().trim());
            List<Long> hits = leadPhoneMapper.selectLeadIdsByContact(lead.getPhone(), null, null, tenantId);
            if (CollUtil.isNotEmpty(hits)) {
                throw new BusinessException(ResultMsg.LEAD_ADD_EXISTS_PHONE);
            }
        }
        // 检查微信号是否已存在（主表 ∪ 子表）
        if (StringUtils.isNotEmpty(req.getWechat())) {
            lead.setWechat(lead.getWechat().trim());
            List<Long> hits = leadPhoneMapper.selectLeadIdsByContact(null, null, lead.getWechat(), tenantId);
            if (CollUtil.isNotEmpty(hits)) {
                throw new BusinessException(ResultMsg.LEAD_ADD_EXISTS_WECHAT);
            }
        }
        // 获取当前登录用户信息
        if (ObjectUtils.isNotEmpty(loginUser)) {
            lead.setCreateBy(loginUser.getUsername());
            lead.setCreateByName(loginUser.getRealName());
            lead.setOwnerUser(loginUser.getUsername());
            lead.setOwnerUserName(loginUser.getRealName());
            // 插入线索
            boolean flag = leadManager.save(lead);
            if (flag) {
                // 同步落 lead_phone 子表（主通道）
                upsertChildPhoneRow(lead.getId(), lead.getPhone(), lead.getOpenId(), lead.getWechat(),
                        "lead_create", GlobalConstant.Number.ONE_INT);
                // 创建线索日志
                LeadLog leadLog = new LeadLog();
                leadLog.setLeadId(lead.getId());
                leadLog.setLogType(1); // 1表示创建线索
                leadLog.setContent("创建线索：" + lead.getLeadName());
                leadLog.setOperatorId(loginUser.getUserId());
                leadLog.setOperatorName(loginUser.getRealName());
                leadLog.setTenantId(tenantId); // 设置租户ID
                leadLogService.addLeadLogByCommon(lead.getId(), GlobalConstant.Number.ONE_INT,
                        "创建线索：" + lead.getLeadName(), loginUser.getUserId(), loginUser.getRealName());
            }
            // 当归属人不是创建者时，通知归属人
            if (flag && !loginUser.getUsername().equals(lead.getOwnerUser())) {
                leadNotificationService.notifyOwner(lead, "new");
            }
            return Boolean.TRUE;
        } else {
            return Boolean.FALSE;
        }
    }

    public Boolean updateLead(UpdateLeadReq req, Long tenantId, LoginUser loginUser) {
        Lead lead = CopierCopyUtils.convertTo(req, Lead.class);
        lead.setTenantId(tenantId);
        // 检查手机号是否已存在（主表 ∪ 子表；排除自身 lead）
        if (StringUtils.isNotEmpty(req.getPhone())) {
            lead.setPhone(lead.getPhone().trim());
            List<Long> hits = leadPhoneMapper.selectLeadIdsByContact(lead.getPhone(), null, null, tenantId);
            if (hits != null && hits.stream().anyMatch(id -> !id.equals(lead.getId()))) {
                throw new BusinessException(ResultMsg.LEAD_UPDATE_EXISTS_PHONE);
            }
        }

        // 检查微信号是否已存在（主表 ∪ 子表；排除自身 lead）
        if (StringUtils.isNotEmpty(req.getWechat())) {
            lead.setWechat(lead.getWechat().trim());
            List<Long> hits = leadPhoneMapper.selectLeadIdsByContact(null, null, lead.getWechat(), tenantId);
            if (hits != null && hits.stream().anyMatch(id -> !id.equals(lead.getId()))) {
                throw new BusinessException(ResultMsg.LEAD_UPDATE_EXISTS_WECHAT);
            }
        }
        // 取原 lead 的 phone/openId/wechat，用于变更判定（必须在 updateById 前读）
        Lead originalLead = leadManager.getById(lead.getId());
        String oldPhone  = originalLead == null ? null : originalLead.getPhone();
        String oldOpenId = originalLead == null ? null : originalLead.getOpenId();
        String oldWechat = originalLead == null ? null : originalLead.getWechat();

        // 获取当前登录用户信息
        if (ObjectUtils.isNotEmpty(loginUser)) {
            lead.setUpdateBy(loginUser.getUsername());
            lead.setUpdateByName(loginUser.getRealName());
        }
        boolean ok = leadManager.updateById(lead);

        // 主通道刷新：若 phone/openId/wechat 任一变了，
        // demote 老主通道（保留为历史），新建/复用一行作为新主通道
        if (ok) {
            leadMergeCoreService.afterUpdateLeadRefreshPrimary(
                    lead.getId(),
                    oldPhone, oldOpenId, oldWechat,
                    lead.getPhone(), lead.getOpenId(), lead.getWechat(),
                    loginUser);
        }
        return ok;
    }

    /**
     * 批量分配线索
     * @param req
     * @param loginUser
     * @return
     */
    public Boolean assignLead(AssignLeadReq req, LoginUser loginUser) {
        if(ObjectUtils.isEmpty(req) || CollUtil.isEmpty(req.getIds())){
            return Boolean.FALSE;
        }
        if(StringUtils.isEmpty(req.getOwnerUser()) || StringUtils.isEmpty(req.getOwnerUserName())){
            return Boolean.FALSE;
        }
        // 普通客服无权限
        if(CollUtil.isEmpty(loginUser.getRoleIds()) || loginUser.getRoleIds().contains(GlobalConstant.Number.FOUR_LONG)){
            throw new BusinessException(ResultMsg.COMMON_NOT_HAVE_ROLE);
        }
        //校验
        checkAssignLead(req.getIds(), loginUser);
        // 修改线索数据
        leadManager.lambdaUpdate()
                .set(Lead::getOwnerUser, req.getOwnerUser())
                .set(Lead::getOwnerUserName, req.getOwnerUserName())
                .set(Lead::getUpdateTime, new Date())
                .set(Lead::getUpdateBy, loginUser.getUsername())
                .set(Lead::getUpdateByName, loginUser.getRealName())
                .in(Lead::getId, req.getIds())
                .update();
        //分配线索日志在前端完成
        req.getIds().forEach(item -> {
            dealCustomer(item, req.getOwnerUser(), req.getOwnerUserName(), loginUser, Boolean.FALSE);
        });
        // 通知被分配的归属人
        List<Lead> assignedLeads = leadManager.lambdaQuery()
                .in(Lead::getId, req.getIds())
                .list();
        assignedLeads.forEach(lead -> leadNotificationService.notifyOwner(lead, "assign"));
        return Boolean.TRUE;
    }

    /**
     * 处理客户
     * @param leadId
     * @param ownerUser
     * @param ownerUserName
     * @param loginUser
     * @param flag true-释放线索 false-分配线索
     */
    @Transactional(rollbackFor = Exception.class)
    public void dealCustomer(Long leadId, String ownerUser, String ownerUserName, LoginUser loginUser, Boolean flag) {
        List<CustomerInfo> customerInfoList = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getLeadId, leadId)
                .list();
        if(CollUtil.isNotEmpty(customerInfoList)){
            Integer customerType = flag ? GlobalConstant.Number.TWO_INT : GlobalConstant.Number.ONE_INT;
            customerInfoList.forEach(item -> {
                customerInfoManager.lambdaUpdate()
                        .set(CustomerInfo::getOwnerUser,ownerUser)
                        .set(CustomerInfo::getOwnerUserName,ownerUserName)
                        .set(CustomerInfo::getCustomerType, customerType)
                        .set(CustomerInfo::getUpdateTime, new Date())
                        .set(CustomerInfo::getUpdateBy, loginUser.getUsername())
                        .set(CustomerInfo::getUpdateByName, loginUser.getRealName())
                        .in(CustomerInfo::getId, item.getId())
                        .update();
                // 日志
                String content = "";
                if(flag){
                    content = MessageFormat.format(LogMessage.LEAD_TO_CUSTOMER_PUBLIC_LOG, item.getOwnerUserName(), loginUser.getRealName());
                }else{
                    content = MessageFormat.format(LogMessage.LEAD_TO_CUSTOMER_PRIVATE_LOG, ownerUserName, loginUser.getRealName());
                }
                customerLogService.recordCustomerLog(item.getId(), GlobalConstant.Number.FIVE_INT, content, loginUser.getUsername(), loginUser.getRealName());
            });
        }

    }

    /**
     * 分配线索校验
     * @param ids
     * @param loginUser
     */
    private void checkAssignLead(List<Long> ids, LoginUser loginUser){
        // 获取当前登录用户
        if(ObjectUtils.isEmpty(loginUser)){
            throw new BusinessException(ResultMsg.LEAD_ASSIGN_NOT_HAVE_LOGIN_USER);
        }
        //管理员不校验
        if(loginUser.isAdmin()){
            return;
        }
        String currentUsername = loginUser.getUsername();
        // 查询所有选中的线索
        List<Lead> leads = leadManager.lambdaQuery()
                .in(Lead::getId, ids)
                .list();
        // 检查是否有不符合条件的线索（已有负责人且不是当前用户）
        List<Lead> invalidLeads = new ArrayList<>();
        for(Lead lead : leads){
            // 如果线索已有负责人且不是当前用户
            if(StringUtils.isNotEmpty(lead.getOwnerUser()) &&
                    !lead.getOwnerUser().equals(currentUsername)){
                invalidLeads.add(lead);
            }
        }
        // 如果有不符合条件的线索，抛出异常
        if(CollUtil.isNotEmpty(invalidLeads)){
            if(invalidLeads.size() == 1){
                // 如果只有一条线索，显示具体线索名称
                String leadName = invalidLeads.get(0).getLeadName();
                throw new BusinessException(MessageFormat.format(ResultMsg.LEAD_ASSIGN_HAS_OWNER_SINGLE, leadName));
            } else {
                // 如果有多条线索，显示通用提示
                throw new BusinessException(ResultMsg.LEAD_ASSIGN_HAS_OWNER_MULTIPLE);
            }
        }
    }

    /**
     * 删除线索（逻辑删除）。
     * 同时把 lead_phone 子表对应行一并软删，避免已删线索仍能通过子表的 phone/openId 被反查到。
     * lead_log 与 customer_info.lead_id 不级联（保留历史轨迹与客户独立性）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteLeadByIds(String ids)  {
        if(StringUtils.isEmpty(ids)){
            return Boolean.FALSE;
        }
        List<Long> list = Arrays.asList(ids.split(GlobalConstant.Symbol.COMMA)).stream()
                .map(Long::valueOf).toList();
        // 1) 软删 sale_lead 主表
        leadManager.lambdaUpdate()
                .set(Lead::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(Lead::getUpdateTime, new Date())
                .in(Lead::getId, list)
                .update();
        // 2) 级联软删 lead_phone 子表（避免 uaAdd 反查到已删线索）
        leadPhoneManager.lambdaUpdate()
                .set(com.qykj.finmaster.customer.lead.entity.LeadPhone::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(com.qykj.finmaster.customer.lead.entity.LeadPhone::getUpdateTime, new Date())
                .in(com.qykj.finmaster.customer.lead.entity.LeadPhone::getLeadId, list)
                .update();
        return Boolean.TRUE;
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean convertToCustomer(Long id, String companyName, Integer customerType, Long tenantId, LoginUser loginUser) {
        // 1. 获取线索信息
        Lead lead = leadManager.lambdaQuery()
                .eq(Lead::getId, id)
                .one();
        if(ObjectUtils.isEmpty(lead)){
            return Boolean.FALSE;
        }
        // 1.1 字段前置校验：customer_info 的关键列是 NOT NULL，缺失时直接抛业务异常
        // （否则会落到全局 handleException 报"系统错误，请联系管理员"，前端看不到真因）
        if (StringUtils.isBlank(lead.getPhone())) {
            throw new BusinessException("线索缺少联系电话，无法转化为客户，请先完善线索的联系电话后再尝试转化");
        }
        if (StringUtils.isBlank(lead.getContactName())) {
            throw new BusinessException("线索缺少联系人，无法转化为客户，请先完善线索的联系人后再尝试转化");
        }
        if (StringUtils.isBlank(companyName)) {
            throw new BusinessException("客户名称不能为空");
        }
        // 2. 创建客户对象
        CustomerInfo customerInfo = CopierCopyUtils.convertTo(lead, CustomerInfo.class);
        customerInfo.setId(null);
        customerInfo.setCustomerName(companyName);
        customerInfo.setCustomerType(customerType);
        customerInfo.setLeadId(lead.getId());
        customerInfo.setCreateTime(new Date());
        customerInfo.setUpdateTime(new Date());
        customerInfo.setTenantId(tenantId);
        // 设置初始状态为未跟进
        customerInfo.setStatus(GlobalConstant.Number.ONE_INT);
        if(ObjectUtils.isNotEmpty(loginUser)) {
            customerInfo.setCreateBy(loginUser.getUsername());
            customerInfo.setCreateByName(loginUser.getRealName());
            customerInfo.setUpdateBy(loginUser.getUsername());
            customerInfo.setUpdateByName(loginUser.getRealName());
            customerInfo.setOwnerUser(loginUser.getUsername());
            customerInfo.setOwnerUserName(loginUser.getRealName());
        }
        customerInfo.setLastFollowContent("【线索】" + lead.getLastFollowContent());
        // 转换 LocalDateTime 到 Date
        if (lead.getNextFollowTime() != null) {
            customerInfo.setNextFollowTime(DateUtils.localDateTimeToDate(lead.getNextFollowTime()));
        }
        // 3. 保存客户信息
        customerInfoManager.save(customerInfo);
        // 4. 更新线索状态为已转化
        leadManager.lambdaUpdate()
                .set(Lead::getStatus, GlobalConstant.Number.THREE_INT)
                .set(Lead::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), Lead::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), Lead::getUpdateByName, loginUser.getRealName())
                .eq(Lead::getId, id)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 合并线索（新版）。
     *
     * 行为：
     *  1. mergeResult 中字段覆盖到 sourceLeadId（保留方）→ 兼容旧前端入参语义
     *  2. 调用 {@link LeadMergeCoreService#mergeByOldApi}：将 targetLeadId（被合并方）的
     *     phone/openId/wechat 落入 sourceLeadId 子表 + 客户层归并 + CC 家族广播 + 软删 targetLeadId
     *
     * 注意：旧 API 的 sourceLeadId 是"保留方"，targetLeadId 是"被合并方"——这与字面理解
     * 相反。新合并核心以"target=保留"为语义，因此调用时需要互换。
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean mergeLead(Map<String, Object> params, Long tenantId)   {
        Long sourceLeadId = Long.valueOf(params.get("sourceLeadId").toString());   // 保留方
        Long targetLeadId = Long.valueOf(params.get("targetLeadId").toString());   // 被合并方
        Map<String, Object> mergeResult = (Map<String, Object>) params.get("mergeResult");
        // 旧 API 中 sourceLeadId = 保留方，新核心入参规约 target=保留方，因此对调
        return leadMergeCoreService.mergeByOldApi(targetLeadId, sourceLeadId, mergeResult, tenantId);
    }

    /**
     * 小程序新增线索（uaAdd）。
     *
     * 新版语义：phone / openId 查重路径走"主表 ∪ 子表"并集（{@link LeadPhoneMapper#selectLeadIdsByContact}）。
     *  1. openId 已在系统中（任一线索的主表或子表）→ 幂等返回
     *  2. phone 已在系统中 → 把 openId 回写到主表 + 子表，并补齐这些 lead 名下客户/公司的 openId
     *  3. 否则新建 lead，并在 lead_phone 子表落一行 is_primary=1
     *
     * @param req
     * @return
     */
    @Transactional
    public Boolean uaAdd(UaAddLeadReq req) {
        // 1) openId 查重（主表 ∪ 子表）
        List<Long> hitByOpenId = StringUtils.isEmpty(req.getOpenId())
                ? Collections.emptyList()
                : leadPhoneMapper.selectLeadIdsByContact(null, req.getOpenId(), null, null);
        if(CollUtil.isNotEmpty(hitByOpenId)) {
            ensureAppletUser(req);
            return Boolean.TRUE; // 已有该 openId 的 lead，幂等；不算首次注册
        }

        // 首次注册标志：当前 openId 在系统里不存在 AppletUser 时，才是真正的"首次注册"
        boolean isFirstRegistration = StringUtils.isNotEmpty(req.getOpenId())
                && appletUserManager.lambdaQuery()
                        .eq(AppletUser::getOpenId, req.getOpenId())
                        .count() == 0;

        // 2) phone 查重（主表 ∪ 子表）
        List<Long> hitByPhone = StringUtils.isEmpty(req.getPhone())
                ? Collections.emptyList()
                : leadPhoneMapper.selectLeadIdsByContact(req.getPhone(), null, null, null);
        List<Long> notifyLeadIds = new ArrayList<>();
        if(CollUtil.isNotEmpty(hitByPhone)) {
            // 把 openId 回写到主表所有 phone 命中的 lead
            if(StringUtils.isNotEmpty(req.getPhone()) && StringUtils.isNotEmpty(req.getOpenId())) {
                leadManager.getBaseMapper().updateOpenIdByPhone(req.getOpenId(), req.getPhone());
            }
            // 给命中的 lead 子表补一行（phone + openId 通道）
            for (Long leadId : hitByPhone) {
                upsertChildPhoneRow(leadId, req.getPhone(), req.getOpenId(), null, "mini_program");
            }
            // 这些 lead 名下客户的 openId 也补齐（兼容历史逻辑）
            List<CustomerInfo> customerInfoList = customerInfoManager.lambdaQuery()
                    .and(item -> item.in(CustomerInfo::getLeadId, hitByPhone)
                            .or().eq(CustomerInfo::getPhone, req.getPhone()))
                    .and(item -> item.isNull(CustomerInfo::getOpenId)
                            .or().eq(CustomerInfo::getOpenId, ""))
                    .list();
            if(CollUtil.isNotEmpty(customerInfoList)) {
                List<Long> customerIdList = customerInfoList.stream().map(CustomerInfo::getId).toList();
                customerInfoManager.lambdaUpdate()
                        .in(CustomerInfo::getId, customerIdList)
                        .set(CustomerInfo::getOpenId, req.getOpenId())
                        .update();
                customerCompanyManager.lambdaUpdate()
                        .in(CustomerCompany::getCustomerId, customerIdList)
                        .set(CustomerCompany::getOpenid, req.getOpenId())
                        .update();
            }
            notifyLeadIds.addAll(hitByPhone);
        } else {
            // 3) 完全新增
            Lead lead = CopierCopyUtils.convertTo(req, Lead.class);
            lead.setLeadName("微信小程序线索");
            lead.setSource(DictConstants.CUSTOMER_SOURCE_1);
            lead.setType(DictConstants.LEAD_TYPE_KEEP_FUNDS);
            lead.setStatus(GlobalConstant.Number.ONE_INT);
            lead.setTenantId(GlobalConstant.DEFAULT_TENANT_ID);
            leadManager.save(lead);
            // 同步落 lead_phone 子表（is_primary=1）
            upsertChildPhoneRow(lead.getId(), lead.getPhone(), lead.getOpenId(), lead.getWechat(), "lead_create", GlobalConstant.Number.ONE_INT);
            leadLogService.addLeadLogByCommon(lead.getId(), GlobalConstant.Number.ONE_INT, MessageFormat.format(LogMessage.CREATE_LEAD_LOG, lead.getLeadName()));
            notifyLeadIds.add(lead.getId());
        }
        ensureAppletUser(req);

        // 首次注册时给关联线索的负责人推弹窗"用户 XXX 已成功注册小程序"
        if (isFirstRegistration && CollUtil.isNotEmpty(notifyLeadIds)) {
            String userIdentifier = StringUtils.isNotEmpty(req.getPhone())
                    ? req.getPhone() : req.getContactName();
            for (Long leadId : notifyLeadIds) {
                try {
                    Lead leadForNotify = leadManager.getById(leadId);
                    if (leadForNotify != null && StringUtils.isNotEmpty(leadForNotify.getOwnerUser())) {
                        leadNotificationService.notifyAppletRegister(leadForNotify, userIdentifier);
                    }
                } catch (Exception ex) {
                    log.warn("[uaAdd] 推送小程序注册通知失败 leadId={} err={}", leadId, ex.getMessage());
                }
            }
        }
        return Boolean.TRUE;
    }

    /** 抽出来：保证 AppletUser 存在 */
    private void ensureAppletUser(UaAddLeadReq req) {
        AppletUser appletUser = appletUserManager.lambdaQuery()
                .eq(AppletUser::getOpenId, req.getOpenId())
                .one();
        if(ObjectUtils.isEmpty(appletUser)){
            appletUser = appletUserService.getAppletUser(req.getPhone(), req.getContactName(), req.getOpenId(), GlobalConstant.Number.ZERO_INT);
            appletUserManager.save(appletUser);
        }
    }

    /**
     * 子表 upsert：兼容则补全、冲突则新增。
     *  - 已有"兼容行"（每个字段要么 null 要么与入参相等）且入参能补 null 字段 → UPDATE 已有行
     *  - 完全一致 → 跳过
     *  - 没有兼容行（任一非空字段冲突）→ 插入新行
     */
    private void upsertChildPhoneRow(Long leadId, String phone, String openId, String wechat, String source) {
        upsertChildPhoneRow(leadId, phone, openId, wechat, source, GlobalConstant.Number.ZERO_INT);
    }

    private void upsertChildPhoneRow(Long leadId, String phone, String openId, String wechat, String source, Integer isPrimary) {
        if (leadId == null) return;
        if (StringUtils.isEmpty(phone) && StringUtils.isEmpty(openId) && StringUtils.isEmpty(wechat)) return;

        String np = StringUtils.isEmpty(phone)  ? null : phone;
        String no = StringUtils.isEmpty(openId) ? null : openId;
        String nw = StringUtils.isEmpty(wechat) ? null : wechat;

        // 取本 lead 下所有子表行，在 Java 里挑兼容行（数量极少，性能可接受）
        List<com.qykj.finmaster.customer.lead.entity.LeadPhone> rows = leadPhoneManager.lambdaQuery()
                .eq(com.qykj.finmaster.customer.lead.entity.LeadPhone::getLeadId, leadId)
                .list();

        for (com.qykj.finmaster.customer.lead.entity.LeadPhone r : rows) {
            String rp = StringUtils.isEmpty(r.getPhone())  ? null : r.getPhone();
            String ro = StringUtils.isEmpty(r.getOpenId()) ? null : r.getOpenId();
            String rw = StringUtils.isEmpty(r.getWechat()) ? null : r.getWechat();
            // 任一字段两边都非空且不等 → 不兼容
            boolean compat = fieldCompat(rp, np) && fieldCompat(ro, no) && fieldCompat(rw, nw);
            if (!compat) continue;
            // 完全一致 → 跳过
            boolean exact = java.util.Objects.equals(rp, np)
                    && java.util.Objects.equals(ro, no)
                    && java.util.Objects.equals(rw, nw);
            if (exact) return;
            // 兼容且能补全 → update
            com.qykj.finmaster.customer.lead.entity.LeadPhone update =
                    new com.qykj.finmaster.customer.lead.entity.LeadPhone();
            update.setId(r.getId());
            boolean dirty = false;
            if (rp == null && np != null) { update.setPhone(np);  dirty = true; }
            if (ro == null && no != null) { update.setOpenId(no); dirty = true; }
            if (rw == null && nw != null) { update.setWechat(nw); dirty = true; }
            if (dirty) {
                update.setUpdateTime(new Date());
                leadPhoneManager.updateById(update);
            }
            return;
        }
        com.qykj.finmaster.customer.lead.entity.LeadPhone row = new com.qykj.finmaster.customer.lead.entity.LeadPhone();
        Lead lead = leadManager.getById(leadId);
        row.setTenantId(lead != null ? lead.getTenantId() : null);
        row.setLeadId(leadId);
        row.setPhone(StringUtils.isEmpty(phone) ? null : phone);
        row.setOpenId(StringUtils.isEmpty(openId) ? null : openId);
        row.setWechat(StringUtils.isEmpty(wechat) ? null : wechat);
        row.setIsPrimary(isPrimary);
        row.setSource(source);
        row.setCreateTime(new Date());
        leadPhoneManager.save(row);
    }

    /** 字段兼容判定：两端要么有一方为空，要么相等 */
    private static boolean fieldCompat(String a, String b) {
        if (a == null || b == null) return true;
        return a.equals(b);
    }

    /**
     * 设置推荐码
     * @param req
     * @return
     * @Description
     *  1.存在客户为公海且（客户负责人为空或推荐码的人员为客户当前负责人）的时候 修改客户的负责人且转为私海客户
     *  2.如果线索被删除，则代表已转化为客户（现在转化客户不会再删除线索，之前会存在这种情况），则直接修改客户负责人即可，不必再给线索赋值
     *  3.若果此时不存客户，则会在客户添加公司的时候创建客户并读取线索的推荐码添加负责人信息，此时不需要处理
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean uaAddRecommendCode(AddRecommendCodeReq req) {
        if(StringUtils.isEmpty(req.getOpenId()) && ObjectUtils.isEmpty(req.getRecommendCode())){
            return Boolean.FALSE;
        }
        User user = userManager.lambdaQuery()
                .eq(User::getId, req.getRecommendCode())
                .one();
        // 存在线索信息且线索负责人为空的时候 修改线索的负责人
        // 先按主表 openId 找；找不到再通过子表反查（兼容合并后 openId 落到子表的场景）
        Lead lead = leadManager.lambdaQuery()
                .eq(Lead::getOpenId, req.getOpenId())
                .one();
        if (ObjectUtils.isEmpty(lead)) {
            List<Long> hitLeadIds = leadPhoneMapper.selectLeadIdsByContactInPhoneTable(null, req.getOpenId(), null);
            if (CollUtil.isNotEmpty(hitLeadIds)) {
                lead = leadManager.getById(hitLeadIds.get(0));
            }
        }
        if(ObjectUtils.isNotEmpty(lead)){
            LambdaUpdateWrapper<Lead> wrapper = Wrappers.lambdaUpdate();
            wrapper.set(Lead::getRecommendCode, req.getRecommendCode());
            if(ObjectUtils.isNotEmpty(user) && StringUtils.isEmpty(lead.getOwnerUser()) && StringUtils.isEmpty(lead.getOwnerUserName())){
                wrapper.set(Lead::getOwnerUser, user.getUsername())
                        .set(Lead::getOwnerUserName, user.getRealName());
            }
            wrapper.eq(Lead::getId, lead.getId());
            leadManager.update(wrapper);
        }

        // 客户：先按 openId 找；找不到再走"openId 子表反查 leadId → 默认承接客户"路径
        CustomerInfo customerInfo = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getOpenId, req.getOpenId())
                .one();
        if (ObjectUtils.isEmpty(customerInfo)) {
            List<com.qykj.finmaster.customer.lead.entity.LeadPhone> rows = leadPhoneManager.lambdaQuery()
                    .eq(com.qykj.finmaster.customer.lead.entity.LeadPhone::getOpenId, req.getOpenId())
                    .list();
            for (com.qykj.finmaster.customer.lead.entity.LeadPhone r : rows) {
                if (r.getDefaultCustomerId() != null) {
                    customerInfo = customerInfoManager.getById(r.getDefaultCustomerId());
                    if (customerInfo != null) break;
                }
            }
            // 默认承接客户为空时，取这些 leadId 下任一客户
            if (customerInfo == null && CollUtil.isNotEmpty(rows)) {
                List<Long> leadIds = rows.stream()
                        .map(com.qykj.finmaster.customer.lead.entity.LeadPhone::getLeadId)
                        .filter(java.util.Objects::nonNull).distinct().toList();
                if (CollUtil.isNotEmpty(leadIds)) {
                    customerInfo = customerInfoManager.lambdaQuery()
                            .in(CustomerInfo::getLeadId, leadIds)
                            .orderByAsc(CustomerInfo::getCreateTime)
                            .last("LIMIT 1")
                            .one();
                }
            }
        }
        // 存在客户信息且客户在公海
        if(ObjectUtils.isNotEmpty(user) && ObjectUtils.isNotEmpty(customerInfo) && customerInfo.getCustomerType() == GlobalConstant.Number.TWO_INT){
            // 客户负责人为空或推荐码的人员为客户当前负责人的时候 修改客户的负责人且转为私海客户
            if(StringUtils.isEmpty(customerInfo.getOwnerUser())
                || StringUtils.isEmpty(customerInfo.getOwnerUserName())
                || user.getUsername().equals((customerInfo.getOwnerUser()))){
                customerInfoManager.lambdaUpdate()
                        .set(CustomerInfo::getOwnerUser, user.getUsername())
                        .set(CustomerInfo::getOwnerUserName, user.getRealName())
                        .set(CustomerInfo::getCustomerType, GlobalConstant.Number.ONE_INT)
                        .eq(CustomerInfo::getId, customerInfo.getId())
                        .update();
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 释放线索
     * @param leadList
     * @param ownerUser
     * @param ownerUserName
     * @param loginUser
     */
    @Transactional(rollbackFor = Exception.class)
    public void releaseLead(List<Lead> leadList, String ownerUser, String ownerUserName, LoginUser loginUser){
        List<Long> idList = leadList.stream().map(Lead::getId).toList();
        leadManager.lambdaUpdate()
                .set(Lead::getOwnerUser, ownerUser)
                .set(Lead::getOwnerUserName, ownerUserName)
                .set(Lead::getUpdateTime, new Date())
                .set(Lead::getUpdateBy, loginUser.getUsername())
                .set(Lead::getUpdateByName, loginUser.getRealName())
                .in(Lead::getId, idList)
                .update();

        //分配线索日志
        leadList.forEach(item -> {
            // 添加释放日志
            leadLogService.addLeadLogByCommon(item.getId(), GlobalConstant.Number.THREE_INT,
                    MessageFormat.format(LogMessage.RELEASE_LEAD_LOG, item.getOwnerUserName(), loginUser.getRealName()),
                    loginUser.getUserId(), loginUser.getRealName());
            // 客户转入公海
            dealCustomer(item.getId(), ownerUser, ownerUserName, loginUser, Boolean.TRUE);
        });
    }
}
