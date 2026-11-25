package com.qykj.finmaster.system.dict.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.dict.entity.SysDictData;
import com.qykj.finmaster.system.dict.manager.DictDataManager;
import com.qykj.finmaster.system.dict.mapper.SysDictDataMapper;
import org.springframework.stereotype.Service;

@Service
public class DictDataManagerImpl extends ManagerImpl<SysDictDataMapper, SysDictData> implements DictDataManager {
}