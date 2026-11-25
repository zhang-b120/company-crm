package com.qykj.finmaster.system.dict.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.dict.entity.SysDictType;
import com.qykj.finmaster.system.dict.manager.DictTypeManager;
import com.qykj.finmaster.system.dict.mapper.SysDictTypeMapper;
import org.springframework.stereotype.Service;

@Service
public class DictTypeManagerImpl extends ManagerImpl<SysDictTypeMapper, SysDictType> implements DictTypeManager {
}