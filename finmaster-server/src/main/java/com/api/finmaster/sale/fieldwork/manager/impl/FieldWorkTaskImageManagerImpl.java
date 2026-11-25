package com.qykj.finmaster.sale.fieldwork.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.sale.fieldwork.entity.FieldWorkTaskImage;
import com.qykj.finmaster.sale.fieldwork.manager.FieldWorkTaskImageManager;
import com.qykj.finmaster.sale.fieldwork.mapper.FieldWorkTaskImageMapper;
import org.springframework.stereotype.Service;

@Service
public class FieldWorkTaskImageManagerImpl extends ManagerImpl<FieldWorkTaskImageMapper, FieldWorkTaskImage>
        implements FieldWorkTaskImageManager {
}
