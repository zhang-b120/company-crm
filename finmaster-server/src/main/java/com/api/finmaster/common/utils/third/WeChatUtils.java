package com.qykj.finmaster.common.utils.third;


import com.alibaba.fastjson.JSON;
import com.qykj.finmaster.common.utils.WebUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.Security;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author zhb
 * @Date 2025-05-14
 * @Description
 */
@Slf4j
public class WeChatUtils {

    //APPID
    private static String APP_ID = "wx852890a17d0529ed";
    //SECRET
    private static String SECRET = "5e562e72b6de7e747b1f3472bcf99969";
    //获取openID请求地址
    private static String OPENID_URL = "https://api.weixin.qq.com/sns/jscode2session?appid=YOUR_APPID&secret=YOUR_SECRET&js_code=CODE&grant_type=authorization_code";
    //获取token地址
    private static String GET_TOKEN = "https://api.weixin.qq.com/cgi-bin/token?appid=YOUR_APPID&secret=YOUR_SECRET&grant_type=client_credential";
    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 根据客户code 获取客户openID
     *
     * @param code
     * @return
     */
    public static String getOpenId(String code) {
        String resultJson = getData(code);
        if(ObjectUtils.isNotEmpty(resultJson)) {
            String openId = JSON.parseObject(resultJson).getString("openid");
            return openId;
        }
        return null;
    }

    /**
     * 根据客户code 获取客户openID
     *
     * @param code
     * @return
     */
    public static String getSessionKey(String code) {
        String resultJson = getData(code);
        if(ObjectUtils.isNotEmpty(resultJson)) {
            String sessionKey = JSON.parseObject(resultJson).getString("session_key");
            return sessionKey;
        }
        return null;
    }

    //获取微信数据
    private static String getData(String code){
        String url = OPENID_URL.replace("YOUR_APPID",APP_ID).replace("YOUR_SECRET",SECRET).replace("CODE",code);
        try {
            Map<String,Object> map = new HashMap<>();
            String resultJson = WebUtils.doPostReString(url, map, WebUtils.DEFAULT_CHARSET, WebUtils.DEFAULT_CHARSET, 5000, 5000);
            return resultJson;
        } catch (IOException e) {
            log.info("获取微信数据失败" + e.getMessage());
        }
        return null;
    }

    //解密数据
    public static String decrypt(String code, String encryptedData, String iv){
        // 被加密的数据
        byte[] dataByte = Base64.getDecoder().decode(encryptedData);
        // 加密秘钥
        byte[] keyByte = Base64.getDecoder().decode(getSessionKey(code));
        // 偏移量
        byte[] ivByte = Base64.getDecoder().decode(iv);

        // 如果密钥不足16位，那么就补足.  这个if中的内容很重要
        int base = 16;
        if (keyByte.length % base != 0) {
            int groups = keyByte.length / base + (keyByte.length % base != 0 ? 1 : 0);
            byte[] temp = new byte[groups * base];
            System.arraycopy(keyByte, 0, temp, 0, keyByte.length);
            keyByte = temp;
        }

        try {
            // 初始化
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
            SecretKeySpec spec = new SecretKeySpec(keyByte, "AES");
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("AES");
            parameters.init(new IvParameterSpec(ivByte));
            cipher.init(Cipher.DECRYPT_MODE, spec, parameters);
            byte[] resultByte = cipher.doFinal(dataByte);
            if (resultByte != null && resultByte.length > 0) {
                return new String(resultByte, "UTF-8");
            }
        }catch (Exception e){
            log.info("解密数据失败:" + e);
        }
        return null;
    }

    //获取token
    public static String getToken(){
        String url = GET_TOKEN.replace("YOUR_APPID",APP_ID).replace("YOUR_SECRET",SECRET);
        try {
            Map<String,Object> map = new HashMap<>();
            String resultJson = WebUtils.doPostReString(url, map, WebUtils.DEFAULT_CHARSET, WebUtils.DEFAULT_CHARSET, 5000, 5000);
            return JSON.parseObject(resultJson).getString("access_token");
        } catch (IOException e) {
            log.info("获取token失败" + e.getMessage());
        }
        return null;
    }
}
