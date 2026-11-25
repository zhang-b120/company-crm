package com.qykj.finmaster.common.utils;

import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;

import java.security.MessageDigest;

/**
 * @Author zhb
 * @Date 2025-06-11
 * @Description
 */
public class MD5Utils {
    public static final String MD5 = "MD5";
    /**
     * MD5方法
     * @param text 明文
     * @param key 密钥
     * @return 密文
     * @throws Exception
     */
    public static String md5(String text, String key) throws Exception {
        byte[] bytes = (text + key).getBytes(GlobalConstant.UTF8);
        MessageDigest messageDigest = MessageDigest.getInstance(MD5);
        messageDigest.update(bytes);
        bytes = messageDigest.digest();
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < bytes.length; i ++)
        {
            if((bytes[i] & 0xff) < 0x10)
            {
                sb.append("0");
            }
            sb.append(Long.toString(bytes[i] & 0xff, 16));
        }
        return sb.toString();
    }

    /**
     * MD5验证方法
     * @param text 明文
     * @param key 密钥
     * @param md5 密文
     * @return true/false
     * @throws Exception
     */
    public static boolean verify(String text, String key, String md5) throws Exception {
        String md5Text = md5(text, key);
        if(md5Text.equals(md5)){
            return true;
        }
        return false;
    }
}