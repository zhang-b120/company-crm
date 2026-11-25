package com.qykj.finmaster.system.dict.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.dict.entity.SysDictType;
import com.qykj.finmaster.system.dict.entity.req.AddDictTypeReq;
import com.qykj.finmaster.system.dict.entity.req.UpdateDictTypeReq;
import com.qykj.finmaster.system.dict.entity.req.GetDictTypePageReq;
import com.qykj.finmaster.system.dict.entity.resp.GetDictDataListResp;
import com.qykj.finmaster.system.dict.entity.resp.GetDictTypePageResp;
import com.qykj.finmaster.system.dict.manager.impl.DictDataManagerImpl;
import com.qykj.finmaster.system.dict.manager.impl.DictTypeManagerImpl;
import com.qykj.finmaster.system.dict.mapper.SysDictTypeMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 字典类型Service实现类
 */
@Service
public class SysDictTypeServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> {

    private static final Logger logger = LoggerFactory.getLogger(SysDictTypeServiceImpl.class);

    @Autowired
    private SysDictDataServiceImpl dictDataService;
    @Autowired
    private DictDataManagerImpl dictDataManager;
    @Autowired
    private DictTypeManagerImpl dictTypeManager;

    public SysDictType getByCode(String code) {
        return baseMapper.selectByCode(code);
    }

    public List<GetDictDataListResp> getDictMap(String code) {
        SysDictType dictType = getByCode(code);
        if (dictType == null) {
            return Collections.emptyList();
        }
        return dictDataService.listByTypeId(dictType.getId());
    }

    /**
     * 获取字典类型列表
     * @param req 查询请求
     * @param tenantId 租户ID
     * @return 字典类型列表
     */
    public List<GetDictTypePageResp> selectDictTypeList(GetDictTypePageReq req, Long tenantId) {
        req.startPage();
        Map<String, Object> params = req.toBeanMap();
        List<GetDictTypePageResp> respList = dictTypeManager.getBaseMapper().selectDictTypeList(params);
        if(CollUtil.isEmpty(respList)){
            return Collections.emptyList();
        }
        return respList;
    }

    /**
     * 新增字典类型
     * @param req 新增请求
     * @param tenantId 租户ID
     * @param loginUser 登录用户
     * @return 操作结果
     */
    public Boolean addDictType(AddDictTypeReq req, Long tenantId, LoginUser loginUser) {
        SysDictType dictType = CopierCopyUtils.convertTo(req, SysDictType.class);
        dictType.setTenantId(tenantId);
        if(ObjectUtils.isNotEmpty(loginUser)) {
            dictType.setCreateBy(loginUser.getUsername());
            dictType.setCreateByName(loginUser.getRealName());
        }
        dictType.setCreateTime(new Date());
        return dictTypeManager.save(dictType);
    }

    /**
     * 更新字典类型
     *
     * @param req       更新请求
     * @param tenantId
     * @param loginUser 登录用户
     * @return 操作结果
     */
    public Boolean updateDictType(UpdateDictTypeReq req, Long tenantId, LoginUser loginUser) {
        SysDictType dictType = CopierCopyUtils.convertTo(req, SysDictType.class);
        dictType.setTenantId(tenantId);
        if(ObjectUtils.isNotEmpty(loginUser)) {
            dictType.setUpdateBy(loginUser.getUsername());
            dictType.setUpdateByName(loginUser.getRealName());
        }
        dictType.setUpdateTime(new Date());
        return dictTypeManager.updateById(dictType);
    }

    /**
     * 删除字典类型
     * @param id 字典类型ID
     * @return 操作结果
     */
    public Boolean removeDictTypeById(Long id) {
        return dictTypeManager.removeById(id);
    }
}
