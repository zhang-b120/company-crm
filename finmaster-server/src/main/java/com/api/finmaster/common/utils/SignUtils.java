package com.qykj.finmaster.common.utils;

import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.security.PrivateKey;
import java.util.ResourceBundle;

import org.apache.commons.codec.binary.StringUtils;

/**
 * @Author zhb
 * @Date 2025-06-11
 * @Description
 *      注：
 */
@Slf4j
public class SignUtils {

    //读取配置文件
    private static final ResourceBundle RESOURCE = ResourceBundle.getBundle("config");

    private static final String PRIVATE_KEY  = RESOURCE.getString("privateKey");

    private static final String PUBLIC_KEY = RESOURCE.getString("publicKey");

    private static final String MD5_KEY = RESOURCE.getString("md5Key");

    /**
     * MD5加密
     * @param data 原文
     * @return
     */
    public static String signWithMD5(String data){
        String result = "";
        try {
            //MD5摘要算法
            result = MD5Utils.md5(data, MD5_KEY);
        } catch (Exception e) {
            log.error("md5加密失败" + e);
        }
        return result;
    }

    /**
     * MD5验密
     * @param data 加密之后数据
     * @param md5 加密之后数据
     * @return
     */
    public static Boolean verifyWithMD5(String data, String md5){
        Boolean result = null;
        try {
            //MD5摘要算法
            result = MD5Utils.verify(data, MD5_KEY, md5);
        } catch (Exception e) {
            log.error("md5验密异常：" + e);
        }
        return result;
    }

    /**
     * RSA加密 公钥加密
     * @param data 需加密数据
     * @return 密文
     */
    public static String signWithRSA(String data){
        byte[] dataByte = null;
        String result = null;
        byte[] publicKeyByte = null;
        try {
            dataByte = data.getBytes(GlobalConstant.UTF8);
            publicKeyByte =  PUBLIC_KEY.getBytes(GlobalConstant.UTF8);
            result = Base64.encodeBase64String(RSAUtils.subEncryptByPublicKey(dataByte, publicKeyByte));
        }catch(Exception  e){
            log.error("加密异常：" + e);
        }
        return result;
    }

    /**
     * RSA解密 私钥解密
     * @param sign 加密数据
     * @return 原文
     */
    public static String decryptWithRSA(String sign){
        byte[] privateKeyByte = null;
        byte[] signByte = null;
        String result = null;
        try{
            signByte = sign.getBytes(GlobalConstant.UTF8);
            privateKeyByte = PRIVATE_KEY.getBytes(GlobalConstant.UTF8);
            byte[] resultByte = RSAUtils.subDecryptByPrivateKey(Base64.decodeBase64(signByte),Base64.decodeBase64(privateKeyByte));
            result = StringUtils.newStringUtf8(resultByte);
        }catch(Exception e){
            log.error("解密异常：" + e);
        }
        return result.trim();
    }


    /**
     * 私钥签名
     * @param data
     * @return 签名
     * @Description
     *      签名是推荐使用此方法
     *      先使用MD5进行加密， 然后对加密数据进行签名
     */
    public static String MD5Sign(String data){
        String result = "";
        byte[] resultByte = null;
        try {
            //MD5摘要算法
            String digest = MD5Utils.md5(data, MD5_KEY);
            byte[] digestByte = StringUtils.getBytesUtf8(digest);
            result = Base64.encodeBase64String(digestByte);
            //私钥加签
            resultByte = RSAUtils.sign(digestByte, Base64.decodeBase64(PRIVATE_KEY));
            result = Base64.encodeBase64String(resultByte);
        } catch (Exception e) {
            log.error("md5加签失败:" + e);
        }
        return result;
    }

    /**
     * 公钥验签
     * @param data 原数据
     * @param sign 签名值
     * @return
     */
    public static Boolean MD5Verify(String data, String sign){
        boolean flag = false;
        try {
            //MD5摘要算法
            String digest = MD5Utils.md5(data, MD5_KEY);
            byte[] digestByte = StringUtils.getBytesUtf8(digest);
            //公钥验签
            flag = RSAUtils.verify(digestByte, Base64.decodeBase64(sign), Base64.decodeBase64(PUBLIC_KEY));
        } catch (Exception e) {
            log.error("验签异常：" + e);
        }
        return flag;
    }

    /**
     * RSA签名
     * @param data 需加密数据
     * @param privateKey 需加密数据
     * @param signType 签名类型
     * @return 密文
     */
    public static String RSASign(String data, PrivateKey privateKey, String signType){
        byte[] dataByte = null;
        String result = null;
        try {
            dataByte = data.getBytes(GlobalConstant.UTF8);
            result = Base64.encodeBase64String(RSAUtils.sign(dataByte, privateKey, signType));
        }catch(Exception  e){
            log.error("签名异常：" + e);
        }
        return result;
    }
}