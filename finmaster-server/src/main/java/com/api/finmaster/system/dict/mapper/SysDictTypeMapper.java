package com.qykj.finmaster.system.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qykj.finmaster.system.dict.entity.SysDictType;
import com.qykj.finmaster.system.dict.entity.resp.GetDictTypePageResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 字典类型Mapper接口
 */
@Mapper
public interface SysDictTypeMapper extends BaseMapper<SysDictType> {
    
    /**
     * 根据字典编码查询字典类型
     *
     * @param code 字典编码
     * @return 字典类型
     */
    SysDictType selectByCode(@Param("code") String code);

    /**
     * 分页查询字典类型列表
     *
     * @param page 分页参数
     * @param name 字典名称
     * @param code 字典编码
     * @return 字典类型分页列表
     */
    List<GetDictTypePageResp> selectDictTypeList(@Param("params") Map<String, Object> params);
}
