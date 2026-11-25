package com.qykj.finmaster.customer.company.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.github.pagehelper.util.StringUtil;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.FileConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.FileUtils;
import com.qykj.finmaster.common.utils.StringUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.common.utils.third.FinancialUtils;
import com.qykj.finmaster.contract.domain.ContractDetail;
import com.qykj.finmaster.contract.manager.ContractDetailManager;
import com.qykj.finmaster.customer.company.domain.*;
import com.qykj.finmaster.customer.company.domain.req.*;
import com.qykj.finmaster.customer.company.domain.resp.*;
import com.qykj.finmaster.customer.company.manager.impl.*;
import com.qykj.finmaster.customer.company.mapper.CompanyServiceMapper;
import com.qykj.finmaster.customer.customerInfo.domain.CustomerInfo;
import com.qykj.finmaster.customer.customerInfo.manager.impl.CustomerInfoManagerImpl;
import com.qykj.finmaster.order.domain.Order;
import com.qykj.finmaster.order.domain.OrderDetail;
import com.qykj.finmaster.order.manager.impl.OrderDetailManagerImpl;
import com.qykj.finmaster.order.manager.impl.OrderManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.dict.service.SysDictDataServiceImpl;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @Author zhb
 * @Date 2025-08-15
 * @Description
 */
@Service
@lombok.extern.slf4j.Slf4j
public class CompanyServiceServiceImpl {

    @Autowired
    private CompanyServiceMapper companyServiceMapper;
    @Autowired
    private SysDictDataServiceImpl sysDictDataService;
    @Autowired
    private OrderDetailManagerImpl orderDetailManager;
    @Autowired
    private CompanyServiceManagerImpl companyServiceManager;
    @Autowired
    private CompanyRecordManagerImpl companyRecordManager;
    @Autowired
    private CompanyServiceRenewManagerImpl companyServiceRenewManager;
    @Autowired
    private OrderManagerImpl orderManager;
    @Autowired
    private ContractDetailManager contractDetailManager;
    @Autowired
    private CustomerInfoManagerImpl customerInfoManager;
    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;
    @Autowired
    private CompanyRecordFileManagerImpl companyRecordFileManager;
    @Autowired
    private BankStatementService bankStatementService;
    @Autowired
    private com.qykj.finmaster.system.bussiness.manager.SysBusinessManager sysBusinessManager;

    /**
     * 查询存在服务公司列表份分页
     * @param req
     * @return
     */
    public List<GetCompanyListWithServiceResp> getCompanyListWithService(GetCompanyListWithServiceReq req) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        // bankStatementUploaded 过滤依赖 statsPeriod，前端未传时回填默认账期，避免 EXISTS 子查询拿不到 period
        if (req.getBankStatementUploaded() != null && StringUtils.isEmpty((String) params.get("statsPeriod"))) {
            params.put("statsPeriod", getDefaultPeriod());
        }
        List<GetCompanyListWithServiceResp> respList = companyServiceMapper.getCompanyListWithService(params);
        if(CollUtil.isEmpty(respList)){
            return Collections.emptyList();
        }
        Map<String, String> dictDataMap = this.getDictDataMap(DictConstants.COMPANY_TYPE);
        //获取订单明细数据
        Set<Long> companyIdSet = respList.stream().map(GetCompanyListWithServiceResp::getId).collect(Collectors.toSet());
        Map<Long, List<OrderDetail>> orderDetailMap = getOrderDetailListMap(companyIdSet);
        // 聚合每家公司的业务类型名称（company_service.business_id -> sys_business.business_name）
        Map<Long, String> companyBusinessNamesMap = getCompanyBusinessNamesMap(companyIdSet);
        //获取评论数据
        Map<Long, List<CompanyRecord>> companyRecordMap = getCompanyRecordList(companyIdSet);
        //获取银行流水上传状态（含 noStatement 标记，三态：0 未上传 / 1 已上传 / 2 无流水）
        String statsPeriod = StringUtils.isNotEmpty(req.getStatsPeriod())
                ? req.getStatsPeriod() : getDefaultPeriod();
        Map<Long, com.qykj.finmaster.customer.company.domain.CompanyBankStatementUpload> bankStateMap =
                bankStatementService.getStateMap(companyIdSet, statsPeriod);
        //获取账套数据
        List<String> taxNumberList = respList.stream()
                .filter(f -> StringUtil.isNotEmpty(f.getTaxNumber()))
                .map(GetCompanyListWithServiceResp::getTaxNumber)
                .distinct().collect(Collectors.toList());
        Map<String, JSONObject> collect = getAccountSetMap(taxNumberList);

