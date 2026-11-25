package com.qykj.finmaster.task.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.task.domain.CommonTaskDetail;
import com.qykj.finmaster.task.manager.CommonTaskDetailManager;
import com.qykj.finmaster.task.mapper.CommonTaskDetailMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-06-03
 * @Description
 */
@Service
public class CommonTaskDetailManagerImpl extends ManagerImpl<CommonTaskDetailMapper, CommonTaskDetail> implements CommonTaskDetailManager {
}