package com.qykj.finmaster.sale.fieldwork.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.sale.fieldwork.entity.FieldWorkProgressRecord;
import com.qykj.finmaster.sale.fieldwork.manager.FieldWorkProgressRecordManager;
import com.qykj.finmaster.sale.fieldwork.mapper.FieldWorkProgressRecordMapper;
import org.springframework.stereotype.Service;

@Service
public class FieldWorkProgressRecordManagerImpl
        extends ManagerImpl<FieldWorkProgressRecordMapper, FieldWorkProgressRecord>
        implements FieldWorkProgressRecordManager {
}
