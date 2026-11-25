package com.qykj.finmaster.third.wx.service;

import com.alibaba.fastjson.JSON;
import com.qykj.finmaster.common.utils.third.WeChatUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * @Author zhb
 * @Date 2025-05-27
 * @Description
 */
@Service
public class WxWebServiceImpl {

    /**
     * 获取微信openId
     * @param code
     * @return
     */
    public String getOpenId(String code){
        String openId = null;
        if(StringUtils.isNotEmpty(code)){
            openId = WeChatUtils.getOpenId(code);
        }
        return openId;
    }

    /**
     * 获取手机号
     * @param code
     * @return
     */
    public String getPhoneNum(String code, String encryptedData, String iv){
        String phoneNum = null;
        String resultJson = WeChatUtils.decrypt(code, encryptedData, iv);
        if(StringUtils.isNotEmpty(resultJson)){
            phoneNum = JSON.parseObject(resultJson).getString("phoneNumber");
        }
        return phoneNum;
    }

    public String getToken() {
        return WeChatUtils.getToken();
    }
}