package com.qykj.finmaster.common.utils.third;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 企业微信回调消息加解密（工单 WCB-01）。
 *
 * <p>单文件实现，等价于企业微信官方 Java 示例 {@code WXBizMsgCrypt}（含 SHA1 / PKCS7 / XML 抽取 / AesException）。
 * 算法：
 * <ul>
 *   <li>验签：SHA1(sort(token, timestamp, nonce, encrypt))；</li>
 *   <li>解密：AES-256-CBC，key = Base64Decode(encodingAesKey + "=")（32B），IV = key 前 16B，PKCS7 去填充；</li>
 *   <li>明文结构：random(16B) + msgLen(4B 网络序大端) + msg + receiveId，并校验 receiveId == 构造入参（CorpID）。</li>
 * </ul>
 *
 * 构造入参：token、encodingAesKey（43 位）、receiveId（企业 CorpID）。
 */
public class WxBizMsgCrypt {

    /** 加解密异常（对齐官方错误码语义）。 */
    public static class AesException extends Exception {
        public static final int OK = 0;
        public static final int ValidateSignatureError = -40001;
        public static final int ParseXmlError = -40002;
        public static final int ComputeSignatureError = -40003;
        public static final int IllegalAesKey = -40004;
        public static final int ValidateCorpidError = -40005;
        public static final int EncryptAESError = -40006;
        public static final int DecryptAESError = -40007;
        public static final int IllegalBuffer = -40008;

        private final int code;

        public AesException(int code) {
            super(message(code));
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        private static String message(int code) {
            switch (code) {
                case ValidateSignatureError: return "签名验证错误";
                case ParseXmlError: return "xml解析失败";
                case ComputeSignatureError: return "sha加密生成签名失败";
                case IllegalAesKey: return "SymmetricKey非法";
                case ValidateCorpidError: return "corpid校验失败";
                case EncryptAESError: return "aes加密失败";
                case DecryptAESError: return "aes解密失败";
                case IllegalBuffer: return "解密后得到的buffer非法";
                default: return "未知错误";
            }
        }
    }

    private final byte[] aesKey;
    private final String token;
    private final String receiveId;

    public WxBizMsgCrypt(String token, String encodingAesKey, String receiveId) throws AesException {
        if (encodingAesKey == null || encodingAesKey.length() != 43) {
            throw new AesException(AesException.IllegalAesKey);
        }
        this.token = token;
        this.receiveId = receiveId;
        this.aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
    }

    /**
     * 验证回调 URL（企业微信「保存」时 GET 验证）。
     * @return 解密后的明文消息（echostr 明文），原样 Urlencode 之外直接返回即可
     */
    public String verifyURL(String msgSignature, String timestamp, String nonce, String echoStr) throws AesException {
        String signature = sha1(token, timestamp, nonce, echoStr);
        if (!signature.equals(msgSignature)) {
            throw new AesException(AesException.ValidateSignatureError);
        }
        return decrypt(echoStr);
    }

    /**
     * 解密回调推送的密文消息（POST body 为 XML，取其中 &lt;Encrypt&gt;）。
     * @return 解密后的明文 XML
     */
    public String decryptMsg(String msgSignature, String timestamp, String nonce, String postData) throws AesException {
        String encrypt = extractEncrypt(postData);
        String signature = sha1(token, timestamp, nonce, encrypt);
        if (!signature.equals(msgSignature)) {
            throw new AesException(AesException.ValidateSignatureError);
        }
        return decrypt(encrypt);
    }

    /* ===================== 内部实现 ===================== */

    /** SHA1(sort(token, timestamp, nonce, encrypt)) 十六进制小写串。 */
    private static String sha1(String token, String timestamp, String nonce, String encrypt) throws AesException {
        try {
            String[] array = new String[]{token, timestamp, nonce, encrypt};
            Arrays.sort(array);
            StringBuilder sb = new StringBuilder();
            for (String s : array) {
                sb.append(s);
            }
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                String shaHex = Integer.toHexString(b & 0xFF);
                if (shaHex.length() < 2) {
                    hex.append(0);
                }
                hex.append(shaHex);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new AesException(AesException.ComputeSignatureError);
        }
    }

    /** AES-256-CBC 解密 + 去 PKCS7 填充 + 校验 receiveId。 */
    private String decrypt(String text) throws AesException {
        byte[] original;
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            byte[] encrypted = Base64.getDecoder().decode(text);
            original = cipher.doFinal(encrypted);
        } catch (Exception e) {
            throw new AesException(AesException.DecryptAESError);
        }

        String content;
        String fromReceiveId;
        try {
            byte[] bytes = pkcs7Decode(original);
            // bytes: random(16) + msgLen(4, 网络序) + msg + receiveId
            byte[] networkOrder = Arrays.copyOfRange(bytes, 16, 20);
            int msgLen = bytesToInt(networkOrder);
            content = new String(Arrays.copyOfRange(bytes, 20, 20 + msgLen), StandardCharsets.UTF_8);
            fromReceiveId = new String(Arrays.copyOfRange(bytes, 20 + msgLen, bytes.length), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new AesException(AesException.IllegalBuffer);
        }

        if (receiveId != null && !receiveId.isEmpty() && !fromReceiveId.equals(receiveId)) {
            throw new AesException(AesException.ValidateCorpidError);
        }
        return content;
    }

    /** 4 字节网络序（大端）转 int。 */
    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24)
                | ((bytes[1] & 0xFF) << 16)
                | ((bytes[2] & 0xFF) << 8)
                | (bytes[3] & 0xFF);
    }

    /** 去除 PKCS7 填充。 */
    private static byte[] pkcs7Decode(byte[] decrypted) {
        int pad = decrypted[decrypted.length - 1] & 0xFF;
        if (pad < 1 || pad > 32) {
            pad = 0;
        }
        return Arrays.copyOfRange(decrypted, 0, decrypted.length - pad);
    }

    /** 从回调 POST body（XML）中抽取 &lt;Encrypt&gt; 节点文本。 */
    private static String extractEncrypt(String xml) throws AesException {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            // 关闭外部实体，防 XXE
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setExpandEntityReferences(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(new InputSource(new StringReader(xml)));
            Element root = document.getDocumentElement();
            NodeList nodelist = root.getElementsByTagName("Encrypt");
            if (nodelist.getLength() == 0) {
                throw new AesException(AesException.ParseXmlError);
            }
            return nodelist.item(0).getTextContent();
        } catch (AesException ae) {
            throw ae;
        } catch (Exception e) {
            throw new AesException(AesException.ParseXmlError);
        }
    }
}
