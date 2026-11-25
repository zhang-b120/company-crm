package com.qykj.finmaster.common.utils.third;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.http.HttpRequest;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.core.domain.constansts.SlsjConstants;
import com.qykj.finmaster.common.utils.MD5Utils;
import com.qykj.finmaster.common.utils.third.req.SlsjIdCardReq;
import com.qykj.finmaster.common.utils.third.resp.SlsjIdCardResp;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 随联数聚 身份证二要素实名核验工具（工单88 / IDV-01）。
 * 仿 {@link QccUtil}：全 static、无状态、不进数据库；MD5 用项目自有 {@link MD5Utils}。
 *
 * <p>安全：日志绝不打身份证明文，不整体打印含 idCard 的 form/req（仅前6后4脱敏）。</p>
 *
 * @Author zhb
 * @Date 2026-06-23
 */
@Slf4j
public class SlsjIdCardUtil {

    private static final int TIME_OUT = 10000;

    /**
     * 身份证二要素核验（姓名 + 身份证号）。
     * @param req name/idCard 必填，orderId 可空（空则自动生成）
     * @return 核验结果；网络/解析异常返回 code 非 0 的兜底响应（绝不抛出影响调用方）
     */
    public static SlsjIdCardResp verify(SlsjIdCardReq req) {
        SlsjIdCardResp resp = new SlsjIdCardResp();
        String orderId = (req != null && StringUtils.isNotBlank(req.getOrderId()))
                ? req.getOrderId()
                : System.currentTimeMillis() + RandomUtil.randomNumbers(4);
        resp.setOrderId(orderId);

        try {
            String body = HttpRequest.post(SlsjConstants.ID_CARD_AUTH_URL)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Auth_Token", buildAuthToken())
                    .header("Sign_Type", "md5")
                    .form("orderId", orderId)
                    .form("name", req.getName())
                    .form("idCard", req.getIdCard())
                    .timeout(TIME_OUT)
                    .execute()
                    .body();

            JSONObject root = JSON.parseObject(body);
            if (root == null) {
                log.error("[slsj-idcard] 返回体为空 idCard={}", mask(req.getIdCard()));
                resp.setCode(-1);
                resp.setMsg("核验服务无响应");
                return resp;
            }
            Integer code = root.getInteger("code");
            String msg = root.getString("msg");
            resp.setCode(code);
            resp.setMsg(msg);

            JSONObject result = root.getJSONObject("result");
            if (code != null && code == 0 && result != null) {
                String rc = result.getString("result");
                resp.setResultCode(rc);
                resp.setConsistent(SlsjConstants.RESULT_CONSISTENT.equals(rc));
                resp.setOrderNumber(result.getString("orderNumber"));
                if (StringUtils.isNotBlank(result.getString("orderId"))) {
                    resp.setOrderId(result.getString("orderId"));
                }
                resp.setGender(result.getString("gender"));
                resp.setBirthday(result.getString("birthday"));
                resp.setAge(result.getString("age"));
                resp.setProvince(result.getString("province"));
                resp.setCity(result.getString("city"));
                resp.setCountry(result.getString("country"));
            } else {
                // code!=0：签名/白名单/余额/审核等错误，交调用方按 code 处理；此处只 log，不打明文
                log.error("[slsj-idcard] 核验失败 code={} msg={} idCard={}", code, msg, mask(req.getIdCard()));
            }
        } catch (Exception e) {
            log.error("[slsj-idcard] 核验异常 idCard={}：{}", mask(req == null ? null : req.getIdCard()), e.getMessage());
            resp.setCode(-1);
            resp.setMsg("核验请求异常：" + e.getMessage());
        }
        return resp;
    }

    /**
     * 组装 Auth_Token（有序 JSON）。
     * 签名：md5("memberId=..dateTime=..version=..key=..") 小写（整串已拼好，故 key 传空）。
     */
    protected static String buildAuthToken() {
        long dateTime = System.currentTimeMillis();
        String raw = "memberId=" + SlsjConstants.MEMBER_ID
                + "dateTime=" + dateTime
                + "version=" + SlsjConstants.VERSION
                + "key=" + SlsjConstants.APP_KEY;
        String signMd5;
        try {
            signMd5 = MD5Utils.md5(raw, ""); // md5(text+key) 小写；勿用 DigestUtils、勿 toUpperCase
        } catch (Exception e) {
            throw new RuntimeException("[slsj-idcard] 签名失败", e);
        }
        JSONObject token = new JSONObject(true); // 有序
        token.put("signMd5", signMd5); // 官方文档键名为 signMd5（用 sign 会 102 签名错误）
        token.put("version", SlsjConstants.VERSION);
        token.put("dateTime", dateTime);
        token.put("memberId", SlsjConstants.MEMBER_ID);
        return token.toJSONString();
    }

    /** 身份证脱敏：前6后4，中间星号。 */
    public static String mask(String idCard) {
        if (StringUtils.isBlank(idCard)) {
            return "";
        }
        String s = idCard.trim();
        if (s.length() <= 10) {
            return "****";
        }
        return s.substring(0, 6) + "********" + s.substring(s.length() - 4);
    }
}
