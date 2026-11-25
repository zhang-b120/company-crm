package com.qykj.finmaster.third.idcard.manager;

import com.qykj.finmaster.common.base.IManager;
import com.qykj.finmaster.third.idcard.entity.IdCardVerifyRecord;
import com.qykj.finmaster.third.idcard.mapper.IdCardVerifyRecordMapper;

/**
 * 身份证核验结果缓存 Manager（工单90 / IDV-03）。
 */
public interface IdCardVerifyRecordManager extends IManager<IdCardVerifyRecordMapper, IdCardVerifyRecord> {
}
