package com.qykj.finmaster.oa.expense.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.expense.domain.OaExpense;
import com.qykj.finmaster.oa.expense.manager.OaExpenseManager;
import com.qykj.finmaster.oa.expense.mapper.OaExpenseMapper;
import org.springframework.stereotype.Service;

/**
 * 报销Manager实现
 */
@Service
public class OaExpenseManagerImpl extends ManagerImpl<OaExpenseMapper, OaExpense> implements OaExpenseManager {
}
