package com.qykj.finmaster.cs.profile.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.cs.profile.domain.CsCustomerProfile;
import com.qykj.finmaster.cs.profile.manager.CsCustomerProfileManager;
import com.qykj.finmaster.cs.profile.mapper.CsCustomerProfileMapper;
import org.springframework.stereotype.Component;

@Component
public class CsCustomerProfileManagerImpl extends ManagerImpl<CsCustomerProfileMapper, CsCustomerProfile>
        implements CsCustomerProfileManager {
}
