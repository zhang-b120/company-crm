package com.qykj.finmaster.system.productType.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.productType.domain.ProductType;
import com.qykj.finmaster.system.productType.manager.ProductTypeManager;
import com.qykj.finmaster.system.productType.mapper.ProductTypeMapper;
import org.springframework.stereotype.Service;

@Service
public class ProductTypeManagerImpl extends ManagerImpl<ProductTypeMapper, ProductType> implements ProductTypeManager {
}
