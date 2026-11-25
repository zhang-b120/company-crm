package com.qykj.finmaster.system.dict.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qykj.finmaster.system.dict.entity.SysDictData;
import com.qykj.finmaster.system.dict.entity.resp.GetDictDataListResp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 字典数据Mapper接口
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {
    
    /**
     * 根据字典类型ID查询字典数据列表
     *
     * @param typeId 字典类型ID
     * @return 字典数据列表
     */
    List<SysDictData> selectByTypeId(@Param("typeId") Long typeId);

    /**
     * 根据字典类型和标签查询字典数据
     *
     * @param dictType 字典类型
     * @param dictLabel 字典标签
     * @return 字典数据
     */
    SysDictData selectByTypeAndLabel(@Param("dictType") String dictType, @Param("dictLabel") String dictLabel);

    List<SysDictData> getDictDataListByTypeCode(@Param("code") String code);

    List<GetDictDataListResp> getDictDataListByTypeCodeList(@Param("codeList") List<String> codeList);
}
