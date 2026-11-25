package com.qykj.finmaster.third.workWx.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessage;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageManager;
import com.qykj.finmaster.third.workWx.mapper.WorkWxMessageMapper;
import org.springframework.stereotype.Service;

/**
 * @Date 2026-06-19
 */
@Service
public class WorkWxMessageManagerImpl extends ManagerImpl<WorkWxMessageMapper, WorkWxMessage> implements WorkWxMessageManager {
}