        respList.forEach(item -> {
            if(CollUtil.isNotEmpty(dictDataMap) && StringUtils.isNotEmpty(dictDataMap.get(item.getCompanyType()))){
                item.setCompanyTypeName(dictDataMap.get(item.getCompanyType()));
            }
            if(CollUtil.isNotEmpty(orderDetailMap) && CollUtil.isNotEmpty(orderDetailMap.get(item.getId()))){
                item.setServiceSum(orderDetailMap.get(item.getId()).size());
                item.setPayAmountSum(orderDetailMap.get(item.getId()).stream()
                        .filter(f -> ObjectUtils.isNotEmpty(f.getAmount()))
                        .map(detail -> detail.getUnitPrice().multiply(new BigDecimal(detail.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
            }
            if(CollUtil.isNotEmpty(collect) && ObjectUtils.isNotEmpty(collect.get(item.getTaxNumber()))){
                item.setIsHavaAccountSet(GlobalConstant.Number.ONE_INT);
            }else{
                item.setIsHavaAccountSet(GlobalConstant.Number.ZERO_INT);
            }
            if(CollUtil.isNotEmpty(companyRecordMap) && CollUtil.isNotEmpty(companyRecordMap.get(item.getId()))){
                List<CompanyRecord> companyRecordList = companyRecordMap.get(item.getId())
                        .stream()
                        .sorted(Comparator.comparing(CompanyRecord::getCreateTime).reversed()).toList();
                item.setNewFollowContent(companyRecordList.get(GlobalConstant.Number.ZERO_INT).getFollowContent());
            }
            // 银行流水上传状态：0 未上传 / 1 已上传 / 2 无流水
            com.qykj.finmaster.customer.company.domain.CompanyBankStatementUpload st = bankStateMap.get(item.getId());
            int state = 0;
            if (st != null) {
                if (st.getUploadCount() != null && st.getUploadCount() > 0) {
                    state = 1;
                } else if (st.getNoStatement() != null && st.getNoStatement() == 1) {
                    state = 2;
                }
            }
            item.setBankStatementUploaded(state);
            // 业务类型名称
            item.setBusinessNames(companyBusinessNamesMap.get(item.getId()));
        });
        return respList;
    }

    /**
     * 按公司聚合业务类型名称：扫一次 company_service + sys_business，返回 companyId → 业务名（多业务用「，」分隔）。
     * 整批一次查完，避免 N+1。
     */
    private Map<Long, String> getCompanyBusinessNamesMap(Set<Long> companyIdSet) {
        if (CollUtil.isEmpty(companyIdSet)) return Collections.emptyMap();
        List<CompanyService> csList = companyServiceManager.lambdaQuery()
                .in(CompanyService::getCompanyId, companyIdSet)
                .isNotNull(CompanyService::getBusinessId)
                .list();
        if (CollUtil.isEmpty(csList)) return Collections.emptyMap();
        Set<Long> businessIdSet = csList.stream()
                .map(CompanyService::getBusinessId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (businessIdSet.isEmpty()) return Collections.emptyMap();
        Map<Long, String> businessNameMap = sysBusinessManager.lambdaQuery()
                .in(com.qykj.finmaster.system.bussiness.domain.SysBusiness::getId, businessIdSet)
                .list().stream()
                .filter(b -> b != null && b.getId() != null && StringUtils.isNotEmpty(b.getBusinessName()))
                .collect(Collectors.toMap(
                        com.qykj.finmaster.system.bussiness.domain.SysBusiness::getId,
                        com.qykj.finmaster.system.bussiness.domain.SysBusiness::getBusinessName,
                        (a, b) -> a));
        // 按 companyId 分组，去重保序后拼成单字符串
        Map<Long, java.util.LinkedHashSet<String>> grouped = new java.util.HashMap<>();
        for (CompanyService cs : csList) {
            String name = businessNameMap.get(cs.getBusinessId());
            if (StringUtils.isEmpty(name)) continue;
            grouped.computeIfAbsent(cs.getCompanyId(), k -> new java.util.LinkedHashSet<>()).add(name);
        }
        Map<Long, String> result = new java.util.HashMap<>();
        grouped.forEach((cid, names) -> result.put(cid, String.join("，", names)));
        return result;
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

    private Map<String, String> getDictDataMap(List<String> codeList){
        Map<String, String> dataMap = sysDictDataService.getDictDataMapByTypeCode(codeList);
        return dataMap;
    }

    /**
     * 获取订单明细数据
     * @param companyIdSet
     * @return
     */
    private Map<Long, List<OrderDetail>> getOrderDetailListMap(Set<Long> companyIdSet){
        List<CompanyService> companyMaintainList = companyServiceManager.lambdaQuery()
                .in(CompanyService::getCompanyId, companyIdSet)
                .list();
        Map<Long, List<OrderDetail>> collect = new HashMap<>();
        if(CollUtil.isNotEmpty(companyMaintainList)) {
            Set<Long> contractIds = companyMaintainList.stream().map(CompanyService::getSourceId).filter(ObjectUtils::isNotEmpty).collect(Collectors.toSet());
            if (CollUtil.isEmpty(contractIds)) {
                return collect;
            }
            List<ContractDetail> contractDetails = contractDetailManager.lambdaQuery()
                    .in(ContractDetail::getContractId, contractIds)
                    .isNotNull(ContractDetail::getOrderDetailId)
                    .list();
            if (CollUtil.isEmpty(contractDetails)) {
                return collect;
            }
            Set<Long> orderDetailIdList = contractDetails.stream().map(ContractDetail::getOrderDetailId).collect(Collectors.toSet());
            List<OrderDetail> orderDetailList = orderDetailManager.lambdaQuery()
                    .in(OrderDetail::getId, orderDetailIdList)
                    .list();
            if (CollUtil.isNotEmpty(orderDetailList)) {
                collect = orderDetailList.stream().collect(Collectors.groupingBy(OrderDetail::getCompanyId));
            }
        }
        return collect;
    }

    /**
     * 查询账套
     * @param taxNumberList
     * @return
     */
    private Map<String, JSONObject> getAccountSetMap(List<String> taxNumberList){
        Map<String, JSONObject> collect = new HashMap<>();
        if(CollUtil.isNotEmpty(taxNumberList)){
            JSONArray dataJson = FinancialUtils.getGetAccountSetsList(taxNumberList);
            if(CollUtil.isNotEmpty(dataJson)){
                collect = dataJson.stream()
                        .filter(f -> ObjectUtils.isNotEmpty(f) && org.apache.commons.lang3.StringUtils.isNotEmpty(((JSONObject) f).getString("creditCode")))
                        .collect(Collectors.toMap(item -> ((JSONObject) item).getString("creditCode"),item ->  (JSONObject) item, (k, v) -> v));
            }
        }
        return collect;
    }

    /**
     * 获取评论数据
     * @param companyIdSet
     * @return
     */
    private Map<Long, List<CompanyRecord>> getCompanyRecordList(Set<Long> companyIdSet){
        Map<Long, List<CompanyRecord>> collect = new HashMap<>();
        if(CollUtil.isNotEmpty(companyIdSet)){
            List<CompanyRecord> companyRecordList = companyRecordManager.lambdaQuery()
                    .in(CompanyRecord::getCompanyId, companyIdSet)
                    .list();
            if(CollUtil.isNotEmpty(companyRecordList)){
                collect = companyRecordList.stream().collect(Collectors.groupingBy(CompanyRecord::getCompanyId));
            }
        }
        return collect;
    }

    /**
     * 查询存在服务公司维护列表份分页
     * @param req
     * @return
     */
    public List<GetCompanyRecordListWithServiceResp> getCompanyRecordListWithService(GetCompanyRecordListWithServiceReq req) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();

        List<GetCompanyRecordListWithServiceResp> respList = companyServiceMapper.getCompanyRecordListWithService(params);
        if(CollUtil.isEmpty(respList)){
            return Collections.emptyList();
        }
        //获取沟通记录
        Set<Long> companyIdSet = respList.stream().map(GetCompanyRecordListWithServiceResp::getId).collect(Collectors.toSet());
        Map<Long, List<CompanyRecord>> companyRecordMap = getCompanyRecordList(companyIdSet);
        respList.forEach(item -> {
            if(CollUtil.isNotEmpty(companyRecordMap) && CollUtil.isNotEmpty(companyRecordMap.get(item.getId()))){
                List<CompanyRecord> companyRecordListDesc = companyRecordMap.get(item.getId())
                        .stream()
                        .sorted(Comparator.comparing(CompanyRecord::getCreateTime).reversed()).toList();
                CompanyRecord companyRecordDesc = companyRecordListDesc.get(GlobalConstant.Number.ZERO_INT);
                item.setNewFollowContent(companyRecordDesc.getFollowContent());
                item.setNewFollowTime(companyRecordDesc.getFollowTime());
                item.setNextFollowTime(companyRecordDesc.getNextFollowTime());
                List<CompanyRecord> companyRecordListAsc = companyRecordMap.get(item.getId())
                        .stream()
                        .sorted(Comparator.comparing(CompanyRecord::getCreateTime)).toList();
                CompanyRecord companyRecordAsc = companyRecordListAsc.get(GlobalConstant.Number.ZERO_INT);
                item.setFirstFollowTime(companyRecordAsc.getFollowTime());
            }
        });

        return respList;
    }

    /**
     * 查询存在服务公司列表续费分页
     * @param req
     * @return
     */
    /** 续费任务详情专用：按 companyId 查该公司全部服务（绕过数据权限 / 分页） */
    public List<GetCompanyRenewListWithServiceResp> listAllServicesByCompanyIdForRenewal(Long companyId) {
        if (companyId == null) return Collections.emptyList();
        List<GetCompanyRenewListWithServiceResp> respList = companyServiceMapper.listAllServicesByCompanyIdForRenewal(companyId);
        if (CollUtil.isEmpty(respList)) return Collections.emptyList();
        Map<String, String> dictDataMap = this.getDictDataMap(DictConstants.COMPANY_TYPE);
        respList.forEach(item -> {
            if (CollUtil.isNotEmpty(dictDataMap) && StringUtils.isNotEmpty(dictDataMap.get(item.getCompanyType()))) {
                item.setCompanyTypeName(dictDataMap.get(item.getCompanyType()));
            }
            if (ObjectUtils.isNotEmpty(item.getEffectiveDate()) && ObjectUtils.isNotEmpty(item.getActualEndDate())) {
                Integer month = DateUtils.localDateIntervalMonth(item.getEffectiveDate(), LocalDate.now(), Boolean.FALSE);
                item.setServiceInterval(month);
                Integer residue = DateUtils.localDateIntervalMonth(LocalDate.now(), item.getActualEndDate(), Boolean.FALSE);
                item.setResidueServiceInterval(residue);
            }
        });
        return respList;
    }

    public List<GetCompanyRenewListWithServiceResp> getCompanyRenewListWithService(GetCompanyRenewListWithServiceReq req) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        List<GetCompanyRenewListWithServiceResp> respList = companyServiceMapper.getCompanyRenewListWithService(params);
        if(CollUtil.isEmpty(respList)){
            return Collections.emptyList();
        }
        Map<String, String> dictDataMap = this.getDictDataMap(DictConstants.COMPANY_TYPE);
        respList.forEach(item -> {
            if(CollUtil.isNotEmpty(dictDataMap) && StringUtils.isNotEmpty(dictDataMap.get(item.getCompanyType()))){
                item.setCompanyTypeName(dictDataMap.get(item.getCompanyType()));
            }

            if(ObjectUtils.isNotEmpty(item.getEffectiveDate()) && ObjectUtils.isNotEmpty(item.getActualEndDate())){
                Integer month = DateUtils.localDateIntervalMonth(item.getEffectiveDate(), LocalDate.now(), Boolean.FALSE);
                item.setServiceInterval(month);
                Integer residue = DateUtils.localDateIntervalMonth(LocalDate.now(), item.getActualEndDate(), Boolean.FALSE);
                item.setResidueServiceInterval(residue);
            }
        });

        return respList;
    }

    /**
     * 续费统计：按 company_service.actual_end_date 的年月聚合，返回区间内每月【公司数量/合同金额】。
     * startMonth/endMonth 为 YYYY-MM，可空：缺省取当年 01~12；startMonth→当月首日、endMonth→当月末日；
     * mapper 只返回有数据月份，这里按区间补全所有月份（缺失补 0），按 month 升序返回。
     */
    public List<RenewalStatRow> getRenewalStats(String startMonth, String endMonth,
                                                List<String> butlerOwnerUserName, List<String> serviceName) {
        int currentYear = java.time.Year.now().getValue();
        java.time.YearMonth startYm = StringUtils.isNotEmpty(startMonth)
                ? java.time.YearMonth.parse(startMonth)
                : java.time.YearMonth.of(currentYear, 1);
        java.time.YearMonth endYm = StringUtils.isNotEmpty(endMonth)
                ? java.time.YearMonth.parse(endMonth)
                : java.time.YearMonth.of(currentYear, 12);
        // 容错：起止反了则交换，保证区间正向
        if (startYm.isAfter(endYm)) {
            java.time.YearMonth tmp = startYm;
            startYm = endYm;
            endYm = tmp;
        }
        LocalDate start = startYm.atDay(1);
        LocalDate end = endYm.atEndOfMonth();

        List<RenewalStatRow> dbRows = companyServiceMapper.getRenewalStats(start, end, butlerOwnerUserName, serviceName);
        Map<String, RenewalStatRow> byMonth = new HashMap<>();
        if (CollUtil.isNotEmpty(dbRows)) {
            for (RenewalStatRow row : dbRows) {
                if (row != null && row.getMonth() != null) {
                    byMonth.put(row.getMonth(), row);
                }
            }
        }
        // 按区间逐月补全（缺失月份补 0），保证返回连续月份
        List<RenewalStatRow> result = new ArrayList<>();
        for (java.time.YearMonth ym = startYm; !ym.isAfter(endYm); ym = ym.plusMonths(1)) {
            String monthStr = ym.toString(); // YYYY-MM
            RenewalStatRow row = byMonth.get(monthStr);
            if (row == null) {
                row = new RenewalStatRow(monthStr, 0L, BigDecimal.ZERO);
            } else if (row.getContractAmount() == null) {
                row.setContractAmount(BigDecimal.ZERO);
            }
            result.add(row);
        }
        return result;
    }

    /**
     * 续费统计下钻明细：给定 month（YYYY-MM），返回该月（按 actual_end_date）到期的公司服务明细。
     * 过滤口径与 {@link #getRenewalStats} 完全一致（公司级 + 服务级排除 + 区间），按服务逐行。
     */
    public List<RenewalStatDetailRow> getRenewalStatDetail(String month,
                                                           List<String> butlerOwnerUserName, List<String> serviceName) {
        if (StringUtils.isEmpty(month)) {
            return Collections.emptyList();
        }
        java.time.YearMonth ym = java.time.YearMonth.parse(month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<RenewalStatDetailRow> rows = companyServiceMapper.getRenewalStatDetail(start, end, butlerOwnerUserName, serviceName);
        return rows == null ? Collections.emptyList() : rows;
    }

    /**
     * 添加公司维护记录
     *
     * @param req
     * @param fileList
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addCompanyRecord(AddCompanyRecordReq req, List<MultipartFile> fileList, LoginUser loginUser) {
        CompanyRecord companyRecord = CopierCopyUtils.convertTo(req, CompanyRecord.class);
        companyRecord.setCreateTime(new Date());
        companyRecord.setUpdateTime(new Date());

        if(ObjectUtils.isNotEmpty(loginUser)) {
            companyRecord.setCreateBy(loginUser.getUsername());
            companyRecord.setCreateByName(loginUser.getRealName());
            companyRecord.setUpdateBy(loginUser.getUsername());
            companyRecord.setUpdateByName(loginUser.getRealName());
        }
        //保存维护信息
        companyRecordManager.save(companyRecord);
        //保存图片信息
        if(CollUtil.isNotEmpty(fileList)){
            fileList.forEach(item -> {
                this.uploadFile(item, FileConstants.FILE_TYPE_COMPANY_RECORD, companyRecord.getId(), loginUser);
            });
        }
        return Boolean.TRUE;
    }


    /**
     * 文件上传
     * @param file
     * @param companyRecordId
     * @param loginUser
     */
    @Transactional(rollbackFor = Exception.class)
    public void uploadFile(MultipartFile file, String uploadType, Long companyRecordId, LoginUser loginUser){
        //源文件名
        String originalFileName = file.getOriginalFilename();
        //文件后缀
        String fileSuffix = FileNameUtil.getSuffix(originalFileName);
        // 获取文件保存路径
        String savePath = FileUtils.getSavePath(uploadType);
        // 获取文件保存名
        String saveFileName = FileUtils.getSaveFileName(uploadType, fileSuffix);
        //文件上传至服务器
        FileUtils.uploadFile(file, savePath, saveFileName);
        CompanyRecordFile companyRecordFile = new CompanyRecordFile();
        companyRecordFile.setCompanyRecordId(companyRecordId);
        companyRecordFile.setFileName(saveFileName);
        companyRecordFile.setOriginalFileName(originalFileName);
        companyRecordFile.setFilePath(FileConstants.FILE_GET_COMPANY_RECORD + saveFileName);
        companyRecordFile.setFileSuffix(fileSuffix);
        companyRecordFile.setUploadType(uploadType);
        companyRecordFile.setCreateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)) {
            companyRecordFile.setCreateBy(loginUser.getUsername());
            companyRecordFile.setCreateByName(loginUser.getRealName());
        }
        companyRecordFileManager.save(companyRecordFile);
    }

    /**
     * 获取公司历史维护记录
     * @param req
     * @return
     */
    public List<GetCompanyRecordHistoryPageResp> getCompanyRecordHistoryPage(GetCompanyRecordHistoryPageReq req) {
        if(ObjectUtils.isEmpty(req)){
            return Collections.emptyList();
        }
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        List<GetCompanyRecordHistoryPageResp> companyRecordList = companyRecordManager.getBaseMapper().getCompanyRecordHistoryPage(params);
        if(CollUtil.isEmpty(companyRecordList)){
            return Collections.emptyList();
        }
        List<Long> companyRecordIdList = companyRecordList.stream().map(GetCompanyRecordHistoryPageResp::getId).toList();
        List<CompanyRecordFile> companyRecordFileList = companyRecordFileManager.lambdaQuery()
                .in(CompanyRecordFile::getCompanyRecordId, companyRecordIdList)
                .list();
        if(CollUtil.isNotEmpty(companyRecordFileList)){
            Map<Long, List<CompanyRecordFile>> collect = companyRecordFileList.stream().collect(Collectors.groupingBy(CompanyRecordFile::getCompanyRecordId));
            companyRecordList.forEach(item -> {
                if(CollUtil.isNotEmpty(collect.get(item.getId()))){
                    List<CompanyRecordFile> fileList = collect.get(item.getId());
                    item.setFilePathList(fileList.stream().map(CompanyRecordFile::getFilePath).toList());
                }
            });
        }
        return companyRecordList;
    }

    /**
     * 公司服务续费
     *
     * @param req
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addCompanyRenew(AddCompanyRenewReq req, LoginUser loginUser) {
        CompanyServiceRenew companyServiceRenew = CopierCopyUtils.convertTo(req, CompanyServiceRenew.class);
        companyServiceRenew.setCreateTime(new Date());
        companyServiceRenew.setUpdateTime(new Date());

        if(ObjectUtils.isNotEmpty(loginUser)) {
            companyServiceRenew.setCreateBy(loginUser.getUsername());
            companyServiceRenew.setCreateByName(loginUser.getRealName());
            companyServiceRenew.setUpdateBy(loginUser.getUsername());
            companyServiceRenew.setUpdateByName(loginUser.getRealName());
        }
        companyServiceRenewManager.save(companyServiceRenew);
        // 未续费(type=3)不更新服务实际结束时间
        if (!Integer.valueOf(3).equals(req.getRenewType())) {
            companyServiceManager.lambdaUpdate()
                    .set(CompanyService::getActualEndDate, req.getRenewEndDate())
                    .eq(CompanyService::getId, req.getCompanyServiceId())
                    .update();
        }
        return Boolean.TRUE;
    }

    /**
     * S3-4 6.3.3 下发续费任务给管家
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean assignRenewalTask(Long companyServiceId, String assigneeUser, String assigneeUserName,
                                     String note, LoginUser loginUser) {
        if (companyServiceId == null) {
            throw new com.qykj.finmaster.common.exception.BusinessException("companyServiceId 不能为空");
        }
        if (assigneeUser == null || assigneeUser.isEmpty()) {
            throw new com.qykj.finmaster.common.exception.BusinessException("请选择责任管家");
        }
        CompanyService cs = companyServiceManager.getById(companyServiceId);
        if (cs == null) {
            throw new com.qykj.finmaster.common.exception.BusinessException("服务不存在");
        }
        companyServiceManager.lambdaUpdate()
                .set(CompanyService::getPendingRenewalAssignee, assigneeUser)
                .set(CompanyService::getPendingRenewalAssigneeName, assigneeUserName)
                .set(CompanyService::getPendingRenewalAssignedAt, java.time.LocalDateTime.now())
                .set(CompanyService::getPendingRenewalNote, note)
                .eq(CompanyService::getId, companyServiceId)
                .update();
        log.info("[续费派工] 主管 {} 把服务 {} 派发给管家 {}",
                loginUser != null ? loginUser.getUsername() : "system", companyServiceId, assigneeUser);
        return Boolean.TRUE;
    }

    /**
     * 我的待办续费任务列表（按 pendingRenewalAssignee = 当前用户筛选）
     */
    public List<CompanyService> listMyRenewalTasks(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUsername() == null) {
            return Collections.emptyList();
        }
        return companyServiceManager.lambdaQuery()
                .eq(CompanyService::getPendingRenewalAssignee, loginUser.getUsername())
                .orderByAsc(CompanyService::getActualEndDate)
                .list();
    }

    /**
     * 取消续费任务派发（清空 pending 字段）
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean cancelRenewalTask(Long companyServiceId, LoginUser loginUser) {
        companyServiceManager.lambdaUpdate()
                .set(CompanyService::getPendingRenewalAssignee, null)
                .set(CompanyService::getPendingRenewalAssigneeName, null)
                .set(CompanyService::getPendingRenewalAssignedAt, null)
                .set(CompanyService::getPendingRenewalNote, null)
                .eq(CompanyService::getId, companyServiceId)
                .update();
        log.info("[续费派工] 取消派发 服务 {} 由 {} 操作",
                companyServiceId, loginUser != null ? loginUser.getUsername() : "system");
        return Boolean.TRUE;
    }

    /**
     * 获取公司服务续费记录
     * @param companyServiceId
     * @return
     */
    public List<GetCompanyRenewHistoryListResp> getCompanyRenewHistoryList(Long companyServiceId) {
        if(ObjectUtils.isEmpty(companyServiceId)){
            return Collections.emptyList();
        }

        List<CompanyServiceRenew> companyServiceRenewList = companyServiceRenewManager.lambdaQuery()
                .eq(CompanyServiceRenew::getCompanyServiceId, companyServiceId)
                .orderByDesc(CompanyServiceRenew::getId)
                .list();
        return CopierCopyUtils.convertTo(companyServiceRenewList, GetCompanyRenewHistoryListResp.class);
    }

    /**
     * 获取公司服务- companyId
     * @param companyId
     * @return
     */
    public List<GetCompanyServiceInfo> getCompanyServiceByCompany(Long companyId) {
        if(ObjectUtils.isEmpty(companyId)){
            return Collections.emptyList();
        }
        List<GetCompanyServiceInfo> respList = companyServiceManager.getBaseMapper()
                .getCompanyServiceByCompany(companyId);
        return respList;
    }

    /**
     * 获取公司服务对应订单（通过合同间接关联：company_service.source_id=合同ID → contract_detail → order_detail → order）
     * @param companyId
     * @return
     */
    public List<GetCompanyServiceOrderInfo> getCompanyServiceOrderByCompany(Long companyId) {
        if (ObjectUtils.isEmpty(companyId)) {
            return Collections.emptyList();
        }
        List<CompanyService> companyServiceList = companyServiceManager.lambdaQuery()
                .eq(CompanyService::getCompanyId, companyId)
                .list();
        if (CollUtil.isEmpty(companyServiceList)) {
            return Collections.emptyList();
        }
        Set<Long> contractIds = companyServiceList.stream()
                .map(CompanyService::getSourceId)
                .filter(ObjectUtils::isNotEmpty)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(contractIds)) {
            return Collections.emptyList();
        }
        List<ContractDetail> contractDetails = contractDetailManager.lambdaQuery()
                .in(ContractDetail::getContractId, contractIds)
                .isNotNull(ContractDetail::getOrderDetailId)
                .list();
        if (CollUtil.isEmpty(contractDetails)) {
            return Collections.emptyList();
        }
        List<Long> orderDetailIds = contractDetails.stream()
                .map(ContractDetail::getOrderDetailId)
                .filter(ObjectUtils::isNotEmpty)
                .distinct()
                .toList();
        List<OrderDetail> orderDetails = orderDetailManager.listByIds(orderDetailIds);
        if (CollUtil.isEmpty(orderDetails)) {
            return Collections.emptyList();
        }
        Set<Long> orderIds = orderDetails.stream().map(OrderDetail::getOrderId).filter(ObjectUtils::isNotEmpty).collect(Collectors.toSet());
        List<Order> orderList = orderManager.lambdaQuery().in(Order::getId, orderIds).list();
        return orderList.stream().map(item -> {
            GetCompanyServiceOrderInfo info = CopierCopyUtils.convertTo(item, GetCompanyServiceOrderInfo.class);
            info.setOrderStatus(item.getStatus());
            info.setOrderAmount(item.getPaidAmount());
            info.setCreateTime(DateUtils.dateToLocalDateTime(item.getCreateTime()));
            return info;
        }).toList();
    }

