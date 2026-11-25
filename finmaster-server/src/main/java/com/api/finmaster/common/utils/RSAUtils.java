package com.qykj.finmaster.common.utils;

import cn.hutool.crypto.digest.DigestAlgorithm;
import com.qykj.finmaster.common.core.domain.constansts.GlobalConstant;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

/**
 * @Author zhb
 * @Date 2025-06-11
 * @Description
 *      1.私钥加密-公钥验签  2.公钥加密-私钥解密
 */
public class RSAUtils {

    public static final String RSA = "RSA";
    public static final String SIGNATURE_ALGORITHM = "NONEwithRSA";
    public static final String SIGNATURE_SHA1 = "SHA1WithRSA";
    public static final String SIGNATURE_SHA256 = "SHA256withRSA";

    /**
     * RSA最大加密明文大小
     */
    private static final int MAX_ENCRYPT_BLOCK = 117;
    /**
     * RSA最大解密密文大小
     */
    private static final int MAX_DECRYPT_BLOCK = 128;

    /**
     * 私钥对数据签名(长数据)
     * @param data
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static byte[] sign(byte[] data, byte[] privateKey) throws Exception {
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        PrivateKey priKey = keyFactory.generatePrivate(pkcs8KeySpec);
        Cipher cipher = Cipher.getInstance(RSA);
        cipher.init(Cipher.ENCRYPT_MODE, priKey);
        int dataLength = data.length;
        // 标识
        int offSet = 0;
        byte[] resultBytes = {};
        byte[] cache = {};
        while (dataLength - offSet > 0) {
            if (dataLength - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
                offSet += MAX_ENCRYPT_BLOCK;
            } else {
                cache = cipher.doFinal(data, offSet, dataLength - offSet);
                offSet = dataLength;
            }
            resultBytes = Arrays.copyOf(resultBytes, resultBytes.length + cache.length);
            System.arraycopy(cache, 0, resultBytes, resultBytes.length - cache.length, cache.length);
        }
        return resultBytes;
    }

    /**
     * 公钥对数据验签
     * @param data 明文数据
     * @param signValue 签名值
     * @param publicKey 验签公钥
     * @return
     * @throws Exception
     */
    public static boolean verify(byte[] data, byte[] signValue, byte[] publicKey)
            throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKey);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        PublicKey pubKey = keyFactory.generatePublic(keySpec);
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(pubKey);
        signature.update(data);
        return signature.verify(signValue);
    }

    /**
     * 私钥对数据签名
     * @param data
     * @param privateKey
     * @return
     * @throws Exception
     */
    public static byte[] sign(byte[] data, byte[] privateKey, String signType) throws Exception {
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        PrivateKey priKey = keyFactory.generatePrivate(pkcs8KeySpec);
        Signature signature = null;

        if(DigestAlgorithm.SHA1.getValue().equals(signType)){
            signature = Signature.getInstance(SIGNATURE_SHA1);
        }else{
            signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        }

        signature.initSign(priKey);
        signature.update(data);
        return signature.sign();
    }

    /**
     * 公钥对数据验签
     * @param data 明文数据
     * @param signValue 签名值
     * @param publicKey 验签公钥
     * @return
     * @throws Exception
     */
    public static boolean verify(byte[] data, byte[] signValue, byte[] publicKey, String signType)
            throws Exception {
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKey));
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        PublicKey pubKey = keyFactory.generatePublic(keySpec);
        Signature signature = null;
        if(DigestAlgorithm.SHA1.equals(signType)){
            signature = Signature.getInstance(SIGNATURE_SHA1);
        }else{
            signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        }
        signature.initVerify(pubKey);
        signature.update(data);
        return signature.verify(signValue);
    }

    /**
     * 分段解密(针对采用分段加密的数据,要采用分段解密算法))
     * @param data 密文报文
     * @param privateKey 私钥
     * @return
     * @throws Exception
     */
    public static byte[] subDecryptByPrivateKey(byte[] data, byte[] privateKey)  throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKey);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA); //
        PrivateKey privateK = keyFactory.generatePrivate(keySpec);
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.DECRYPT_MODE, privateK);
        int inputLen = data.length;
        byte[] result = new byte[(data.length / 128 + 1) * 117];
        int offSet = 0;
        byte[] cache;
        int i = 0;
        int len = 0;
        // 对数据分段解密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_DECRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_DECRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            i++;
            offSet = i * MAX_DECRYPT_BLOCK;
            for (byte b : cache) {
                result[len++] = b;
            }
        }
        return result;
    }


    /**
     * 分段加密(针对要加密的数据过长,采用分段加密)
     * @param  data
     * @param  publicKey
     * @return byte[]
     * @throws Exception
     */
    public static byte[] subEncryptByPublicKey(byte[] data, byte[] publicKey) throws Exception {
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKey));
        KeyFactory keyFactory = KeyFactory.getInstance(RSA);
        Key publicK = keyFactory.generatePublic(x509KeySpec);
        // 对数据加密
        Cipher cipher = Cipher.getInstance(keyFactory.getAlgorithm());
        cipher.init(Cipher.ENCRYPT_MODE, publicK);
        int inputLen = data.length;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] cache;
        int i = 0;
        // 对数据分段加密
        while (inputLen - offSet > 0) {
            if (inputLen - offSet > MAX_ENCRYPT_BLOCK) {
                cache = cipher.doFinal(data, offSet, MAX_ENCRYPT_BLOCK);
            } else {
                cache = cipher.doFinal(data, offSet, inputLen - offSet);
            }
            out.write(cache, 0, cache.length);
            i++;
            offSet = i * MAX_ENCRYPT_BLOCK;
        }
        byte[] result = out.toByteArray();
        out.close();
        return result;
    }


    /**
     * 私钥对数据签名
     * @param data
     * @param priKey
     * @param signType
     * @return
     * @throws Exception
     */
    public static byte[] sign(byte[] data, PrivateKey priKey, String signType) throws Exception {
        Signature signature = null;
        if(GlobalConstant.SIGN_RSA_1.equals(signType)){
            signature = Signature.getInstance(SIGNATURE_SHA1);
        }else if(GlobalConstant.SIGN_RSA_256.equals(signType)){
            signature = Signature.getInstance(SIGNATURE_SHA256);
        }else{
            signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        }

        signature.initSign(priKey);
        signature.update(data);
        return signature.sign();
    }

}