package com.qykj.finmaster.oa.trip.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.oa.trip.domain.OaBusinessTripLeg;
import com.qykj.finmaster.oa.trip.manager.OaBusinessTripLegManager;
import com.qykj.finmaster.oa.trip.mapper.OaBusinessTripLegMapper;
import org.springframework.stereotype.Service;

/**
 * 出差行程段Manager实现
 */
@Service
public class OaBusinessTripLegManagerImpl extends ManagerImpl<OaBusinessTripLegMapper, OaBusinessTripLeg> implements OaBusinessTripLegManager {
}
