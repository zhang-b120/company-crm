package com.qykj.finmaster.third.idcard.controller;

import com.qykj.finmaster.common.core.controller.BaseController;
import com.qykj.finmaster.common.core.domain.resp.AjaxResult;
import com.qykj.finmaster.common.utils.third.req.SlsjIdCardReq;
import com.qykj.finmaster.common.utils.third.resp.SlsjIdCardResp;
import com.qykj.finmaster.third.idcard.service.IdCardVerifyServiceImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 身份证二要素实名核验入口（工单88 / IDV-02）。
 * 登录态访问（继承 BaseController，不加 @PermitAll）；复杂逻辑（校验/重试/错误分流）在 IdCardVerifyServiceImpl。
 * 不进数据库：默认不持久化核验结果（如需在实体记"已核验"，先与产品确认）。
 *
 * @Author zhb
 * @Date 2026-06-23
 */
@RestController
@RequestMapping("/idcard")
public class IdCardVerifyController extends BaseController {

    @Autowired
    private IdCardVerifyServiceImpl idCardVerifyService;

    /** 实名核验：姓名 + 身份证号二要素。控制器只做鉴权 + 取参 + 调 service + 包 AjaxResult。 */
    @PostMapping("/verify")
    public AjaxResult verify(@RequestBody SlsjIdCardReq req) {
        if (getLoginUser() == null) {
            return AjaxResult.error("未登录");
        }
        String name = req == null ? null : req.getName();
        String idCard = req == null ? null : req.getIdCard();
        SlsjIdCardResp resp = idCardVerifyService.verify(name, idCard);
        if (resp.getCode() != null && resp.getCode() == 0) {
            return AjaxResult.success(resp);
        }
        return AjaxResult.error(StringUtils.isNotBlank(resp.getMsg()) ? resp.getMsg() : "实名核验暂不可用，请稍后重试", resp);
    }
}