    /**
     * 获取公司服务所属客户- companyId
     * @param companyId
     * @return
     */
    public GetCompanyServiceCustomerInfo getCompanyServiceCustomerByCompany(Long companyId) {
        GetCompanyServiceCustomerInfo resp = new GetCompanyServiceCustomerInfo();
        if(ObjectUtils.isEmpty(companyId)){
            return resp;
        }
        List<CompanyService> companyServiceList = companyServiceManager.lambdaQuery()
                .eq(CompanyService::getCompanyId, companyId)
                .list();
        if(CollUtil.isEmpty(companyServiceList)){
            return resp;
        }
        // 客户信息
        List<Long> customerIdList = companyServiceList.stream()
                .filter(f -> ObjectUtils.isNotEmpty(f.getCustomerId()))
                .map(CompanyService::getCustomerId)
                .distinct().toList();
        if(CollUtil.isEmpty(customerIdList)){
            return resp;
        }
        CustomerInfo customerInfo = customerInfoManager.lambdaQuery()
                .eq(CustomerInfo::getId, customerIdList.get(GlobalConstant.Number.ZERO_INT))
                .one();

        return CopierCopyUtils.convertTo(customerInfo, GetCompanyServiceCustomerInfo.class);
    }

    /**
     * 获取银行流水上传统计
     * 格式: 记账户xx，银行流水上传xx，未上传xx，完成率xx
     */
    public Map<String, Object> getBankStatementStats(GetCompanyListWithServiceReq req) {
        Map<String, Object> params = req.toBeanMap();
        // 只统计记账户
        params.put("isBookkeeping", GlobalConstant.Number.ONE_INT);
        // 不分页，获取所有记账户
        List<GetCompanyListWithServiceResp> bookkeepingList = companyServiceMapper.getCompanyListWithService(params);

        int bookkeepingCount = bookkeepingList.size();
        int uploadedCount = 0;

        if (CollUtil.isNotEmpty(bookkeepingList)) {
            Set<Long> companyIds = bookkeepingList.stream()
                    .map(GetCompanyListWithServiceResp::getId).collect(Collectors.toSet());
            String period = StringUtils.isNotEmpty(req.getStatsPeriod())
                    ? req.getStatsPeriod()
                    : getDefaultPeriod();
            // "已完成"=已上传 + 无流水，与公司列表 / 数据收集任务进度口径一致
            Map<Long, com.qykj.finmaster.customer.company.domain.CompanyBankStatementUpload> stateMap =
                    bankStatementService.getStateMap(companyIds, period);
            for (var st : stateMap.values()) {
                boolean uploaded = st.getUploadCount() != null && st.getUploadCount() > 0;
                boolean noStatement = st.getNoStatement() != null && st.getNoStatement() == 1;
                if (uploaded || noStatement) uploadedCount++;
            }
        }

        int notUploadedCount = bookkeepingCount - uploadedCount;
        String completionRate = bookkeepingCount > 0
                ? String.format("%.1f%%", (double) uploadedCount / bookkeepingCount * 100)
                : "0%";

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("bookkeepingCount", bookkeepingCount);
        stats.put("uploadedCount", uploadedCount);
        stats.put("notUploadedCount", notUploadedCount);
        stats.put("completionRate", completionRate);
        return stats;
    }

