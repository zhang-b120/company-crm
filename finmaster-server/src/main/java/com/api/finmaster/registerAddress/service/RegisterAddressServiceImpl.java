package com.qykj.finmaster.registerAddress.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.exception.BusinessException;
import com.qykj.finmaster.common.utils.DateUtils;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.registerAddress.domain.RegisterAddress;
import com.qykj.finmaster.registerAddress.domain.req.AddRegisterAddressReq;
import com.qykj.finmaster.registerAddress.domain.req.GetRegisterAddressListReq;
import com.qykj.finmaster.registerAddress.domain.req.UpdateRegisterAddressReq;
import com.qykj.finmaster.registerAddress.domain.resp.GetRegisterAddressListResp;
import com.qykj.finmaster.registerAddress.manager.impl.RegisterAddressManagerImpl;
import com.qykj.finmaster.registerAddress.mapper.RegisterAddressMapper;
import com.qykj.finmaster.security.LoginUser;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 注册地址Service实现
 */
@Service
public class RegisterAddressServiceImpl extends ServiceImpl<RegisterAddressMapper, RegisterAddress> {

    @Autowired
    private RegisterAddressManagerImpl registerAddressManager;
    @Autowired
    private RegisterAddressMapper registerAddressMapper;

    /**
     * 查询注册地址列表（分页）
     */
    public List<GetRegisterAddressListResp> getRegisterAddressList(GetRegisterAddressListReq req, Long tenantId) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        params.put("tenantId", tenantId);
        List<GetRegisterAddressListResp> list = registerAddressMapper.getRegisterAddressList(params);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        list.forEach(item -> {
            if (item.getCreateTime() != null) {
                item.setCreateTime(DateUtils.dateToLocalDateTime(DateUtils.localDateTimeToDate(item.getCreateTime())));
            }
        });
        return list;
    }

    /**
     * 新增注册地址
     */
    public Boolean addRegisterAddress(AddRegisterAddressReq req, Long tenantId, LoginUser loginUser) {
        if (StringUtils.isEmpty(req.getAddress())) {
            throw new BusinessException("地址不能为空！");
        }
        RegisterAddress registerAddress = CopierCopyUtils.convertTo(req, RegisterAddress.class);
        registerAddress.setTenantId(tenantId);
        registerAddress.setStatus(GlobalConstant.Number.ZERO_INT); // 默认未使用
        if (req.getEnable() == null) {
            registerAddress.setEnable(GlobalConstant.Number.ONE_INT); // 默认启用
        }
        registerAddress.setCreateTime(new Date());
        if (ObjectUtils.isNotEmpty(loginUser)) {
            registerAddress.setCreateBy(loginUser.getUsername());
            registerAddress.setCreateByName(loginUser.getRealName());
        }
        return registerAddressManager.save(registerAddress);
    }

    /**
     * 编辑注册地址
     */
    public Boolean updateRegisterAddress(UpdateRegisterAddressReq req, Long tenantId, LoginUser loginUser) {
        if (ObjectUtils.isEmpty(req.getId())) {
            throw new BusinessException("未获取到要编辑的地址ID！");
        }
        if (StringUtils.isEmpty(req.getAddress())) {
            throw new BusinessException("地址不能为空！");
        }
        RegisterAddress registerAddress = CopierCopyUtils.convertTo(req, RegisterAddress.class);
        registerAddress.setTenantId(tenantId);
        registerAddress.setUpdateTime(new Date());
        if (ObjectUtils.isNotEmpty(loginUser)) {
            registerAddress.setUpdateBy(loginUser.getUsername());
            registerAddress.setUpdateByName(loginUser.getRealName());
        }
        return registerAddressManager.updateById(registerAddress);
    }

    /**
     * 删除注册地址（逻辑删除：显式将 deleted 置为 1，避免 @TableLogic 导致 updateById 未更新该字段）
     */
    public Boolean deleteRegisterAddress(Long id, LoginUser loginUser) {
        if (ObjectUtils.isEmpty(id)) {
            return Boolean.FALSE;
        }
        RegisterAddress registerAddress = registerAddressManager.getById(id);
        if (ObjectUtils.isEmpty(registerAddress)) {
            return Boolean.TRUE;
        }
        boolean ok = registerAddressManager.lambdaUpdate()
                .set(RegisterAddress::getDeleted, GlobalConstant.Number.ONE_INT)
                .set(RegisterAddress::getUpdateTime, new Date())
                .set(ObjectUtils.isNotEmpty(loginUser), RegisterAddress::getUpdateBy, loginUser != null ? loginUser.getUsername() : null)
                .set(ObjectUtils.isNotEmpty(loginUser), RegisterAddress::getUpdateByName, loginUser != null ? loginUser.getRealName() : null)
                .eq(RegisterAddress::getId, id)
                .update();
        return ok;
    }

    /**
     * 获取注册地址详情
     */
    public RegisterAddress getRegisterAddressDetail(Long id) {
        return registerAddressManager.getById(id);
    }

    /**
     * 更新地址状态
     */
    public Boolean updateAddressStatus(Long id, Integer status, LoginUser loginUser) {
        if (ObjectUtils.isEmpty(id) || ObjectUtils.isEmpty(status)) {
            return Boolean.FALSE;
        }
        RegisterAddress registerAddress = registerAddressManager.getById(id);
        if (ObjectUtils.isEmpty(registerAddress)) {
            return Boolean.FALSE;
        }
        registerAddress.setStatus(status);
        registerAddress.setUpdateTime(new Date());
        if (ObjectUtils.isNotEmpty(loginUser)) {
            registerAddress.setUpdateBy(loginUser.getUsername());
            registerAddress.setUpdateByName(loginUser.getRealName());
        }
        return registerAddressManager.updateById(registerAddress);
    }

    /**
     * 启用/禁用地址
     */
    public Boolean updateAddressEnable(Long id, Integer enable, LoginUser loginUser) {
        if (ObjectUtils.isEmpty(id) || ObjectUtils.isEmpty(enable)) {
            return Boolean.FALSE;
        }
        RegisterAddress registerAddress = registerAddressManager.getById(id);
        if (ObjectUtils.isEmpty(registerAddress)) {
            return Boolean.FALSE;
        }
        registerAddress.setEnable(enable);
        registerAddress.setUpdateTime(new Date());
        if (ObjectUtils.isNotEmpty(loginUser)) {
            registerAddress.setUpdateBy(loginUser.getUsername());
            registerAddress.setUpdateByName(loginUser.getRealName());
        }
        return registerAddressManager.updateById(registerAddress);
    }
}
