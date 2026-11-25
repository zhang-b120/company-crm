package com.qykj.finmaster.third.workWx.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessageMedia;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageMediaManager;
import com.qykj.finmaster.third.workWx.mapper.WorkWxMessageMediaMapper;
import org.springframework.stereotype.Service;

@Service
public class WorkWxMessageMediaManagerImpl extends ManagerImpl<WorkWxMessageMediaMapper, WorkWxMessageMedia>
        implements WorkWxMessageMediaManager {
}
