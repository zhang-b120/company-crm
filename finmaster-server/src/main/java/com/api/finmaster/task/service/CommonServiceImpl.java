package com.qykj.finmaster.task.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.file.FileNameUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.core.domain.constansts.DictConstants;
import com.qykj.finmaster.common.core.domain.constansts.FileConstants;
import com.qykj.finmaster.common.core.domain.constansts.FinancialConstants;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.FileUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.common.utils.third.FinancialUtils;
import com.qykj.finmaster.common.utils.third.req.AddAccountSetMigrationDataReq;
import com.qykj.finmaster.common.utils.third.req.CreateAccountSetReq;
import com.qykj.finmaster.contract.domain.Contract;
import com.qykj.finmaster.contract.manager.ContractManager;
import com.qykj.finmaster.customer.company.domain.CompanyService;
import com.qykj.finmaster.customer.company.domain.CustomerCompany;
import com.qykj.finmaster.customer.company.manager.impl.CompanyServiceManagerImpl;
import com.qykj.finmaster.customer.company.manager.impl.CustomerCompanyManagerImpl;
import com.qykj.finmaster.migration.domain.FinancialDataMigration;
import com.qykj.finmaster.migration.manager.impl.FinancialDataMigrationManagerImpl;
import com.qykj.finmaster.order.domain.OrderDetail;
import com.qykj.finmaster.order.manager.impl.OrderDetailManagerImpl;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.dict.service.SysDictDataServiceImpl;
import com.qykj.finmaster.system.taxPlaceLocation.entity.dto.GetTaxPlaceLocationUserDto;
import com.qykj.finmaster.system.taxPlaceLocation.manager.TaxPlaceLocationUserManager;
import com.qykj.finmaster.system.user.entity.User;
import com.qykj.finmaster.system.user.manager.impl.UserManagerImpl;
import com.qykj.finmaster.system.user.service.UserServiceImpl;
import com.qykj.finmaster.task.domain.CommonTask;
import com.qykj.finmaster.task.domain.CommonTaskDetail;
import com.qykj.finmaster.task.domain.CommonTaskFile;
import com.qykj.finmaster.task.domain.dto.CommonDetailInfo;
import com.qykj.finmaster.task.domain.dto.DealCommonTaskDetailInfo;
import com.qykj.finmaster.task.domain.dto.DealCommonTaskInfo;
import com.qykj.finmaster.task.domain.req.*;
import com.qykj.finmaster.task.domain.resp.*;
import com.qykj.finmaster.task.manager.CommonTaskDetailManager;
import com.qykj.finmaster.task.manager.CommonTaskFileManager;
import com.qykj.finmaster.task.manager.impl.CommonTaskManagerImpl;
import com.qykj.finmaster.task.mapper.CommonTaskMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

// Excel 相关
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * @Author zhb
 * @Date 2025-08-09
 * @Description
 */
@Service
public class CommonServiceImpl {

    @Autowired
    private CommonTaskMapper commonTaskMapper;
    @Autowired
    private CommonTaskManagerImpl commonTaskManager;
    @Autowired
    private CommonTaskDetailManager commonTaskDetailManager;
    @Autowired
    private SysDictDataServiceImpl sysDictDataService;
    @Autowired
    private CustomerCompanyManagerImpl customerCompanyManager;
    @Autowired
    private FinancialDataMigrationManagerImpl financialDataMigrationManager;
    @Autowired
    private OrderDetailManagerImpl orderDetailManager;
    @Autowired
    private ContractManager contractManager;
    @Autowired
    private CompanyServiceManagerImpl companyServiceManager;
    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private CommonTaskFileManager commonTaskFileManager;
    @Autowired
    private UserManagerImpl userManager;
    @Autowired
    private TaxPlaceLocationUserManager taxPlaceLocationUserManager;
    @Autowired
    private com.qykj.finmaster.cs.schedule.service.CsScheduleServiceImpl csScheduleService;

