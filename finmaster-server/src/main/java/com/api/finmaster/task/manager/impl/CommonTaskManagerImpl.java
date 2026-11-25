package com.qykj.finmaster.task.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.task.domain.CommonTask;
import com.qykj.finmaster.task.manager.CommonTaskManager;
import com.qykj.finmaster.task.mapper.CommonTaskMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-06-03
 * @Description
 */
@Service
public class CommonTaskManagerImpl extends ManagerImpl<CommonTaskMapper, CommonTask> implements CommonTaskManager {
}