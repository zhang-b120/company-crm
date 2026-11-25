package com.qykj.finmaster.task.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.task.domain.CommonTaskFile;
import com.qykj.finmaster.task.manager.CommonTaskFileManager;
import com.qykj.finmaster.task.mapper.CommonTaskFileMapper;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-06-03
 * @Description
 */
@Service
public class CommonTaskFileManagerImpl extends ManagerImpl<CommonTaskFileMapper, CommonTaskFile> implements CommonTaskFileManager {
}