package com.qykj.finmaster.common.utils.third;

import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.core.domain.constansts.BaiduConstants;
import com.qykj.finmaster.common.core.domain.message.ResultMsg;
import com.qykj.finmaster.common.utils.WebUtils;
import com.qykj.finmaster.common.utils.third.req.GetGeneralBasicReq;
import com.qykj.finmaster.common.utils.third.resp.GetBaiduAccessTokenResp;
import org.springframework.cache.annotation.Cacheable;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-09-10
 * @Description
 */
public class BaiduUtils {

    /**
     * 发送请求获取token
     * @return
     */
    public static GetBaiduAccessTokenResp getBaiduAiAccessToken(){
        GetBaiduAccessTokenResp resp = new GetBaiduAccessTokenResp();
        Map<String,Object> params = new HashMap<>();
        params.put("grant_type","client_credentials");
        params.put("client_id", BaiduConstants.API_KEY);
        params.put("client_secret", BaiduConstants.SECRET_KEY);
        try {
            String result = WebUtils.doGet(BaiduConstants.GET_ACCESS_TOKEN_URL, params);
            JSONObject jsonObject = JSONObject.parseObject(result);
            if(jsonObject.containsKey("error")){
                System.out.println(jsonObject.getString("error_description"));
            }
            resp.setAccessToken(jsonObject.getString("access_token"));
            resp.setRefreshToken(jsonObject.getString("refresh_token"));
            resp.setExpiresIn(jsonObject.getLong("expires_in"));
            resp.setScope(jsonObject.getString("scope"));
            resp.setSessionKey(jsonObject.getString("session_key"));
            resp.setSessionSecret(jsonObject.getString("session_secret"));
        } catch (Exception e) {
            System.out.println(MessageFormat.format(ResultMsg.BAIDU_ACCESS_TOKEN_ERROR,e));
        }
        return resp;
    }

    /**
     * 通用文字识别
     * @param req
     * @param accessToken
     * @return
     */
    public static String generalBasic(GetGeneralBasicReq req, String accessToken) throws Exception{
        // 请求url
        String url = getUrlSpAT(BaiduConstants.GENERAL_BASIC, accessToken);
        Map<String, Object> params = req.toBeanMap();
        String result = WebUtils.doPostReString(url, params, WebUtils.CONTENT_TYPE_FORM, WebUtils.DEFAULT_CHARSET, 5000, 5000);
        return result;
    }

    /**
     * 获取完整请求url
     * @param url
     * @param accessToken
     * @return
     */
    public static String getUrlSpAT(String url, String accessToken) {
        return url.concat("?access_token=").concat(accessToken);
    }
}