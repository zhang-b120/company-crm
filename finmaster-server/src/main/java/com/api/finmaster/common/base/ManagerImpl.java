package com.qykj.finmaster.common.base;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

/**
 * 自定义Manager实现类
 * @param <M> Mapper类型
 * @param <T> 实体类型
 */
public class ManagerImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements IManager<M, T> {
    @Override
    public M getBaseMapper() {
        return super.getBaseMapper();
    }
} 