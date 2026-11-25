package com.qykj.finmaster.oa.expense.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.expense.domain.OaExpenseItem;
import com.qykj.finmaster.oa.expense.manager.OaExpenseItemManager;
import com.qykj.finmaster.oa.expense.mapper.OaExpenseItemMapper;
import org.springframework.stereotype.Service;

/**
 * 报销明细Manager实现
 */
@Service
public class OaExpenseItemManagerImpl extends ManagerImpl<OaExpenseItemMapper, OaExpenseItem> implements OaExpenseItemManager {
}
