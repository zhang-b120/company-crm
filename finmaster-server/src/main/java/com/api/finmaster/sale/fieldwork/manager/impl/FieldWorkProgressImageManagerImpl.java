package com.qykj.finmaster.sale.fieldwork.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.sale.fieldwork.entity.FieldWorkProgressImage;
import com.qykj.finmaster.sale.fieldwork.manager.FieldWorkProgressImageManager;
import com.qykj.finmaster.sale.fieldwork.mapper.FieldWorkProgressImageMapper;
import org.springframework.stereotype.Service;

@Service
public class FieldWorkProgressImageManagerImpl
        extends ManagerImpl<FieldWorkProgressImageMapper, FieldWorkProgressImage>
        implements FieldWorkProgressImageManager {
}
