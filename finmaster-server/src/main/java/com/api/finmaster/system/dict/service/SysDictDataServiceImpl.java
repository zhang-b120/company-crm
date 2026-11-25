package com.qykj.finmaster.system.dict.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import com.qykj.finmaster.common.utils.copy.CopierCopyUtils;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.system.dict.entity.SysDictData;
import com.qykj.finmaster.system.dict.entity.req.AddDictDataReq;
import com.qykj.finmaster.system.dict.entity.req.UpdateDictDataReq;
import com.qykj.finmaster.system.dict.entity.resp.GetDictDataListResp;
import com.qykj.finmaster.system.dict.manager.impl.DictDataManagerImpl;
import com.qykj.finmaster.system.dict.mapper.SysDictDataMapper;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 字典数据Service实现类
 */
@Service
public class SysDictDataServiceImpl extends ServiceImpl<SysDictDataMapper, SysDictData> {

    private static final Logger logger = LoggerFactory.getLogger(SysDictDataServiceImpl.class);

    @Autowired
    private DictDataManagerImpl dictDataManager;

    public List<GetDictDataListResp> listByTypeId(Long typeId) {
        List<SysDictData> dictDataList = dictDataManager.lambdaQuery()
                .eq(SysDictData::getTypeId, typeId)
                .orderByDesc(SysDictData::getSort)
                .list();
        if(CollUtil.isEmpty(dictDataList)){
            return Collections.emptyList();
        }
        return CopierCopyUtils.convertTo(dictDataList, GetDictDataListResp.class);
    }

    /**
     * 新增字典数据
     * @param req 新增请求
     * @param tenantId 租户ID
     * @param loginUser 登录用户
     * @return 操作结果
     */
    public Boolean addDictData(AddDictDataReq req, Long tenantId, LoginUser loginUser) {
        SysDictData dictData = CopierCopyUtils.convertTo(req, SysDictData.class);
        dictData.setTenantId(tenantId);
        dictData.setCreateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)){
            dictData.setCreateBy(loginUser.getUsername());
            dictData.setCreateByName(loginUser.getRealName());
        }
        return dictDataManager.save(dictData);
    }

    /**
     * 更新字典数据
     *
     * @param req       更新请求
     * @param tenantId
     * @param loginUser 登录用户
     * @return 操作结果
     */
    public Boolean updateDictData(UpdateDictDataReq req, Long tenantId, LoginUser loginUser) {
        SysDictData dictData = CopierCopyUtils.convertTo(req, SysDictData.class);
        dictData.setTenantId(tenantId);
        dictData.setUpdateTime(new Date());
        if(ObjectUtils.isNotEmpty(loginUser)){
            dictData.setUpdateBy(loginUser.getUsername());
            dictData.setUpdateByName(loginUser.getRealName());
        }
        return dictDataManager.updateById(dictData);
    }

    /**
     * 删除字典数据
     *
     * @param id        字典数据ID
     * @param tenantId
     * @param loginUser 登录用户
     * @return 操作结果
     */
    public Boolean removeDictDataById(Long id) {
        return removeById(id);
    }

    public SysDictData selectDictDataByTypeAndLabel(String dictType, String dictLabel) {
        return dictDataManager.getBaseMapper().selectByTypeAndLabel(dictType, dictLabel);
    }

    /**
     * 根据typeCode获取字典值
     * @param code
     * @return
     */
    public List<GetDictDataListResp> getDictDataListByTypeCode(String code) {
        List<SysDictData> sysDictDataList = dictDataManager.getBaseMapper().getDictDataListByTypeCode(code);
        if(CollUtil.isEmpty(sysDictDataList)){
            return Collections.emptyList();
        }
        return CopierCopyUtils.convertTo(sysDictDataList, GetDictDataListResp.class);
    }

    /**
     * 根据字典键值获取数据 value-label
     * @param valueList
     * @return
     */
    public Map<String, String> getDictDataMap(List<String> valueList){
        List<SysDictData> dictDataList = dictDataManager.lambdaQuery()
                .in(CollUtil.isNotEmpty(valueList), SysDictData::getValue, valueList)
                .list();
        if(CollUtil.isEmpty(dictDataList)){
            return Collections.emptyMap();
        }
        return dictDataList.stream().collect(Collectors.toMap(SysDictData::getValue, SysDictData::getLabel));
    }

    /**
     * 根据字典typeCode获取数据 value-label
     * @param code
     * @return
     */
    public Map<String, String> getDictDataMapByTypeCode(String code){
        List<GetDictDataListResp> dictDataList = this.getDictDataListByTypeCode(code);
        if(CollUtil.isEmpty(dictDataList)){
            return Collections.emptyMap();
        }
        return dictDataList.stream().collect(Collectors.toMap(GetDictDataListResp::getValue, GetDictDataListResp::getLabel));
    }

    /**
     * 根据字典typeCode获取数据 value-label 批量
     * @param codeList
     * @return
     */
    public Map<String, String> getDictDataMapByTypeCode(List<String> codeList){
        List<GetDictDataListResp> dictDataList = dictDataManager.getBaseMapper().getDictDataListByTypeCodeList(codeList);
        if(CollUtil.isEmpty(dictDataList)){
            return Collections.emptyMap();
        }
        return dictDataList.stream().collect(Collectors.toMap(GetDictDataListResp::getValue, GetDictDataListResp::getLabel));
    }

    /**
     * 批量、分组获取字段集合
     * @param code
     * @return
     */
    public Map<String, List<GetDictDataListResp>> uaGetDictDataListByTypeCodeList(String code) {
        List<String> codeList = Arrays.asList(code.split(GlobalConstant.Symbol.COMMA));
        List<GetDictDataListResp> dictDataList = dictDataManager.getBaseMapper().getDictDataListByTypeCodeList(codeList);
        if(CollUtil.isEmpty(dictDataList)){
            return Collections.emptyMap();
        }
        return dictDataList.stream().collect(Collectors.groupingBy(GetDictDataListResp::getTypeCode));
    }
}
