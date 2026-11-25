package com.qykj.finmaster.third.workWx.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.third.workWx.entity.WorkWxResponseMetric;
import com.qykj.finmaster.third.workWx.manager.WorkWxResponseMetricManager;
import com.qykj.finmaster.third.workWx.mapper.WorkWxResponseMetricMapper;
import org.springframework.stereotype.Service;

@Service
public class WorkWxResponseMetricManagerImpl extends ManagerImpl<WorkWxResponseMetricMapper, WorkWxResponseMetric>
        implements WorkWxResponseMetricManager {
}
