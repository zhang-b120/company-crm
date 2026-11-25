package com.qykj.finmaster.system.taxPlaceLocation.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.system.taxPlaceLocation.entity.TaxPlaceLocationUser;
import com.qykj.finmaster.system.taxPlaceLocation.manager.TaxPlaceLocationUserManager;
import com.qykj.finmaster.system.taxPlaceLocation.mapper.TaxPlaceLocationUserMapper;
import org.springframework.stereotype.Service;

@Service
public class TaxPlaceLocationUserManagerImpl extends ManagerImpl<TaxPlaceLocationUserMapper, TaxPlaceLocationUser> implements TaxPlaceLocationUserManager {
}
