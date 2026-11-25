package com.qykj.finmaster.third.idcard.manager.impl;

import com.qykj.finmaster.common.base.ManagerImpl;
import com.qykj.finmaster.third.idcard.entity.IdCardVerifyRecord;
import com.qykj.finmaster.third.idcard.manager.IdCardVerifyRecordManager;
import com.qykj.finmaster.third.idcard.mapper.IdCardVerifyRecordMapper;
import org.springframework.stereotype.Service;

/**
 * 身份证核验结果缓存 ManagerImpl（工单90 / IDV-03）。
 */
@Service
public class IdCardVerifyRecordManagerImpl extends ManagerImpl<IdCardVerifyRecordMapper, IdCardVerifyRecord>
        implements IdCardVerifyRecordManager {
}
