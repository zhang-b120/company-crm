package com.qykj.finmaster.system.serviceMaintenance.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceMaintenance;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceMaintenanceProduct;
import com.qykj.finmaster.system.serviceMaintenance.domain.ServiceProcess;
import com.qykj.finmaster.system.serviceMaintenance.domain.req.AddServiceProcessListReq;
import com.qykj.finmaster.system.serviceMaintenance.domain.req.GetServiceMProcessListReq;
import com.qykj.finmaster.system.serviceMaintenance.domain.req.UpdateServiceProcessListReq;
import com.qykj.finmaster.system.serviceMaintenance.domain.resp.GetServiceProcessInfoResp;
import com.qykj.finmaster.system.serviceMaintenance.domain.resp.GetServiceProcessListResp;
import com.qykj.finmaster.system.serviceMaintenance.manager.impl.ServiceProcessManagerImpl;
import com.qykj.finmaster.system.serviceMaintenance.mapper.ServiceProcessMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class ServiceProcessServiceImpl {

    @Autowired
    private ServiceProcessMapper serviceProcessMapper;
    @Autowired
    private ServiceProcessManagerImpl serviceProcessManager;

    /**
     * 服务流程列表查询
     * @param req
     * @return
     */
    public List<GetServiceProcessListResp> list(GetServiceMProcessListReq req) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        List<GetServiceProcessListResp> respList = serviceProcessMapper.selectServiceProcessList(params);
        if(CollUtil.isEmpty(respList)){
            return Collections.emptyList();
        }
        return respList;
    }

    /**
     * 添加服务流程
     * @param req
     * @param loginUser
     * @return
     */
    public Boolean addServiceProcess(AddServiceProcessListReq req, LoginUser loginUser) {
        ServiceProcess serviceProcess = CopierCopyUtils.convertTo(req, ServiceProcess.class);
        // 校验服务名称是否存在
        this.checkProcessNameAndCode(req.getProcessName(), req.getProcessCode(), null);

        serviceProcess.setCreateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)){
            serviceProcess.setCreateBy(loginUser.getUsername());
            serviceProcess.setCreateByName(loginUser.getRealName());
        }
        serviceProcessManager.save(serviceProcess);
        return Boolean.TRUE;
    }

    /**
     * 修改服务流程
     * @param req
     * @param loginUser
     * @return
     */
    public Boolean updateServiceProcess(UpdateServiceProcessListReq req, LoginUser loginUser) {
        ServiceProcess serviceProcess = CopierCopyUtils.convertTo(req, ServiceProcess.class);
        // 校验服务名称是否存在
        this.checkProcessNameAndCode(req.getProcessName(), req.getProcessCode(), serviceProcess.getId());

        serviceProcess.setUpdateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)){
            serviceProcess.setUpdateBy(loginUser.getUsername());
            serviceProcess.setUpdateByName(loginUser.getRealName());
        }
        serviceProcessManager.updateById(serviceProcess);
        return Boolean.TRUE;
    }

    /**
     * 校验流程名称或编码是否存在
     * @param processName
     * @param processCode
     * @param processId
     */
    private void checkProcessNameAndCode(String processName, String processCode, Long processId) {
        LambdaQueryWrapper<ServiceProcess> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(ServiceProcess::getProcessName, processName)
                .ne(ObjectUtils.isNotEmpty(processId), ServiceProcess::getId, processId);
        List<ServiceProcess> cheakNameList = serviceProcessManager.list(wrapper);
        if(CollUtil.isNotEmpty(cheakNameList)){
            throw new BusinessException(ResultMsg.SERVICE_PROCESS_ADD_NAME_EXIST);
        }
        wrapper.clear();
        wrapper.eq(ServiceProcess::getProcessCode, processCode)
                .ne(ObjectUtils.isNotEmpty(processId), ServiceProcess::getId, processId);
        List<ServiceProcess> cheakCodeList = serviceProcessManager.list(wrapper);
        if(CollUtil.isNotEmpty(cheakCodeList)){
            throw new BusinessException(ResultMsg.SERVICE_PROCESS_ADD_CODE_EXIST);
        }
    }


    /**
     * 删除服务流程
     * @param id
     * @param loginUser
     * @return
     */
    public Boolean deleteServiceProcess(Long id, LoginUser loginUser) {
        if(ObjectUtils.isEmpty(id)){
            return Boolean.FALSE;
        }
        serviceProcessManager.lambdaUpdate()
                .set(ServiceProcess::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(ObjectUtils.isNotEmpty(loginUser), ServiceProcess::getUpdateBy, loginUser.getUsername())
                .set(ObjectUtils.isNotEmpty(loginUser), ServiceProcess::getUpdateByName, loginUser.getRealName())
                .set(ServiceProcess::getUpdateTime, new Date())
                .eq(ServiceProcess::getId, id)
                .update();
        return Boolean.TRUE;
    }

    /**
     * 获取流程信息
     * @param id
     * @return
     */
    public GetServiceProcessInfoResp getInfo(Long id) {
        GetServiceProcessInfoResp resp = new GetServiceProcessInfoResp();
        ServiceProcess serviceProcess = serviceProcessManager.lambdaQuery()
                .eq(ServiceProcess::getId, id)
                .one();
        if(ObjectUtils.isNotEmpty(serviceProcess)){
            resp = CopierCopyUtils.convertTo(serviceProcess, GetServiceProcessInfoResp.class);
        }
        return resp;
    }

    /**
     * 根据服务id获取对应服务流程
     * @param id
     * @return
     */
    public List<GetServiceProcessInfoResp> getListByServiceId(Long id) {
        if(ObjectUtils.isEmpty(id)){
            return Collections.emptyList();
        }
        List<ServiceProcess> serviceProcessList = serviceProcessMapper.getListByServiceId(id);
        if(CollUtil.isEmpty(serviceProcessList)){
            return Collections.emptyList();
        }
        return CopierCopyUtils.convertTo(serviceProcessList, GetServiceProcessInfoResp.class);
    }

}
