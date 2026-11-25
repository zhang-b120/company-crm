package com.qykj.finmaster.sale.fieldwork.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.sale.fieldwork.entity.FieldWorkTask;
import com.qykj.finmaster.sale.fieldwork.manager.FieldWorkTaskManager;
import com.qykj.finmaster.sale.fieldwork.mapper.FieldWorkTaskMapper;
import org.springframework.stereotype.Service;

@Service
public class FieldWorkTaskManagerImpl extends ManagerImpl<FieldWorkTaskMapper, FieldWorkTask>
        implements FieldWorkTaskManager {
}
