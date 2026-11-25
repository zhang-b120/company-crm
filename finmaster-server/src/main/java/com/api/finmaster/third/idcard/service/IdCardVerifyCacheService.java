package com.qykj.finmaster.third.idcard.service;

import com.qykj.finmaster.third.idcard.entity.IdCardVerifyRecord;
import com.qykj.finmaster.third.idcard.manager.IdCardVerifyRecordManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 身份证核验缓存写入（工单90 / IDV-03 复审修复）。
 *
 * <p>独立 bean + {@link Propagation#REQUIRES_NEW}：缓存写入**独立提交**，不随外层
 * createWorkOrder/saveBookkeepingDispatch 事务回滚——"此人 name+id_card 一致"是 API 客观事实，
 * 与本次保存成败无关；否则核验非 01 抛异常回滚时，本批已命中 01 的缓存会一并回滚，重试重复计费。</p>
 *
 * <p>⚠️ 必须放在独立 bean、经 Spring 代理调用，REQUIRES_NEW 才生效（同类私有/自调用 AOP 不生效）。</p>
 */
@Service
public class IdCardVerifyCacheService {

    @Autowired
    private IdCardVerifyRecordManager recordManager;

    /** 按 uk(tenant,name,id_card) saveOrUpdate，独立事务提交。 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(IdCardVerifyRecord r) {
        IdCardVerifyRecord existing = recordManager.lambdaQuery()
                .eq(r.getTenantId() != null, IdCardVerifyRecord::getTenantId, r.getTenantId())
                .isNull(r.getTenantId() == null, IdCardVerifyRecord::getTenantId)
                .eq(IdCardVerifyRecord::getName, r.getName())
                .eq(IdCardVerifyRecord::getIdCard, r.getIdCard())
                .last("limit 1")
                .one();
        if (existing != null) {
            r.setId(existing.getId());
        }
        recordManager.saveOrUpdate(r);
    }
}
