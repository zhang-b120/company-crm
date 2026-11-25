package com.qykj.finmaster.system.taxPlaceLocation.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.taxPlaceLocation.entity.TaxPlaceLocation;
import com.qykj.finmaster.system.taxPlaceLocation.manager.TaxPlaceLocationManager;
import com.qykj.finmaster.system.taxPlaceLocation.mapper.TaxPlaceLocationMapper;
import org.springframework.stereotype.Service;

@Service
public class TaxPlaceLocationManagerImpl extends ManagerImpl<TaxPlaceLocationMapper, TaxPlaceLocation> implements TaxPlaceLocationManager {
}
