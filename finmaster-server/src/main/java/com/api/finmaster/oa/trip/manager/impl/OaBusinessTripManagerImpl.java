package com.qykj.finmaster.oa.trip.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.trip.domain.OaBusinessTrip;
import com.qykj.finmaster.oa.trip.manager.OaBusinessTripManager;
import com.qykj.finmaster.oa.trip.mapper.OaBusinessTripMapper;
import org.springframework.stereotype.Service;

/**
 * 出差申请Manager实现
 */
@Service
public class OaBusinessTripManagerImpl extends ManagerImpl<OaBusinessTripMapper, OaBusinessTrip> implements OaBusinessTripManager {
}