    /**
     * 修改是否记账户
     */
    public void updateBookkeeping(Long companyId, Integer isBookkeeping) {
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getIsBookkeeping, isBookkeeping)
                .eq(CustomerCompany::getId, companyId)
                .update();
    }

    /**
     * 修改双免个体标记
     */
    public void updateDoubleExemption(Long companyId, Integer isDoubleExemption) {
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getIsDoubleExemption, isDoubleExemption)
                .eq(CustomerCompany::getId, companyId)
                .update();
    }

    /**
     * 修改是否个体工商户
     */
    public void updateIndividual(Long companyId, Integer isIndividual) {
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getIsIndividual, isIndividual)
                .eq(CustomerCompany::getId, companyId)
                .update();
    }

    /**
     * 获取默认账期：15号之前=上月，15号及之后=当月
     */
    private String getDefaultPeriod() {
        LocalDate today = LocalDate.now();
        LocalDate period = today.getDayOfMonth() < 15
                ? today.minusMonths(1).withDayOfMonth(1)
                : today.withDayOfMonth(1);
        return period.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * 获取公司服务所属公司- companyId
     * @param companyId
     * @return
     */
    public GetCompanyServiceCompanyInfo getCompanyServiceCompany(Long companyId) {
        GetCompanyServiceCompanyInfo resp = new GetCompanyServiceCompanyInfo();
        if(ObjectUtils.isEmpty(companyId)){
            return resp;
        }
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, companyId)
                .one();
        if(ObjectUtils.isEmpty(customerCompany)){
            return resp;
        }
        resp = CopierCopyUtils.convertTo(customerCompany, GetCompanyServiceCompanyInfo.class);
        Map<String, String> dictDataMap = this.getDictDataMap(DictConstants.COMPANY_TYPE);
        if(CollUtil.isNotEmpty(dictDataMap) && StringUtils.isNotEmpty(dictDataMap.get(resp.getCompanyType()))){
            resp.setCompanyTypeName(dictDataMap.get(resp.getCompanyType()));
        }
        return resp;
    }
}