    /**
     * 获取分页数据
     * @param req
     * @return
     */
    public List<GetCommonTaskPageResp> getCommonTaskPage(GetCommonTaskPageReq req) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        if(ObjectUtils.isNotEmpty(req.getStatus())){
            params.put("statusList", Arrays.asList(req.getStatus()));
        }else{
            params.put("statusList", Arrays.asList(DictConstants.COMMON_TASK_STATUS_PENDING, DictConstants.COMMON_TASK_STATUS_PROCESSING));
        }
        List<GetCommonTaskPageResp> commonTaskPage = commonTaskMapper.getCommonTaskPage(params);
        if(CollUtil.isEmpty(commonTaskPage)){
            return Collections.emptyList();
        }
        Map<String, String> statusMap = getDictDataMap(Arrays.asList(DictConstants.COMMON_TASK_STATUS, DictConstants.COMPANY_TYPE));
        Map<String, String> detailStatusMap = getDictDataMap(DictConstants.COMMON_TASK_DETAIL_STATUS);
        //任务明细数据
        List<Long> commonTaskIdList = commonTaskPage.stream().map(GetCommonTaskPageResp::getId).toList();
        Map<Long, List<CommonTaskDetail>> commonTaskDetailMap = getCommonTaskDetailMap(commonTaskIdList);
        commonTaskPage.forEach(item -> {
            if(CollUtil.isNotEmpty(statusMap) && StringUtils.isNotEmpty(statusMap.get(item.getStatus()))){
                item.setStatusName(statusMap.get(item.getStatus()));
            }
            if(CollUtil.isNotEmpty(statusMap) && StringUtils.isNotEmpty(statusMap.get(item.getCompanyType()))){
                item.setCompanyTypeName(statusMap.get(item.getCompanyType()));
            }
            if(CollUtil.isNotEmpty(commonTaskDetailMap) && CollUtil.isNotEmpty(commonTaskDetailMap.get(item.getId()))){
                List<GetCommonDetailBasicDataResp> detailBasicDataRespList = commonTaskDetailMap.get(item.getId()).stream().map(detail -> {
                    GetCommonDetailBasicDataResp detailBasicDataResp = CopierCopyUtils.convertTo(detail, GetCommonDetailBasicDataResp.class);
                    if (CollUtil.isNotEmpty(detailStatusMap) && StringUtils.isNotEmpty(detailStatusMap.get(detail.getStatus()))) {
                        detailBasicDataResp.setStatusName(detailStatusMap.get(detail.getStatus()));
                    }
                    return detailBasicDataResp;
                }).sorted(Comparator.comparing(GetCommonDetailBasicDataResp::getSort)).toList();
                item.setCommonDetailBasicDataList(detailBasicDataRespList);
            }
        });
        return commonTaskPage;
    }

    /**
     * 获取任务流程数据
     * @param commonTaskIdList
     * @return
     */
    private Map<Long, List<CommonTaskDetail>> getCommonTaskDetailMap(List<Long> commonTaskIdList){
        Map<Long, List<CommonTaskDetail>> collect = new HashMap<>();
        if(CollUtil.isNotEmpty(commonTaskIdList)) {
            List<CommonTaskDetail> commonTaskDetailList = commonTaskDetailManager.lambdaQuery()
                    .in(CommonTaskDetail::getTaskId, commonTaskIdList)
                    .list();
            collect = commonTaskDetailList.stream().collect(Collectors.groupingBy(CommonTaskDetail::getTaskId));
        }
        return collect;
    }


    /**
     * 添加任务
     * @param req
     * @param tenantId
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean addCommonTask(AddCommonTaskReq req, Long tenantId, LoginUser loginUser) {
        if(ObjectUtils.isEmpty(req.getServiceId())){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_SERVICE);
        }
        CommonTask commonTask = CopierCopyUtils.convertTo(req, CommonTask.class);
        // 数据校验
        checkCommonTaskData(commonTask.getServiceId(), commonTask.getOwnerUser(),
                commonTask.getSourceNumber(), commonTask.getSourceId(), null);
        commonTask.setTaskNo(getTaskNo());
        commonTask.setTenantId(tenantId);
        commonTask.setCreateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)){
            commonTask.setCreateBy(loginUser.getUsername());
            commonTask.setCreateByName(loginUser.getRealName());
        }
        // 创建任务单当前负责人默认为创建人
        commonTask.setCurrentOwnerUser(commonTask.getCreateBy());
        commonTask.setCurrentOwnerUserName(commonTask.getCreateByName());
        commonTask.setStatus(DictConstants.COMMON_TASK_STATUS_PENDING);

        // 处理排班号源预约
        if (ObjectUtils.isNotEmpty(req.getScheduleSlotId())) {
            com.qykj.finmaster.cs.schedule.domain.CsScheduleSlot slot =
                    csScheduleService.bookSlot(req.getScheduleSlotId());
            commonTask.setScheduleSlotId(slot.getId());
            commonTask.setOwnerUser(slot.getUserAccount());
            commonTask.setOwnerUserName(slot.getUserName());
            commonTask.setCurrentOwnerUser(slot.getUserAccount());
            commonTask.setCurrentOwnerUserName(slot.getUserName());
            // 以 slotTime (HH:mm) + workDate 组合为处理时间
            String timeStr = slot.getWorkDate() + "T" + slot.getSlotTime() + ":00";
            commonTask.setScheduleTime(java.time.LocalDateTime.parse(timeStr));
            commonTask.setWishFinishTime(slot.getWorkDate());
        }

        commonTaskManager.save(commonTask);

        if(CollUtil.isNotEmpty(req.getDetailInfoList())){
            AtomicReference<GetTaxPlaceLocationUserDto> dto = new AtomicReference<>(new GetTaxPlaceLocationUserDto());
            List<CommonTaskDetail> commonTaskDetailList = req.getDetailInfoList().stream()
                    .sorted(Comparator.comparing(item -> item.getSort()))
                    .map(item -> {
                        CommonTaskDetail commonTaskDetail = CopierCopyUtils.convertTo(item, CommonTaskDetail.class);
                        commonTaskDetail.setTaskId(commonTask.getId());
                        // 新增时默认顺序为1的流程为当前流程
                        if (commonTaskDetail.getSort() == GlobalConstant.Number.ONE_INT) {
                            commonTaskDetail.setIsCurrent(GlobalConstant.Number.ONE_INT);
                        }else{
                            commonTaskDetail.setIsCurrent(GlobalConstant.Number.ZERO_INT);
                        }
                        commonTaskDetail.setTenantId(tenantId);
                        commonTaskDetail.setCreateTime(new Date());
                        if (ObjectUtils.isNotEmpty(loginUser)) {
                            commonTaskDetail.setCreateBy(loginUser.getUsername());
                            commonTaskDetail.setCreateByName(loginUser.getRealName());
                        }
                        commonTaskDetail.setStatus(DictConstants.COMMON_TASK_DETAIL_STATUS_PENDING);
                        // 如果流程度责任为空，则总负责人负责此流程
                        if(StringUtils.isEmpty(commonTaskDetail.getOwnerUser()) || StringUtils.isEmpty(commonTaskDetail.getOwnerUser())){
                            commonTaskDetail.setOwnerUser(commonTask.getOwnerUser());
                            commonTaskDetail.setOwnerUserName(commonTask.getOwnerUserName());
                        }
                        //财务流程负责人需要根据创建公司账套的省份赋值
                        if(DictConstants.SERVICE_PROCESS_CREATE_ACCOUNT_SET.equals(item.getProcessCode())){
                            JSONObject jsonObject = JSON.parseObject(item.getTaskData());
                            JSONArray ownArray = jsonObject.getJSONArray("own");
                            Optional<String> province = ownArray.stream()
                                    .map(obj -> (JSONObject) obj)
                                    .filter(data -> "province".equals(data.getString("prop")))
                                    .map(obj -> obj.getString("value"))
                                    .findFirst();
                            dto.set(taxPlaceLocationUserManager.getBaseMapper().getUserByProvince(province.get()));
                            if(ObjectUtils.isEmpty(dto) || ObjectUtils.isEmpty(dto.get().getFinancialUserId())){
                                throw new BusinessException(ResultMsg.COMMON_TASK_NOT_PROVINCE_USER);
                            }
                        }
                        if(DictConstants.SERVICE_PROCESS_FINANCE_DEAL.equals(item.getProcessCode())){
                            commonTaskDetail.setOwnerUser(dto.get().getUserName());
                            commonTaskDetail.setOwnerUserName(dto.get().getRealName());
                        }
                        return commonTaskDetail;
                    }).toList();
            commonTaskDetailManager.saveBatch(commonTaskDetailList);
        }
        // 更新公司服务的管家负责人为任务负责人
        updateCompanyServiceButler(commonTask);

        // 更新合同转任务单状态
        contractManager.lambdaUpdate()
                .set(Contract::getTransferTask, DictConstants.TRANSFER_TASK_TRANSFERRED)
                .eq(Contract::getId, commonTask.getSourceId())
                .update();
        return Boolean.TRUE;
    }

    /**
     * 转任务单时，更新公司服务的管家负责人为任务负责人
     */
    private void updateCompanyServiceButler(CommonTask commonTask) {
        if (ObjectUtils.isEmpty(commonTask.getSourceId()) || ObjectUtils.isEmpty(commonTask.getOwnerUser())) {
            return;
        }
        List<CompanyService> companyServiceList = companyServiceManager.lambdaQuery()
                .eq(CompanyService::getSourceId, commonTask.getSourceId())
                .list();
        if (CollUtil.isNotEmpty(companyServiceList)) {
            for (CompanyService cs : companyServiceList) {
                cs.setButlerOwnerUser(commonTask.getOwnerUser());
                cs.setButlerOwnerUserName(commonTask.getOwnerUserName());
            }
            companyServiceManager.updateBatchById(companyServiceList);
        }
    }

    /**
     * 添加公司服务
     * 来源仅为合同：source_number/source_id 为合同编码/合同ID，不查订单，effective_date/end_date 仅从合同取
     * 开始新事物， 保证不会影响主方法事务管理
     * @param commonTask
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addCompanyService(CommonTask commonTask) {
        try {
            if (ObjectUtils.isEmpty(commonTask.getCompanyId())) {
                return;
            }
            Contract contract = contractManager.getById(commonTask.getSourceId());
            if (ObjectUtils.isEmpty(contract)) {
                return;
            }
            // 续费合同在完成时已强制校验存在公司服务数据，这里不再新增 company_service，只更新转任务单标识
            if (DictConstants.CONTRACT_TYPE_RENEW.equals(contract.getContractType())) {
                contract.setTransferTask(DictConstants.TRANSFER_TASK_TRANSFERRED);
                contractManager.updateById(contract);
                return;
            }
            CompanyService companyService = new CompanyService();
            companyService.setCompanyId(commonTask.getCompanyId());
            companyService.setCustomerId(commonTask.getCustomerId());
            companyService.setSourceNumber(commonTask.getSourceNumber());
            companyService.setSourceId(commonTask.getSourceId());
            companyService.setCommonTaskId(commonTask.getId());
            companyService.setServiceId(commonTask.getServiceId());
            // 业务类型ID：从合同直接取，替代原先的 product_id 语义
            if (ObjectUtils.isNotEmpty(contract.getBusinessId())) {
                companyService.setBusinessId(contract.getBusinessId());
            }
            companyService.setSaleOwnerUser(commonTask.getCreateBy());
            companyService.setSaleOwnerUserName(commonTask.getCreateByName());
            if (ObjectUtils.isNotEmpty(contract.getContractStartTime())) {
                companyService.setEffectiveDate(DateUtils.dateToLocalDateTime(contract.getContractStartTime()).toLocalDate());
            }
            if (ObjectUtils.isNotEmpty(contract.getContractEndTime())) {
                LocalDate endDate = DateUtils.dateToLocalDateTime(contract.getContractEndTime()).toLocalDate();
                companyService.setEndDate(endDate);
                companyService.setActualEndDate(endDate);
            }
            if (ObjectUtils.isEmpty(companyService.getCustomerId()) && ObjectUtils.isNotEmpty(contract.getCompanyId())) {
                CustomerCompany company = customerCompanyManager.getById(contract.getCompanyId());
                if (ObjectUtils.isNotEmpty(company)) {
                    companyService.setCustomerId(company.getCustomerId());
                }
            }
            List<CompanyService> companyServiceList = companyServiceManager.lambdaQuery()
                    .eq(CompanyService::getCompanyId, companyService.getCompanyId())
                    .list();
            if (CollUtil.isNotEmpty(companyServiceList)) {
                CompanyService service = companyServiceList.get(GlobalConstant.Number.ZERO_INT);
                companyService.setButlerOwnerUser(service.getButlerOwnerUser());
                companyService.setButlerOwnerUserName(service.getButlerOwnerUserName());
            } else {
                companyService.setButlerOwnerUser(commonTask.getOwnerUser());
                companyService.setButlerOwnerUserName(commonTask.getOwnerUserName());
            }
            companyServiceManager.save(companyService);
            // 转任务单成功后，将合同转任务单标识更新为 2-已转
            contract.setTransferTask(DictConstants.TRANSFER_TASK_TRANSFERRED);
            contractManager.updateById(contract);
        } catch (Exception e) {
            // 主动捕获异常，不影响主流程
            System.out.println(e.getMessage() + "1111111111" + e);
        }
    }

    /**
     * 获取任务单号
     * @return
     */
    private String getTaskNo(){
        // 生成任务编号：TASK + 年月日时分秒 + 4位随机数
        return "TASK" + String.format("%tY%<tm%<td%<tH%<tM%<tS", new Date()) +
                String.format("%04d", (int)(Math.random() * 10000));
    }

    /**
     * 校验任务数据
     * @param serviceId
     * @param ownerUser
     * @param sourceNumber
     * @param sourceId
     */
    private void checkCommonTaskData(Long serviceId, String ownerUser, String sourceNumber, Long sourceId, Long id){
        //无服务
        if(ObjectUtils.isEmpty(serviceId)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_SERVICE);
        }
        // 负责人由排班号源自动填充，此处不强校验
        //任务缺少来源单据号和来源单据ID
        if(StringUtils.isEmpty(sourceNumber) && ObjectUtils.isEmpty(sourceId)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_SOURCE_NUMBER_OR_ID);
        }
        //根据来源单据号、来源单据ID和服务类型id判断是否已存在任务
        List<CommonTask> commonTaskList = commonTaskManager.lambdaQuery()
                .eq(CommonTask::getServiceId, serviceId)
                .eq(CommonTask::getSourceNumber, sourceNumber)
                .eq(CommonTask::getSourceId, sourceId)
                .ne(ObjectUtils.isNotEmpty(id), CommonTask::getId, id)
                .list();
        if(CollUtil.isNotEmpty(commonTaskList)){
            throw new BusinessException(ResultMsg.COMMON_TASK_EXISTS);
        }
    }

    /**
     * 删除任务
     * @param id
     * @param tenantId
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteCommonTask(Long id, Long tenantId, LoginUser loginUser) {
        // 删除前获取任务（用于判断是否来源于合同并回写 transfer_task）
        CommonTask task = commonTaskManager.getById(id);
        //校验能否删除
        checkUpdateOrDeleteCommonTask(id);
        commonTaskManager.lambdaUpdate()
                .set(CommonTask::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(CommonTask::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateByName, loginUser.getRealName())
                .eq(CommonTask::getId, id)
                .update();

        commonTaskDetailManager.lambdaUpdate()
                .set(CommonTaskDetail::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(CommonTaskDetail::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateByName, loginUser.getRealName())
                .eq(CommonTaskDetail::getTaskId, id)
                .update();
        //删除服务
        deleteCompanyService(id);
        // 若任务来源于合同，将合同转任务单标识回写为 1-是
        if (ObjectUtils.isNotEmpty(task) && ObjectUtils.isNotEmpty(task.getSourceId())) {
            Contract contract = contractManager.getById(task.getSourceId());
            if (ObjectUtils.isNotEmpty(contract)) {
                contract.setTransferTask(DictConstants.TRANSFER_TASK_YES);
                contractManager.updateById(contract);
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 删除服务
     * @param commonTaskId
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteCompanyService(Long commonTaskId) {
        // 主动捕获异常，不影响主流程
        try{
            companyServiceManager.lambdaUpdate()
                    .set(CompanyService::getDeleted, GlobalConstant.Number.ONE_INT)
                    .eq(CompanyService::getCommonTaskId, commonTaskId)
                    .update();
        }catch (Exception e){
            // todo
        }
    }

    /**
     * 检验能否修改或删除任务单
     * @param id
     */
    private void checkUpdateOrDeleteCommonTask(Long id){
        CommonTask commonTask = commonTaskManager.getById(id);
        if(ObjectUtils.isEmpty(commonTask)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        //非待处理状态任务不能删除
        if(!DictConstants.COMMON_TASK_STATUS_PENDING.equals(commonTask.getStatus())){
            throw new BusinessException(ResultMsg.COMMON_TASK_STATUS_NOT_PENDING);
        }
    }

    /**
     * 修改任务
     * @param req
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCommonTask(UpdateCommonTaskReq req, LoginUser loginUser) {
        CommonTask commonTask = CopierCopyUtils.convertTo(req, CommonTask.class);
        // 数据校验
        checkCommonTaskData(commonTask.getServiceId(), commonTask.getOwnerUser(),
                commonTask.getSourceNumber(), commonTask.getSourceId(), commonTask.getId());
        commonTask.setUpdateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)){
            commonTask.setUpdateBy(loginUser.getUsername());
            commonTask.setUpdateByName(loginUser.getRealName());
        }
        commonTaskManager.updateById(commonTask);

        if(CollUtil.isNotEmpty(req.getDetailInfoList())){
            //先删除本次修改没有的明细数据
            List<CommonDetailInfo> commonDetailInfoList = req.getDetailInfoList();
            List<Long> detailIdList = commonDetailInfoList.stream()
                    .filter(f -> ObjectUtils.isNotEmpty(f.getId()))
                    .map(CommonDetailInfo::getId).toList();
            commonTaskDetailManager.lambdaUpdate()
                    .set(CommonTaskDetail::getDeleted, GlobalConstant.Number.ONE_INT)
                    .set(CommonTaskDetail::getUpdateTime, new Date())
                    .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateBy, loginUser.getUsername())
                    .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateByName, loginUser.getRealName())
                    .eq(CommonTaskDetail::getTaskId, req.getId())
                    .notIn(CommonTaskDetail::getId, detailIdList)
                    .update();
            List<CommonTaskDetail> commonTaskDetailList = commonDetailInfoList.stream().map(item -> {
                CommonTaskDetail commonTaskDetail = CopierCopyUtils.convertTo(item, CommonTaskDetail.class);
                if(ObjectUtils.isEmpty(commonTaskDetail.getTaskId())) {
                    commonTaskDetail.setTaskId(commonTask.getId());
                }
                // 新增时默认顺序为1的流程为当前流程
                if (commonTaskDetail.getSort() == GlobalConstant.Number.ZERO_INT) {
                    commonTaskDetail.setIsCurrent(GlobalConstant.Number.ONE_INT);
                }else{
                    commonTaskDetail.setIsCurrent(GlobalConstant.Number.ZERO_INT);
                }
                commonTaskDetail.setUpdateTime(new Date());
                if (ObjectUtils.isNotEmpty(loginUser)) {
                    commonTaskDetail.setUpdateBy(loginUser.getUsername());
                    commonTaskDetail.setUpdateByName(loginUser.getRealName());
                }
                return commonTaskDetail;
            }).toList();
            commonTaskDetailManager.saveOrUpdateBatch(commonTaskDetailList);
        }
        return Boolean.TRUE;
    }

    /**
     * 获取任务详情
     * @param id
     * @return
     */
    public GetCommonTaskInfoResp getCommonTaskInfo(Long id) {
        GetCommonTaskInfoResp resp = new GetCommonTaskInfoResp();
        if(ObjectUtils.isEmpty(id)){
            return resp;
        }
        resp = commonTaskMapper.getCommonTaskInfo(id);
        Map<String, String> detailStatusMap = getDictDataMap(Arrays.asList(DictConstants.COMMON_TASK_DETAIL_STATUS, DictConstants.COMPANY_TYPE, DictConstants.COMMON_TASK_DETAIL_STATUS));
        if(ObjectUtils.isNotEmpty(resp)){
            if(CollUtil.isNotEmpty(detailStatusMap) && StringUtils.isNotEmpty(detailStatusMap.get(resp.getStatus()))){
                resp.setStatusName(detailStatusMap.get(resp.getStatus()));
            }
            if(CollUtil.isNotEmpty(detailStatusMap) && StringUtils.isNotEmpty(detailStatusMap.get(resp.getCompanyType()))){
                resp.setCompanyTypeName(detailStatusMap.get(resp.getCompanyType()));
            }
        }

        List<CommonTaskDetail> commonTaskDetailList = commonTaskDetailManager.lambdaQuery()
                .eq(CommonTaskDetail::getTaskId, id)
                .list();
        if(CollUtil.isNotEmpty(commonTaskDetailList)){
            List<GetCommonTaskDetailInfoResp> detailInfoRespList = commonTaskDetailList.stream().map(item -> {
                GetCommonTaskDetailInfoResp detailInfoResp = CopierCopyUtils.convertTo(item, GetCommonTaskDetailInfoResp.class);
                if(CollUtil.isNotEmpty(detailStatusMap) && StringUtils.isNotEmpty(detailStatusMap.get(detailInfoResp.getStatus()))){
                    detailInfoResp.setStatusName(detailStatusMap.get(detailInfoResp.getStatus()));
                }
                return detailInfoResp;
            }).sorted(Comparator.comparing(GetCommonTaskDetailInfoResp::getSort)).toList();
            resp.setDetailInfoRespList(detailInfoRespList);
        }
        return resp;
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
     * 分配负责人
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean distributeOwnerUser(DistributeTaskOwnerUserReq req, LoginUser loginUser) {
        //基础数据校验
        if(ObjectUtils.isEmpty(req.getId())){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        if(CollUtil.isEmpty(req.getDetailOwnerUserList())){
            return Boolean.TRUE;
        }
        CommonTask commonTask = commonTaskManager.getById(req.getId());
        if(ObjectUtils.isEmpty(commonTask)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        //校验状态
        if(DictConstants.COMMON_TASK_STATUS_COMPLETED.equals(commonTask.getStatus())
                || DictConstants.COMMON_TASK_STATUS_CANCELLED.equals(commonTask.getStatus())
                || DictConstants.COMMON_TASK_STATUS_PUTUP.equals(commonTask.getStatus())){
            throw new BusinessException(ResultMsg.COMMON_TASK_DISTRIBUTE_OWNER_USER_NOT_EXISTS_STATUS);
        }
        //任务负责人校验
        if(!loginUser.getUsername().equals(commonTask.getOwnerUser())){
            throw new BusinessException(ResultMsg.COMMON_TASK_EXECUTE_NOT_EXISTS_GENERAL_OWNER);
        }
        // 根据任务流程执行顺序倒序查询，方便获取任务当前负责人
        List<Long> taskDetailIdList = req.getDetailOwnerUserList().stream().map(DistributeDetailOwnerUserReq::getId).collect(Collectors.toList());
        List<CommonTaskDetail> commonTaskDetailList = commonTaskDetailManager.lambdaQuery()
                .in(CommonTaskDetail::getId, taskDetailIdList)
                .eq(CommonTaskDetail::getHandMethod, GlobalConstant.Number.ZERO_INT)
                .eq(CommonTaskDetail::getStatus, DictConstants.COMMON_TASK_DETAIL_STATUS_PENDING)
                .orderByDesc(CommonTaskDetail::getSort)
                .list();
        //任务当前负责人取值为第一个人工待处理的任务流程负责人
        String currentOwnerUser = null;
        String currentOwnerUserName = null;
        if(CollUtil.isNotEmpty(commonTaskDetailList)) {
            Map<Long, DistributeDetailOwnerUserReq> detailOwnerUserMap = req.getDetailOwnerUserList()
                    .stream().collect(Collectors.toMap(DistributeDetailOwnerUserReq::getId, item -> item));
            for (CommonTaskDetail item : commonTaskDetailList) {
                DistributeDetailOwnerUserReq detailOwnerUser = detailOwnerUserMap.get(item.getId());
                commonTaskDetailManager.lambdaUpdate()
                        .set(CommonTaskDetail::getOwnerUser, detailOwnerUser.getOwnerUser())
                        .set(CommonTaskDetail::getOwnerUserName, detailOwnerUser.getOwnerUserName())
                        .set(CommonTaskDetail::getUpdateTime, new Date())
                        .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateBy, loginUser.getUsername())
                        .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateByName, loginUser.getRealName())
                        .eq(CommonTaskDetail::getId, item.getId())
                        .update();
                currentOwnerUser = detailOwnerUser.getOwnerUser();
                currentOwnerUserName = detailOwnerUser.getOwnerUserName();
                //财务处理流程更换负责人时修改账套归属人
                if(DictConstants.SERVICE_PROCESS_FINANCE_DEAL.equals(item.getProcessCode())){
                    this.updateAccountSetsUser(detailOwnerUser.getOwnerUser(), item.getOwnerUser(), commonTask.getCompanyId());
                }
            }
        }
        //如果存在当前负责人则进行修改
        if(StringUtils.isNotEmpty(currentOwnerUser)){
            commonTaskManager.lambdaUpdate()
                    .set(CommonTask::getCurrentOwnerUser, currentOwnerUser)
                    .set(CommonTask::getCurrentOwnerUserName, currentOwnerUserName)
                    .set(CommonTask::getUpdateTime, new Date())
                    .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateBy, loginUser.getUsername())
                    .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateByName, loginUser.getRealName())
                    .eq(CommonTask::getId, req.getId())
                    .update();
        }
        return Boolean.TRUE;
    }

    /**
     * 修改账套归属人
     * @param ownerUser
     * @param preOwnerUser
     * @param companyId
     */
    private void updateAccountSetsUser(String ownerUser, String preOwnerUser, Long companyId){
        User user = userManager.lambdaQuery()
                .eq(User::getUsername, ownerUser)
                .one();
        User preUser = userManager.lambdaQuery()
                .eq(User::getUsername, preOwnerUser)
                .one();
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, companyId)
                .one();
        if(Objects.nonNull(user) && Objects.nonNull(preUser) && Objects.nonNull(customerCompany)){
            try {
                FinancialUtils.updateAccountSetsUser(user.getPhone(), preUser.getPhone(), customerCompany.getTaxNumber());
            }catch (Exception e){

            }
        }
    }

    /**
     * 更换任务负责人
     * @param req
     * @param loginUser
     * @return
     */
    public Boolean changeTaskOwnerUser(TaskChangeOwnerUserReq req, LoginUser loginUser) {
        //基础数据校验
        if(ObjectUtils.isEmpty(req.getId())){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        CommonTask commonTask = commonTaskManager.getById(req.getId());
        if(ObjectUtils.isEmpty(commonTask)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        //校验状态
        if(DictConstants.COMMON_TASK_STATUS_COMPLETED.equals(commonTask.getStatus())
                || DictConstants.COMMON_TASK_STATUS_CANCELLED.equals(commonTask.getStatus())
                || DictConstants.COMMON_TASK_STATUS_PUTUP.equals(commonTask.getStatus())){
            throw new BusinessException(ResultMsg.COMMON_TASK_CHANGE_OWNER_USER_NOT_EXISTS_STATUS);
        }
        //任务负责人校验
        if(!loginUser.getUsername().equals(commonTask.getOwnerUser())){
            throw new BusinessException(ResultMsg.COMMON_TASK_EXECUTE_NOT_EXISTS_GENERAL_OWNER);
        }

        LambdaUpdateWrapper<CommonTask> wrapper = Wrappers.lambdaUpdate();
        wrapper.set(CommonTask::getOwnerUser, req.getOwnerUser())
                .set(CommonTask::getOwnerUserName, req.getOwnerUserName())
                .set(CommonTask::getUpdateTime, new Date());
        if(ObjectUtils.isNotEmpty(commonTask.getCurrentOwnerUser()) && commonTask.getCurrentOwnerUser().equals(commonTask.getOwnerUser())){
            wrapper.set(CommonTask::getCurrentOwnerUser, req.getOwnerUser())
                    .set(CommonTask::getCurrentOwnerUserName, req.getOwnerUserName());
        }
        if(ObjectUtils.isNotEmpty(loginUser)){
            wrapper.set(CommonTask::getUpdateBy, loginUser.getUsername())
                    .set(CommonTask::getUpdateByName, loginUser.getRealName());
        }
        wrapper.eq(CommonTask::getId, commonTask.getId());
        commonTaskManager.update(wrapper);
        // 如果总负责发生变更，流程负责人为总负责人的 也要进行变更
        commonTaskDetailManager.lambdaUpdate()
                .set(CommonTaskDetail::getOwnerUser, req.getOwnerUser())
                .set(CommonTaskDetail::getOwnerUserName, req.getOwnerUserName())
                .eq(CommonTaskDetail::getTaskId, commonTask.getId())
                .eq(CommonTaskDetail::getOwnerUser, commonTask.getOwnerUser())
                .eq(CommonTaskDetail::getOwnerUserName, commonTask.getOwnerUserName())
                .update();
        return Boolean.TRUE;
    }

    /**
     * 开始任务
     * @param id
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean startTask(Long id, LoginUser loginUser) {
        //基础数据校验
        if(ObjectUtils.isEmpty(id)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        CommonTask commonTask = commonTaskManager.getById(id);
        if(ObjectUtils.isEmpty(commonTask)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        //非待处理状态，无法执行开始任务
        if(!DictConstants.COMMON_TASK_STATUS_PENDING.equals(commonTask.getStatus())){
            throw new BusinessException(ResultMsg.COMMON_TASK_STATUS_NOT_PENDING);
        }
        //任务负责人校验
        if(!loginUser.getUsername().equals(commonTask.getOwnerUser())
                && !loginUser.getUsername().equals(commonTask.getCurrentOwnerUser()) ){
            throw new BusinessException(ResultMsg.COMMON_TASK_EXECUTE_NOT_EXISTS_OWNER);
        }
        //根据执行任务流程执行顺序获取第一条流程数据
        LambdaQueryWrapper<CommonTaskDetail> detailWrapper = Wrappers.lambdaQuery();
        detailWrapper.eq(CommonTaskDetail::getTaskId, id)
                .eq(CommonTaskDetail::getStatus, DictConstants.COMMON_TASK_DETAIL_STATUS_PENDING)
                .eq(CommonTaskDetail::getIsCurrent, GlobalConstant.Number.ONE_INT)
                .orderByAsc(CommonTaskDetail::getSort);

        CommonTaskDetail commonTaskDetail = commonTaskDetailManager.getBaseMapper().selectOne(detailWrapper);
        //开始任务是需判断当前流程是否需要执行 不需要执行无需额外处理，修改状态出处理中
        if(GlobalConstant.Number.ONE_INT == commonTaskDetail.getHandMethod()) {
            commonTaskManager.lambdaUpdate()
                    .set(CommonTask::getStartTime, LocalDateTime.now())
                    .eq(CommonTask::getId, commonTask.getId())
                    .update();
            executeTaskDetail(commonTask, commonTaskDetail, loginUser);

        }else{
            commonTaskManager.lambdaUpdate()
                    .set(CommonTask::getStatus, DictConstants.COMMON_TASK_STATUS_PROCESSING)
                    .set(CommonTask::getStartTime, LocalDateTime.now())
                    .set(CommonTask::getUpdateTime, new Date())
                    .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateBy, loginUser.getUsername())
                    .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateByName, loginUser.getRealName())
                    .eq(CommonTask::getId, commonTask.getId())
                    .update();
        }
        return Boolean.TRUE;
    }

    /**
     * 执行任务流程
     *
     * @param req
     * @param fileList
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean executeTaskDetail(ExecuteTaskDetailReq req, List<MultipartFile> fileList, LoginUser loginUser) {
        // 校验能否执行
        if(ObjectUtils.isEmpty(req.getTaskId())){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        CommonTask commonTask = commonTaskManager.getById(req.getTaskId());
        if(ObjectUtils.isEmpty(commonTask)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        // 任务非处理中或挂起，无法执行
        if(!DictConstants.COMMON_TASK_STATUS_PROCESSING.equals(commonTask.getStatus())
                && !DictConstants.COMMON_TASK_STATUS_PUTUP.equals(commonTask.getStatus())){
            throw new BusinessException(ResultMsg.COMMON_TASK_STATUS_NOT_PROCESSING_OR_PUTUP);
        }
        CommonTaskDetail commonTaskDetail = commonTaskDetailManager.lambdaQuery()
                .eq(CommonTaskDetail::getTaskId, req.getTaskId())
                .eq(CommonTaskDetail::getId, req.getId())
                .one();
        if(ObjectUtils.isEmpty(commonTaskDetail)){
            throw new BusinessException(ResultMsg.COMMON_TASK_DETAIL_NOT_DATA);
        }
        // 任务负责人校验
        if(!loginUser.getUsername().equals(commonTaskDetail.getOwnerUser())
                && !loginUser.getUsername().equals(commonTask.getCurrentOwnerUser())
                && !loginUser.getUsername().equals(commonTask.getOwnerUser())){
            throw new BusinessException(ResultMsg.COMMON_TASK_EXECUTE_NOT_EXISTS_OWNER);
        }
        //执行任务
        executeTaskDetail(commonTask, commonTaskDetail, req.getTaskData(), fileList, loginUser);
        return Boolean.TRUE;
    }

    /**
     * 任务处理中间过渡
     *  例某流程数据替换等
     * @param commonTask
     * @param commonTaskDetail
     * @param updateTaskData
     * @param loginUser
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeTaskDetail(CommonTask commonTask, CommonTaskDetail commonTaskDetail,
                                  String updateTaskData, List<MultipartFile> fileList, LoginUser loginUser){
        //流程数据特殊处理
        if((DictConstants.SERVICE_PROCESS_VERITY_PERSON_TAX.equals(commonTaskDetail.getProcessCode())||DictConstants.SERVICE_PROCESS_FINANCE_DEAL.equals(commonTaskDetail.getProcessCode()))
            && StringUtils.isNotEmpty(updateTaskData)){
            commonTaskDetail.setTaskData(updateTaskData);
        }
        if(DictConstants.SERVICE_PROCESS_OLD_ACCOUNT_IMPORT.equals(commonTaskDetail.getProcessCode())
                && StringUtils.isNotEmpty(updateTaskData)){
            commonTaskDetail.setTaskData(updateTaskData);
            Integer value = GlobalConstant.Number.ZERO_INT;
            JSONArray taskDetailOwnArray = JSON.parseObject(updateTaskData).getJSONArray("own");
            if(CollUtil.isNotEmpty(taskDetailOwnArray)){
                for (int j = 0; j < taskDetailOwnArray.size(); j++) {
                    JSONObject taskDetailData = taskDetailOwnArray.getJSONObject(j);
                    if(taskDetailData.containsKey("prop") && "isHaveOldAccount".equals(taskDetailData.get("prop"))){
                        value = Integer.valueOf(taskDetailData.get("value").toString());
                        break;
                    }
                }
            }
            // 如果value值为1，则代表需上传旧账文件
            if(GlobalConstant.Number.ONE_INT == value){
                if(CollUtil.isEmpty(fileList)){
                    throw new BusinessException(ResultMsg.COMMON_TASK_DETAIL_NOT_FILE);
                }
                fileList.forEach(item -> {
                    fileUpload(item, FileConstants.FILE_TYPE_ORD_ACCOUNT, null, commonTask, loginUser);
                });
            }
        }



        executeTaskDetail(commonTask, commonTaskDetail, loginUser);
    }

    /**
     * 任务执行
     * @param commonTaskDetail
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeTaskDetail(CommonTask commonTask, CommonTaskDetail commonTaskDetail, LoginUser loginUser){
        //执行当前任务 根据流程CODE判断执行不同任务
        //无具体执行方发的任务直接返回true
        Boolean flag = switch (commonTaskDetail.getProcessCode()) {
            case DictConstants.SERVICE_PROCESS_INVOICE_PROCESS -> invoicing(commonTask.getCompanyId(), loginUser);
            case DictConstants.SERVICE_PROCESS_VERITY_TAX -> verityTax(commonTask.getCompanyId(), loginUser);
            case DictConstants.SERVICE_PROCESS_CREATE_ACCOUNT_SET ->
                    createAccountSet(commonTask, commonTaskDetail);
            case DictConstants.SERVICE_PROCESS_ACCOUNT_SET_DATA_MIGRATION ->
                    accountDataMigration(commonTask.getCompanyId(), loginUser);
            case DictConstants.SERVICE_PROCESS_VERITY_PERSON_TAX ->
                    verityPersonTax(commonTask, commonTaskDetail);
            case DictConstants.SERVICE_PROCESS_OLD_ACCOUNT_IMPORT ->
                    commonDeal(commonTask, commonTaskDetail);
            case DictConstants.SERVICE_PROCESS_FINANCE_DEAL ->
                    initFinancialData(commonTask, commonTaskDetail);
            default -> Boolean.TRUE;
        };
        //TRUE代表执行成功，修改状态
        if(flag) {
            commonTaskDetailManager.lambdaUpdate()
                    .set(CommonTaskDetail::getStatus, DictConstants.COMMON_TASK_DETAIL_STATUS_COMPLETED)
                    .set(CommonTaskDetail::getFinishTime, LocalDateTime.now())
                    .set(CommonTaskDetail::getIsCurrent, GlobalConstant.Number.ZERO_INT)
                    .set(CommonTaskDetail::getUpdateTime, new Date())
                    .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateBy, loginUser.getUsername())
                    .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateByName, loginUser.getRealName())
                    .eq(CommonTaskDetail::getId, commonTaskDetail.getId())
                    .update();
            // 挂起任务，仅需判断是否需要修改任务为已完成（无挂起和待处理任务流程）
            if(DictConstants.COMMON_TASK_DETAIL_STATUS_PUTUP.equals(commonTaskDetail.getStatus())){
                List<CommonTaskDetail> dealCommonTaskDetailList = commonTaskDetailManager.lambdaQuery()
                        .eq(CommonTaskDetail::getTaskId, commonTaskDetail.getTaskId())
                        .in(CommonTaskDetail::getStatus, Arrays.asList(DictConstants.COMMON_TASK_DETAIL_STATUS_PUTUP,
                                DictConstants.COMMON_TASK_DETAIL_STATUS_PENDING))
                        .list();
                if(CollUtil.isEmpty(dealCommonTaskDetailList)){
                    completedCommonTask(commonTask, loginUser);
                }
                return;
            }
            //获取下一条任务并判断是否需要执行
            LambdaQueryWrapper<CommonTaskDetail> detailWrapper = Wrappers.lambdaQuery();
            detailWrapper.eq(CommonTaskDetail::getTaskId, commonTaskDetail.getTaskId())
                    .eq(CommonTaskDetail::getStatus, DictConstants.COMMON_TASK_DETAIL_STATUS_PENDING)
                    .eq(CommonTaskDetail::getSort, commonTaskDetail.getSort() + GlobalConstant.Number.ONE_INT)
                    .orderByAsc(CommonTaskDetail::getSort);
            CommonTaskDetail nextCommonTaskDetail = commonTaskDetailManager.getBaseMapper().selectOne(detailWrapper);
            if (ObjectUtils.isNotEmpty(nextCommonTaskDetail)) {
                if (GlobalConstant.Number.ONE_INT == nextCommonTaskDetail.getHandMethod()) {
                    //自动执行
                    executeTaskDetail(commonTask, nextCommonTaskDetail, loginUser);
                } else {
                    //人工执行, 不进行任务处理，修改任务当前负责人，并强制修改任务状态
                    handOverProcess(nextCommonTaskDetail, loginUser);
                }
            } else {
                //如果查询不到数据，查询是否含有挂起任务(任务状态为处理中时，只可能存在已挂起任务流程，任务总状态为挂起和取消无法执行任务流程)
                List<CommonTaskDetail> putUpCommonTaskDetailList = commonTaskDetailManager.lambdaQuery()
                        .eq(CommonTaskDetail::getTaskId, commonTaskDetail.getTaskId())
                        .eq(CommonTaskDetail::getStatus, DictConstants.COMMON_TASK_DETAIL_STATUS_PUTUP)
                        .orderByAsc(CommonTaskDetail::getSort)
                        .list();
                if(CollUtil.isNotEmpty(putUpCommonTaskDetailList)){
                    //按照任务流程执行顺序 修改任务当前负责人，并强制修改任务状态
                    CommonTaskDetail putUpCommonTaskDetail = putUpCommonTaskDetailList.get(GlobalConstant.Number.ZERO_INT);
                    handOverProcess(putUpCommonTaskDetail, loginUser);
                }else {
                    //则代表任务流程全部执行完成，则修改任务为已完成，当前负责人移交给总负责人
                    completedCommonTask(commonTask, loginUser);
                }
            }
        }else{
            throw new BusinessException(MessageFormat.format(ResultMsg.COMMON_TASK_DETAIL_EXECUTE_FAIL, commonTaskDetail.getProcessName()));
        }
    }

    /**
     * 完成任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void completedCommonTask(CommonTask commonTask, LoginUser loginUser){
        commonTaskManager.lambdaUpdate()
                .set(CommonTask::getStatus, DictConstants.COMMON_TASK_STATUS_COMPLETED)
                .set(CommonTask::getRealFinishTime, LocalDateTime.now())
                .set(CommonTask::getUpdateTime, new Date())
                .set(CommonTask::getCurrentOwnerUser, commonTask.getOwnerUser())
                .set(CommonTask::getCurrentOwnerUserName, commonTask.getOwnerUserName())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateByName, loginUser.getRealName())
                .eq(CommonTask::getId, commonTask.getId())
                .update();
    }

    /**
     * 流程移交
     */
    @Transactional(rollbackFor = Exception.class)
    public void handOverProcess(CommonTaskDetail commonTaskDetail, LoginUser loginUser){
        //任务移交，修改任务当前流程，修改任务当前负责人，并强制修改任务状态
        commonTaskManager.lambdaUpdate()
                .set(CommonTask::getStatus, DictConstants.COMMON_TASK_STATUS_PROCESSING)
                .set(ObjectUtils.isNotEmpty(commonTaskDetail.getOwnerUser()),
                        CommonTask::getCurrentOwnerUser, commonTaskDetail.getOwnerUser())
                .set(ObjectUtils.isNotEmpty(commonTaskDetail.getOwnerUserName()),
                        CommonTask::getCurrentOwnerUserName, commonTaskDetail.getOwnerUserName())
                .set(CommonTask::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateByName, loginUser.getRealName())
                .eq(CommonTask::getId, commonTaskDetail.getTaskId())
                .update();
        // 修改流程当前状态
        commonTaskDetailManager.lambdaUpdate()
                .set(CommonTaskDetail::getIsCurrent, GlobalConstant.Number.ONE_INT)
                .set(CommonTaskDetail::getStartTime, LocalDateTime.now())
                .set(CommonTaskDetail::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateByName, loginUser.getRealName())
                .eq(CommonTaskDetail::getId, commonTaskDetail.getId())
                .update();
    }

    /**
     * 处理账套初始数据
     * @param commonTask
     * @param commonTaskDetail
     * @return
     */
    public Boolean initFinancialData(CommonTask commonTask, CommonTaskDetail commonTaskDetail){

        try{
            String taxNumber = customerCompanyManager.getById(commonTask.getCompanyId()).getTaxNumber();

            if(StringUtils.isNotEmpty(commonTaskDetail.getTaskData())) {
                JSONObject jsonObject = JSON.parseObject(commonTaskDetail.getTaskData());
                JSONArray ownArray = jsonObject.getJSONArray("own");
                String filePath = "";
                String oldFile = "";
                for (int i = 0; i < ownArray.size(); i++) {
                    JSONObject data = ownArray.getJSONObject(i);

                    if ("filePath".equals(data.getString("prop"))) {
                        filePath = data.getString("value");
                    }
                    if ("oldFile".equals(data.getString("prop"))) {
                        oldFile = data.getString("value");
                    }
                }
                FinancialUtils.importSubjectInit(taxNumber,filePath,oldFile);
            }
            commonDeal(commonTask, commonTaskDetail);
            return true;
        }catch (Exception e){
            e.getMessage();
            return false;
        }

    }


    /**
     * 处理个税
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean verityPersonTax(CommonTask commonTask, CommonTaskDetail commonTaskDetail){
        //基础数据校验
        if(ObjectUtils.isEmpty(commonTaskDetail.getId())){
            return Boolean.FALSE;
        }
        //修改任务数据
        commonDeal(commonTask, commonTaskDetail);
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, commonTask.getCompanyId())
                .one();
        //修改财务个税密码
        if(ObjectUtils.isNotEmpty(customerCompany)){
            if(StringUtils.isNotEmpty(commonTaskDetail.getTaskData())) {
                JSONObject jsonObject = JSON.parseObject(commonTaskDetail.getTaskData());
                JSONArray ownArray = jsonObject.getJSONArray("own");
                for (int i = 0; i < ownArray.size(); i++) {
                    JSONObject data = ownArray.getJSONObject(i);
                    if ("personTaxPassword".equals(data.getString("prop"))) {
                        FinancialUtils.updateIndividualTaxCodeByCreditCode(customerCompany.getTaxNumber(), data.getString("value"));
                        break;
                    }
                }
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 通用处理
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean commonDeal(CommonTask commonTask, CommonTaskDetail commonTaskDetail){
        //基础数据校验
        if(ObjectUtils.isEmpty(commonTaskDetail.getId())){
            return Boolean.FALSE;
        }
        //修改任务数据
        updateCommonTaskData(commonTask, commonTaskDetail);
        commonTaskDetailManager.lambdaUpdate()
                .set(CommonTaskDetail::getTaskData, commonTaskDetail.getTaskData())
                .eq(CommonTaskDetail::getId, commonTaskDetail.getId())
                .update();
        return Boolean.TRUE;
    }

    /**
     * 验证税密
     */
    public Boolean verityTax(Long companyId, LoginUser loginUser){
        //基础数据校验
        if(ObjectUtils.isEmpty(companyId)){
            return Boolean.FALSE;
        }
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getIsVerifyTax, GlobalConstant.Number.ONE_INT)
                .set(CustomerCompany::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), CustomerCompany::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CustomerCompany::getUpdateByName, loginUser.getRealName())
                .eq(CustomerCompany::getId, companyId)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 开票服务
     */
    public Boolean invoicing(Long companyId, LoginUser loginUser){
        //基础数据校验
        if(ObjectUtils.isEmpty(companyId)){
            return Boolean.FALSE;
        }
        customerCompanyManager.lambdaUpdate()
                .set(CustomerCompany::getIsInvoicing, GlobalConstant.Number.ONE_INT)
                .set(CustomerCompany::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), CustomerCompany::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CustomerCompany::getUpdateByName, loginUser.getRealName())
                .eq(CustomerCompany::getId, companyId)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 创建账套
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean createAccountSet(CommonTask commonTask, CommonTaskDetail commonTaskDetail){
        //基础数据校验
        if(ObjectUtils.isEmpty(commonTask.getCompanyId())){
            return Boolean.FALSE;
        }
        if(StringUtils.isEmpty(commonTaskDetail.getTaskData())){
            return Boolean.FALSE;
        }
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, commonTask.getCompanyId())
                .one();
        //未查到公司信息
        if(ObjectUtils.isEmpty(customerCompany)){
            return Boolean.FALSE;
        }
        // 修改任务数据
        updateCommonTaskData(commonTask, commonTaskDetail);
        //存在正式账套
        JSONObject dataJson = FinancialUtils.getAccountSets(customerCompany.getTaxNumber());
        if(ObjectUtils.isNotEmpty(dataJson)){
            return Boolean.TRUE;
        }
        //拼装请求参数
        CreateAccountSetReq req = getCreateAccountSetReq(commonTaskDetail.getTaskData());
        if(ObjectUtils.isEmpty(req)){
            return Boolean.FALSE;
        }
        //创建账套
        JSONObject jsonObject = FinancialUtils.createAccountSet(req);
        if(ObjectUtils.isNotEmpty(jsonObject) && jsonObject.containsKey("code")){
            if(jsonObject.getInteger("code") == GlobalConstant.Number.TWOHUNDRED_INT){
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }

    /**
     * 修改任务数据
     * @param commonTask
     * @param commonTaskDetail
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCommonTaskData(CommonTask commonTask, CommonTaskDetail commonTaskDetail){
        if(StringUtils.isNotEmpty(commonTask.getTaskData()) && StringUtils.isNotEmpty(commonTaskDetail.getTaskData())) {
            JSONObject commonTaskData = JSON.parseObject(commonTask.getTaskData());
            JSONObject commonDetailTaskData = JSON.parseObject(commonTaskDetail.getTaskData());
            JSONArray taskOwnArray = commonTaskData.getJSONArray("own");
            if(CollUtil.isNotEmpty(taskOwnArray)){
                for (int i = 0; i < taskOwnArray.size(); i++) {
                    JSONObject taskData = taskOwnArray.getJSONObject(i);
                    JSONArray taskDetailOwnArray = commonDetailTaskData.getJSONArray("own");
                    if(taskData.containsKey("prop") && CollUtil.isNotEmpty(taskDetailOwnArray)){
                        String prop = taskData.get("prop").toString();
                        for (int j = 0; j < taskDetailOwnArray.size(); j++) {
                            JSONObject taskDetailData = taskDetailOwnArray.getJSONObject(j);
                            if(taskDetailData.containsKey("prop") && prop.equals(taskDetailData.get("prop"))){
                                taskData.put("value", taskDetailData.get("value"));
                            }
                        }
                    }
                }
            }
            commonTaskManager.lambdaUpdate()
                    .set(CommonTask::getTaskData, commonTaskData.toJSONString())
                    .eq(CommonTask::getId, commonTask.getId())
                    .update();
        }
    }

    /**
     * 拼装创建账套请求数据
     * @param taskData
     * @return
     */
    private CreateAccountSetReq getCreateAccountSetReq(String taskData){
        CreateAccountSetReq req = new CreateAccountSetReq();
        String companyType = null;
        if(StringUtils.isNotEmpty(taskData)) {
            JSONObject jsonObject = JSON.parseObject(taskData);
            JSONArray ownArray = jsonObject.getJSONArray("own");
            for (int i = 0; i < ownArray.size(); i++) {
                JSONObject data = ownArray.getJSONObject(i);
                if ("companyName".equals(data.getString("prop"))) {
                    req.setCompanyName(data.getString("value"));
                }
                if ("companyType".equals(data.getString("prop"))) {
                    companyType = data.getString("value");
                }
                if ("creditCode".equals(data.getString("prop"))) {
                    req.setCreditCode(data.getString("value"));
                }
                if ("enableDate".equals(data.getString("prop"))) {
                    req.setEnableDate(
                            DateUtils.stringToLocalDate(data.getString("value"), DateUtils.YYYY_MM_DD)
                                    .with(TemporalAdjusters.firstDayOfMonth())
                    );
                }
                if ("province".equals(data.getString("prop"))) {
                    req.setProvince(data.getString("value"));
                    GetTaxPlaceLocationUserDto dto = taxPlaceLocationUserManager.getBaseMapper().getUserByProvince(data.getString("value"));
                    if(ObjectUtils.isEmpty(dto) || ObjectUtils.isEmpty(dto.getFinancialUserId())){
                        return null;
                    }
                    req.setCreatorId(dto.getFinancialUserId());
                }
            }
        }
        if(DictConstants.COMPANY_TYPE_SCALE.equals(companyType)){
            req.setAstId(FinancialConstants.TEMPLATE_ID_SMALL_SCALE);
            req.setVatType(FinancialConstants.VAT_TYPE_SMALL_SCALE);
        }else if(DictConstants.COMPANY_TYPE_GENERAL.equals(companyType)){
            req.setAstId(FinancialConstants.TEMPLATE_ID_COMMONLY);
            req.setVatType(FinancialConstants.VAT_TYPE_COMMONLY);
        }
        //固定取值
        req.setElectricityTaxPhone(FinancialConstants.ELECTRICITY_TAX_PHONE);
        req.setElectricityTaxCode(FinancialConstants.ELECTRICITY_TAX_CODE);
        req.setZeroDeclarations(Boolean.FALSE);
        req.setVoucherReviewed(Boolean.FALSE);
        return req;
    }

    /**
     * 账套数据迁移
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean accountDataMigration(Long companyId, LoginUser loginUser){
        //基础数据校验
        if(ObjectUtils.isEmpty(companyId)){
            return Boolean.FALSE;
        }
        CustomerCompany customerCompany = customerCompanyManager.lambdaQuery()
                .eq(CustomerCompany::getId, companyId)
                .one();
        if(ObjectUtils.isEmpty(customerCompany)){
            return Boolean.FALSE;
        }
        if(ObjectUtils.isEmpty(customerCompany.getExperienceAccountSetId())) {
            return Boolean.TRUE;
        }
        //获取体验账套数据
        List<FinancialDataMigration> list = financialDataMigrationManager.lambdaQuery()
                .eq(FinancialDataMigration::getExperienceAccountSetId, customerCompany.getExperienceAccountSetId())
                .eq(FinancialDataMigration::getMigrationStatus, GlobalConstant.Number.ONE_INT)
                .list();
        if(CollUtil.isNotEmpty(list)){
            return Boolean.TRUE;
        }
        JSONArray experienceMigrationData = FinancialUtils.getExperienceMigrationData(customerCompany.getExperienceAccountSetId());
        if (CollUtil.isNotEmpty(experienceMigrationData)) {
            list = experienceMigrationData.stream().map(item -> {
                FinancialDataMigration data = CopierCopyUtils.convertTo(item, FinancialDataMigration.class);
                data.setMigrationStatus(GlobalConstant.Number.ZERO_INT);
                data.setCreateTime(new Date());
                if(ObjectUtils.isNotEmpty(loginUser)){
                    data.setCreateBy(loginUser.getUsername());
                    data.setCreateByName(loginUser.getRealName());
                }
                return data;
            }).toList();
            financialDataMigrationManager.saveBatch(list);
        }
        //添加至正式账套
        if(CollUtil.isNotEmpty(list)) {
            JSONObject dataJson = FinancialUtils.getAccountSets(customerCompany.getTaxNumber());
            if (ObjectUtils.isNotEmpty(dataJson)) {
                Long accountSetId = Long.valueOf(dataJson.getLong("id").toString());
                list.forEach(item -> item.setAccountSetId(accountSetId));
                Boolean flag = FinancialUtils.addAccountSetMigrationData(CopierCopyUtils.convertTo(list, AddAccountSetMigrationDataReq.class));
                if (!flag) {
                    return Boolean.FALSE;
                } else {
                    list.forEach(item -> {
                        item.setMigrationStatus(GlobalConstant.Number.ONE_INT);
                        item.setUpdateTime(new Date());
                        if(ObjectUtils.isNotEmpty(loginUser)){
                            item.setUpdateBy(loginUser.getUsername());
                            item.setUpdateByName(loginUser.getRealName());
                        }
                    });
                    financialDataMigrationManager.updateBatchById(list);
                }
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 任务挂起
     * @param req
     * @param loginUser
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    public Boolean putUpTask(PutupTaskReq req, LoginUser loginUser) {
        if(ObjectUtils.isEmpty(req.getTaskId()) || ObjectUtils.isEmpty(req.getTaskDetailId())){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        CommonTaskDetail commonTaskDetail = commonTaskDetailManager.getById(req.getTaskDetailId());
        CommonTask commonTask = commonTaskManager.getById(req.getTaskId());
        if(ObjectUtils.isEmpty(commonTask) || ObjectUtils.isEmpty(commonTaskDetail)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        // 任务非处理中，无法执行
        if(!DictConstants.COMMON_TASK_STATUS_PROCESSING.equals(commonTask.getStatus())){
            throw new BusinessException(ResultMsg.COMMON_TASK_STATUS_NOT_PROCESSING);
        }
        if(!loginUser.getUsername().equals(commonTask.getCurrentOwnerUser())
                && !loginUser.getUsername().equals(commonTask.getOwnerUser())
                && !loginUser.getUsername().equals(commonTaskDetail.getOwnerUser())){
            throw new BusinessException(ResultMsg.COMMON_TASK_EXECUTE_NOT_EXISTS_OWNER);
        }
        commonTaskDetailManager.lambdaUpdate()
                .set(CommonTaskDetail::getStatus, DictConstants.COMMON_TASK_DETAIL_STATUS_PUTUP)
                .set(CommonTaskDetail::getIsCurrent, GlobalConstant.Number.ZERO_INT)
                .set(req.getIsHandOverProcess() == GlobalConstant.Number.ONE_INT, CommonTaskDetail::getPutUpReason, req.getPutUpReason())
                .set(CommonTaskDetail::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateByName, loginUser.getRealName())
                .eq(CommonTaskDetail::getId, req.getTaskDetailId())
                .update();
        if(GlobalConstant.Number.ZERO_INT == req.getIsHandOverProcess()){
            // 不移交，挂起主任务，不做其他处理
            commonTaskManager.lambdaUpdate()
                    .set(CommonTask::getStatus, DictConstants.COMMON_TASK_STATUS_PUTUP)
                    .set(CommonTask::getPreStatus, commonTask.getStatus())
                    .set(CommonTask::getPutUpReason, req.getPutUpReason())
                    .set(CommonTask::getUpdateTime, new Date())
                    .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateBy, loginUser.getUsername())
                    .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateByName, loginUser.getRealName())
                    .eq(CommonTask::getId, req.getTaskId())
                    .update();
        }else if(GlobalConstant.Number.ONE_INT == req.getIsHandOverProcess()){
            //任务移交，判断是否存在下一流程
            CommonTaskDetail nextCommonTaskDetail = commonTaskDetailManager.lambdaQuery()
                    .eq(CommonTaskDetail::getStatus, DictConstants.COMMON_TASK_DETAIL_STATUS_PENDING)
                    .eq(CommonTaskDetail::getSort, commonTaskDetail.getSort() + GlobalConstant.Number.ONE_INT)
                    .eq(CommonTaskDetail::getTaskId, req.getTaskId())
                    .one();
            if (ObjectUtils.isNotEmpty(nextCommonTaskDetail)) {
                //判断是否需要自动执行，接入任务执行流程
                //任务最终流程有人工任务兜底，无需考虑因事务问题导致忽略此挂起任务使得任务状态修改为完成
                if (GlobalConstant.Number.ONE_INT == nextCommonTaskDetail.getHandMethod()) {
                    //自动执行
                    executeTaskDetail(commonTask, nextCommonTaskDetail, loginUser);
                } else {
                    //人工执行, 不进行任务处理，修改任务当前负责人，并强制修改任务状态
                    handOverProcess(nextCommonTaskDetail, loginUser);
                }
            } else {
                throw new BusinessException(ResultMsg.COMMON_TASK_DETAIL_PUTUP_NOT_DATA);
            }
        }
        return Boolean.TRUE;
    }

    /**
     * 任务取消
     * @param req
     * @param loginUser
     * @return
     */
    public Boolean cancelTask(CancelTaskReq req, LoginUser loginUser) {
        if(ObjectUtils.isEmpty(req.getTaskId())){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        CommonTask commonTask = commonTaskManager.getById(req.getTaskId());
        if(ObjectUtils.isEmpty(commonTask)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        // 任务非处理中，无法执行
        if(!DictConstants.COMMON_TASK_STATUS_PROCESSING.equals(commonTask.getStatus())){
            throw new BusinessException(ResultMsg.COMMON_TASK_STATUS_NOT_PROCESSING);
        }
        if(!loginUser.getUsername().equals(commonTask.getCurrentOwnerUser())
                && !loginUser.getUsername().equals(commonTask.getOwnerUser())){
            throw new BusinessException(ResultMsg.COMMON_TASK_EXECUTE_NOT_EXISTS_OWNER);
        }
        //修改任务状态即可
        commonTaskManager.lambdaUpdate()
                .set(CommonTask::getStatus, DictConstants.COMMON_TASK_STATUS_CANCELLED)
                .set(CommonTask::getPreStatus, commonTask.getStatus())
                .set(CommonTask::getCancelReason, req.getCancelReason())
                .set(CommonTask::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateByName, loginUser.getRealName())
                .eq(CommonTask::getId, req.getTaskId())
                .update();
        //修改待处理任务流程状态
        commonTaskDetailManager.lambdaUpdate()
                .set(CommonTaskDetail::getStatus, DictConstants.COMMON_TASK_DETAIL_STATUS_CANCELLED)
                .set(CommonTaskDetail::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateByName, loginUser.getRealName())
                .eq(CommonTaskDetail::getStatus, DictConstants.COMMON_TASK_DETAIL_STATUS_PENDING)
                .eq(CommonTaskDetail::getTaskId, req.getTaskId())
                .update();
        // 释放排班号源
        csScheduleService.releaseSlot(commonTask.getScheduleSlotId());
        return Boolean.TRUE;
    }

    /**
     * 任务恢复
     * @param id
     * @param loginUser
     * @return
     */
    public Boolean restoreTask(Long id, LoginUser loginUser) {
        if(ObjectUtils.isEmpty(id)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        CommonTask commonTask = commonTaskManager.getById(id);
        if(ObjectUtils.isEmpty(commonTask)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        // 任务非挂起和取消，无法执行
        if(!DictConstants.COMMON_TASK_STATUS_CANCELLED.equals(commonTask.getStatus())
                && !DictConstants.COMMON_TASK_STATUS_PUTUP.equals(commonTask.getStatus())){
            throw new BusinessException(ResultMsg.COMMON_TASK_RESTORE_NOT_EXISTS_STATUS);
        }
        //只可以由当前负责人或总负责人恢复
        if(DictConstants.COMMON_TASK_STATUS_PUTUP.equals(commonTask.getStatus())){
            if(!loginUser.getUsername().equals(commonTask.getCurrentOwnerUser())
                    && !loginUser.getUsername().equals(commonTask.getOwnerUser())){
                throw new BusinessException(ResultMsg.COMMON_TASK_EXECUTE_NOT_EXISTS_OWNER);
            }
        }
        //恢复任务状态
        commonTaskManager.lambdaUpdate()
                .set(CommonTask::getStatus, commonTask.getPreStatus())
                .set(CommonTask::getPreStatus, commonTask.getStatus())
                .set(CommonTask::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTask::getUpdateByName, loginUser.getRealName())
                .eq(CommonTask::getId, id)
                .update();
        //恢复任务流程状态
        commonTaskDetailManager.lambdaUpdate()
                .set(CommonTaskDetail::getStatus, DictConstants.COMMON_TASK_DETAIL_STATUS_PENDING)
                .set(CommonTaskDetail::getIsCurrent, GlobalConstant.Number.ONE_INT)
                .set(CommonTaskDetail::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskDetail::getUpdateByName, loginUser.getRealName())
                .in(CommonTaskDetail::getStatus, Arrays.asList(DictConstants.COMMON_TASK_DETAIL_STATUS_PUTUP,
                        DictConstants.COMMON_TASK_DETAIL_STATUS_CANCELLED))
                .eq(CommonTaskDetail::getTaskId, id)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 获取待执行任务信息（任务层面）
     * @param loginUser
     * @return
     */
    public GetDealCommonTaskResp getCommonTaskDealInfo(LoginUser loginUser) {
        GetDealCommonTaskResp resp = new GetDealCommonTaskResp();
        String ownerUser;
        if(!userService.checkLoginUserIdAdmin(loginUser)){
            ownerUser = loginUser.getUsername();
        } else {
            ownerUser = null;
        }
        List<CommonTask> commonTaskList = commonTaskManager.lambdaQuery()
                .in(CommonTask::getStatus, Arrays.asList(DictConstants.COMMON_TASK_STATUS_PENDING, DictConstants.COMMON_TASK_STATUS_PROCESSING))
                .eq(ObjectUtils.isNotEmpty(ownerUser), CommonTask::getCurrentOwnerUser, loginUser.getUsername())
                .list();
        if(CollUtil.isEmpty(commonTaskList)){
            return resp;
        }
        // 任务数据
        resp.setTaskSum(commonTaskList.size());
        Map<Boolean, List<CommonTask>> taskPart = commonTaskList.stream()
                .collect(Collectors.partitioningBy(item -> item.getCreateTime()
                        .before(DateUtils.getBeforeDateWithTime(
                                GlobalConstant.Number.ONE_INT,
                                GlobalConstant.Number.SEVENTEEN_INT,
                                GlobalConstant.Number.ZERO_INT,
                                GlobalConstant.Number.ZERO_INT)
                        )
                ));
        // 昨天数据
        List<CommonTask> beforeTask = taskPart.get(true);
        if(CollUtil.isNotEmpty(beforeTask)){
            resp.setYesterdayTaskCount(beforeTask.size());
            resp.setYesterdayTaskInfoList(getDealCommonTaskInfoList(beforeTask));
            List<Long> taskIdList = beforeTask.stream().map(CommonTask::getId).toList();
            List<DealCommonTaskDetailInfo> detailInfoList = getDealCommonTaskDetailInfoList(taskIdList, ownerUser);
            if(CollUtil.isNotEmpty(detailInfoList)){
                resp.setYesterdayTaskDetailCount(detailInfoList.stream().mapToInt(DealCommonTaskDetailInfo::getCount).sum());
                resp.setYesterdayTaskDetailInfoList(detailInfoList);
            }
        }
        // 今天数据
        List<CommonTask> afterTask = taskPart.get(false);
        if(CollUtil.isNotEmpty(afterTask)){
            resp.setTodayTaskCount(afterTask.size());
            resp.setTodayTaskInfoList(getDealCommonTaskInfoList(afterTask));
            List<Long> taskIdList = afterTask.stream().map(CommonTask::getId).toList();
            List<DealCommonTaskDetailInfo> detailInfoList = getDealCommonTaskDetailInfoList(taskIdList, ownerUser);
            if(CollUtil.isNotEmpty(detailInfoList)){
                resp.setTodayTaskDetailCount(detailInfoList.stream().mapToInt(DealCommonTaskDetailInfo::getCount).sum());
                resp.setTodayTaskDetailInfoList(detailInfoList);
            }
        }
        resp.setTaskDetailSum(resp.getYesterdayTaskDetailCount() + resp.getTodayTaskDetailCount());
        return resp;
    }

    /**
     * 获取返回任务数据
     * @param commonTaskList
     * @return
     */
    private ArrayList<DealCommonTaskInfo> getDealCommonTaskInfoList(List<CommonTask> commonTaskList){
        Map<Long, List<CommonTask>> collect = commonTaskList.stream().collect(Collectors.groupingBy(CommonTask::getServiceId));
        ArrayList<DealCommonTaskInfo> dealCommonTaskInfoList = new ArrayList<>();
        collect.forEach((k, v) -> {
            DealCommonTaskInfo info = new DealCommonTaskInfo();
            info.setServiceId(k);
            info.setServiceName(v.get(GlobalConstant.Number.ZERO_INT).getServiceName());
            info.setCount(v.size());
            dealCommonTaskInfoList.add(info);
        });
        return dealCommonTaskInfoList;
    }

    /**
     * 获取返回任务流程数据
     * @param commonTaskIdList
     * @param ownerUser
     * @return
     */
    private List<DealCommonTaskDetailInfo> getDealCommonTaskDetailInfoList(List<Long> commonTaskIdList, String ownerUser){
        List<CommonTaskDetail> commonTaskDetailList = commonTaskDetailManager.lambdaQuery()
                .eq(CommonTaskDetail::getStatus, DictConstants.COMMON_TASK_DETAIL_STATUS_PENDING)
                .in(CommonTaskDetail::getTaskId, commonTaskIdList)
                .eq(ObjectUtils.isNotEmpty(ownerUser), CommonTaskDetail::getOwnerUser, ownerUser)
                .list();
        if(CollUtil.isEmpty(commonTaskDetailList)){
            return Collections.emptyList();
        }
        Map<String, List<CommonTaskDetail>> collect = commonTaskDetailList.stream().collect(Collectors.groupingBy(CommonTaskDetail::getProcessCode));
        ArrayList<DealCommonTaskDetailInfo> dealCommonTaskDetailInfoList = new ArrayList<>();
        collect.forEach((k, v) -> {
            DealCommonTaskDetailInfo info = new DealCommonTaskDetailInfo();
            info.setProcessCode(k);
            info.setProcessName(v.get(GlobalConstant.Number.ZERO_INT).getProcessName());
            info.setCount(v.size());
            dealCommonTaskDetailInfoList.add(info);
        });
        return dealCommonTaskDetailInfoList;
    }

    /**
     * 上传旧账文件
     * @param file
     * @param uploadType
     * @param originalFileName
     * @param commonTask
     * @param loginUser
     * @return
     */
    public Boolean fileUpload(MultipartFile file, String uploadType, String originalFileName, CommonTask commonTask, LoginUser loginUser) {
        if(ObjectUtils.isEmpty(commonTask)){
            throw new BusinessException(ResultMsg.COMMON_TASK_NOT_DATA);
        }
        if(StringUtils.isEmpty(originalFileName)){
            originalFileName = file.getOriginalFilename();
        }
        //文件后缀
        String fileSuffix = FileNameUtil.getSuffix(originalFileName);
        // 获取文件保存路径
        String savePath = FileUtils.getSavePath(uploadType);
        String taxNumber = customerCompanyManager.getById(commonTask.getCompanyId()).getTaxNumber();

        // 获取文件保存名
        String saveFileName = System.currentTimeMillis()+"."+fileSuffix;
        //文件上传至服务器
        FileUtils.uploadFile(file, savePath, saveFileName);
        // 拼装保存参数
        CommonTaskFile commonTaskFile = new CommonTaskFile();
        commonTaskFile.setTaskId(commonTask.getId());
        commonTaskFile.setCompanyId(commonTask.getCompanyId());
        commonTaskFile.setUploadType(uploadType);
        commonTaskFile.setFileName(saveFileName);
        commonTaskFile.setOriginalFileName(originalFileName);
        commonTaskFile.setFileSuffix(fileSuffix);
        commonTaskFile.setFilePath(FileConstants.FILE_GET_ORD_ACCOUNT + saveFileName);
        commonTaskFile.setCreateTime(new Date());
        commonTaskFile.setTaxNumber(taxNumber);
        if(ObjectUtils.isNotEmpty(loginUser)) {
            commonTaskFile.setCreateBy(loginUser.getUsername());
            commonTaskFile.setCreateByName(loginUser.getRealName());
        }
        commonTaskFileManager.save(commonTaskFile);
        return Boolean.TRUE;
    }

    /**
     * 删除任务文件
     * @param fileId
     * @param loginUser
     * @return
     */
    public Boolean deleteCommonTaskFile(Long fileId, LoginUser loginUser) {
        if(ObjectUtils.isEmpty(fileId)){
            return Boolean.TRUE;
        }
        commonTaskFileManager.lambdaUpdate()
                .set(CommonTaskFile::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(CommonTaskFile::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskFile::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), CommonTaskFile::getUpdateByName, loginUser.getRealName())
                .eq(CommonTaskFile::getId, fileId)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 获取任务上传文件
     * @param req
     * @return
     */
    public List<GetCommonTaskFileListResp> getCommonTaskFileList(GetCommonTaskFileListReq req){
        if(ObjectUtils.isEmpty(req.getTaskId())){
            return Collections.emptyList();
        }
        List<CommonTaskFile> commonTaskFileList = commonTaskFileManager.lambdaQuery()
                .eq(ObjectUtils.isNotEmpty( req.getTaskId()),CommonTaskFile::getTaskId, req.getTaskId())
                .eq(StringUtils.isNotEmpty(req.getTaxNumber()),CommonTaskFile::getTaxNumber, req.getTaxNumber())
                .eq(ObjectUtils.isNotEmpty(req.getUploadType()), CommonTaskFile::getUploadType, req.getUploadType())
                .list();
        return CopierCopyUtils.convertTo(commonTaskFileList, GetCommonTaskFileListResp.class);
    }

    /**
     * 保存账套初始化数据，保存为文件
     * @param list
     * @return
     */
    public String saveFinancialInitFile(List<SubjectReq> list){

        // 列头（添加旧账科目编码列）
        final String[] headers = new String[]{"科目编码", "科目名称", "科目类别", "余额方向", "数量核算", "辅助核算", "旧账科目编码"};
        // 生成文件名（时间戳）
        String fileName = System.currentTimeMillis() + ".xls";
        // 目录
        String dirPath = FileConstants.FILE_URL_ORD_ACCOUNT;
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File outFile = new File(dir, fileName);

        try (Workbook workbook = new XSSFWorkbook(); FileOutputStream fos = new FileOutputStream(outFile)) {
            Sheet sheet = workbook.createSheet("科目初始化");

            // 表头样式
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // 写表头
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // 写数据
            if (CollUtil.isNotEmpty(list)) {
                for (int i = 0; i < list.size(); i++) {
                    SubjectReq item = list.get(i);
                    Row row = sheet.createRow(i + 1);
                    // 依次对应：科目编码、科目名称、科目类别、余额方向、数量核算、辅助核算、旧账科目编码
                    // 允许对应字段缺省，判空后写入
                    row.createCell(0).setCellValue(Optional.ofNullable(item.getCode()).orElse(""));
                    row.createCell(1).setCellValue(Optional.ofNullable(item.getName()).orElse(""));
                    row.createCell(2).setCellValue(Optional.ofNullable(item.getCategory()).orElse(""));
                    row.createCell(3).setCellValue(Optional.ofNullable(item.getBalanceDirection()).orElse(""));
                    row.createCell(4).setCellValue(Optional.ofNullable(item.getQuantityAccounting()).orElse(""));
                    row.createCell(5).setCellValue(Optional.ofNullable(item.getAuxiliaryAccounting()).orElse(""));
                    row.createCell(6).setCellValue(Optional.ofNullable(item.getOldAccountCode()).orElse(""));
                }
            }

            // 自适应列宽
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                // 稍作扩展，避免中文被截断
                int current = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.min(current + 512, 10000));
            }

            workbook.write(fos);
        } catch (IOException e) {
            throw new BusinessException("保存科目初始化文件失败:" + e.getMessage());
        }
        // 返回完整文件路径
        return outFile.getAbsolutePath();
    }

}