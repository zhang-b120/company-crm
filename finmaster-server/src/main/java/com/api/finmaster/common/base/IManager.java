package com.qykj.finmaster.common.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 自定义Manager接口
 * @param <M> Mapper类型
 * @param <T> 实体类型
 */
public interface IManager<M extends BaseMapper<T>, T> extends IService<T> {
    /**
     * 获取Mapper实例
     * @return Mapper实例
     */
    M getBaseMapper();
} 