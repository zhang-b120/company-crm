package com.qykj.finmaster.system.product.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.product.domain.Product;
import com.qykj.finmaster.system.product.manager.ProductManager;
import com.qykj.finmaster.system.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-06-17
 * @Description
 */
@Service
public class ProductManagerImpl extends ManagerImpl<ProductMapper, Product> implements ProductManager {
}