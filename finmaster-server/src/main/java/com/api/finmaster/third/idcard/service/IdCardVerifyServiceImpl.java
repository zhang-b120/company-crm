package com.qykj.finmaster.third.idcard.service;

import com.qykj.finmaster.common.core.domain.constansts.SlsjConstants;
import com.qykj.finmaster.common.utils.third.SlsjIdCardUtil;
import com.qykj.finmaster.common.utils.third.req.SlsjIdCardReq;
import com.qykj.finmaster.common.utils.third.resp.SlsjIdCardResp;
import com.qykj.finmaster.security.LoginUser;
import com.qykj.finmaster.third.idcard.entity.IdCardVerifyRecord;
import com.qykj.finmaster.third.idcard.manager.IdCardVerifyRecordManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.regex.Pattern;

/**
 * 身份证二要素实名核验编排（工单88 / IDV-02 重构：复杂逻辑从 controller 下沉 service）。
 *
 * <p>承载：预校验（不过直接返回失败、不计费）→ 调 {@link SlsjIdCardUtil#verify} → 瞬时错误退避重试 ≤2
 * → 配置类错误码不重试原样透出 → 身份证脱敏日志。{@code SlsjIdCardUtil} 保持纯第三方调用，由本类编排。</p>
 *
 * @Author zhb
 * @Date 2026-06-24
 */
@Slf4j
@Service
public class IdCardVerifyServiceImpl {

    /** 18 位身份证号正则（17 位数字 + 校验位数字/X） */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");

    /** 最大重试次数（仅瞬时错误：网络/4xx/5xx，不计费、退避） */
    private static final int MAX_RETRY = 2;

    @Autowired
    private IdCardVerifyRecordManager recordManager;
    @Autowired
    private IdCardVerifyCacheService cacheService;

    /**
     * 实名核验：姓名 + 身份证号二要素（缓存优先，工单90 / IDV-03）。
     * ① 预校验 → ② 命中缓存(已一致)直接返回、不调 API/不计费 → ③ 未命中调 API(重试/错误分流)
     * → ④ resultCode==01 入库缓存 → ⑤ 02/03/04/code!=0 不入库原样返回。
     */
    public SlsjIdCardResp verify(String name, String idCard) {
        // ① 预校验：name 非空 + idCard 18 位，不过直接返回失败、不计费
        if (StringUtils.isBlank(name)) {
            return invalid("姓名不能为空");
        }
        String ic = idCard == null ? "" : idCard.trim();
        if (!ID_CARD_PATTERN.matcher(ic).matches()) {
            return invalid("身份证号格式不正确（应为 18 位）");
        }
        String nm = name.trim();
        Long tenantId = currentTenantId();

        // ② 缓存命中（已一致）→ 直接返回，不调 API、不计费
        IdCardVerifyRecord cached = findCache(tenantId, nm, ic);
        if (cached != null) {
            log.info("[slsj-idcard] 命中缓存 name={} idCard={}", nm, SlsjIdCardUtil.mask(ic));
            return fromCache(cached);
        }

        // ③ 未命中 → 调 API（含重试/错误分流）
        SlsjIdCardReq req = new SlsjIdCardReq();
        req.setName(nm);
        req.setIdCard(ic);
        SlsjIdCardResp resp = verifyWithRetry(req);

        // ④ 仅一致(resultCode=01) 才入库缓存；02/03/04/code!=0 不入库
        if (resp != null && resp.getCode() != null && resp.getCode() == 0
                && SlsjConstants.RESULT_CONSISTENT.equals(resp.getResultCode())) {
            saveCache(tenantId, nm, ic, resp);
        }
        return resp;
    }

    /** 当前登录租户（service 层从 SecurityContext 取，与 BaseController.getLoginUser().getTenantId() 同源）。 */
    private Long currentTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            return ((LoginUser) auth.getPrincipal()).getTenantId();
        }
        return null;
    }

    private IdCardVerifyRecord findCache(Long tenantId, String name, String idCard) {
        return recordManager.lambdaQuery()
                .eq(tenantId != null, IdCardVerifyRecord::getTenantId, tenantId)
                .isNull(tenantId == null, IdCardVerifyRecord::getTenantId)
                .eq(IdCardVerifyRecord::getName, name)
                .eq(IdCardVerifyRecord::getIdCard, idCard)
                .last("limit 1")
                .one();
    }

    private SlsjIdCardResp fromCache(IdCardVerifyRecord c) {
        SlsjIdCardResp r = new SlsjIdCardResp();
        r.setCode(0);
        r.setMsg("命中缓存");
        r.setResultCode(c.getResultCode());
        r.setConsistent(Boolean.TRUE); // 仅缓存一致结果
        r.setOrderNumber(c.getOrderNumber());
        r.setGender(c.getGender());
        r.setBirthday(c.getBirthday());
        r.setAge(c.getAge());
        r.setProvince(c.getProvince());
        r.setCity(c.getCity());
        r.setCountry(c.getCountry());
        return r;
    }

    /**
     * 写缓存：组装记录后交独立 bean {@link IdCardVerifyCacheService#save}（REQUIRES_NEW 独立提交，不随外层回滚）。
     * 失败只 warn、不影响核验返回。
     */
    private void saveCache(Long tenantId, String name, String idCard, SlsjIdCardResp resp) {
        try {
            IdCardVerifyRecord rec = new IdCardVerifyRecord();
            rec.setTenantId(tenantId);
            rec.setName(name);
            rec.setIdCard(idCard);
            rec.setResultCode(resp.getResultCode());
            rec.setConsistent(1);
            rec.setGender(resp.getGender());
            rec.setBirthday(resp.getBirthday());
            rec.setAge(resp.getAge());
            rec.setProvince(resp.getProvince());
            rec.setCity(resp.getCity());
            rec.setCountry(resp.getCountry());
            rec.setOrderNumber(resp.getOrderNumber());
            rec.setVerifyTime(new Date());
            rec.setSource("SLSJ");
            cacheService.save(rec); // 经 Spring 代理调用，REQUIRES_NEW 生效；uk 去重在其内完成
        } catch (Exception e) {
            log.warn("[slsj-idcard] 写核验缓存失败 idCard={}：{}", SlsjIdCardUtil.mask(idCard), e.getMessage());
        }
    }

    private SlsjIdCardResp invalid(String msg) {
        SlsjIdCardResp resp = new SlsjIdCardResp();
        resp.setCode(-1);
        resp.setMsg(msg);
        return resp;
    }

    /** 仅对瞬时错误（网络/4xx/5xx）有限退避重试；业务码（签名/白名单/余额/审核等）与结果码不重试，避免重复计费。 */
    private SlsjIdCardResp verifyWithRetry(SlsjIdCardReq req) {
        SlsjIdCardResp resp = SlsjIdCardUtil.verify(req);
        int attempt = 0;
        while (attempt < MAX_RETRY && isTransient(resp.getCode())) {
            attempt++;
            try {
                Thread.sleep(300L * attempt); // 线性退避
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
            log.warn("[slsj-idcard] 瞬时错误重试 第{}次 code={} idCard={}", attempt, resp.getCode(), SlsjIdCardUtil.mask(req.getIdCard()));
            resp = SlsjIdCardUtil.verify(req);
        }
        return resp;
    }

    /** 瞬时错误：本地网络/解析(-1)、网关 4xx/5xx；这些不计费、可重试。 */
    private boolean isTransient(Integer code) {
        if (code == null) return true;
        return code == -1 || code == 400 || code == 401 || code == 500;
    }
}
