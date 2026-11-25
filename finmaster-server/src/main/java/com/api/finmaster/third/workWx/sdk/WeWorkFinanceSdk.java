package com.qykj.finmaster.third.workWx.sdk;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/**
 * 企业微信「会话内容存档」官方 SDK（com.tencent.wework.Finance）的反射包装。
 *
 * <p>为什么用反射：官方 {@code WeWorkFinanceSdk_Java.jar} 不在 Maven 中央库，且本工单（73 PoC）
 * 物料（jar + native .dll/.so）尚未就位。用反射调用 {@code com.tencent.wework.Finance} 的静态 native 方法，
 * 可使本模块在 <b>无 jar 时正常编译</b>，并在 jar 到位（加入 classpath）后 <b>零改动直接生效</b>。</p>
 *
 * <p>物料到位步骤（TODO）：</p>
 * <ol>
 *   <li>把官方 {@code WeWorkFinanceSdk_Java.jar} 装入本地 Maven 仓
 *       （{@code mvn install:install-file -DgroupId=com.tencent.wework -DartifactId=wework-finance-sdk -Dversion=1.0 -Dpackaging=jar -Dfile=WeWorkFinanceSdk_Java.jar}）
 *       并在 pom.xml 增加对应 dependency（见 pom.xml 中本工单的注释块）；</li>
 *   <li>native 库置于 {@code java.library.path}：Windows 放 {@code WeWorkFinanceSdk.dll}，
 *       Linux 放 {@code libWeWorkFinanceSdk_Java.so}（启动参数 {@code -Djava.library.path=<目录>} 或系统库目录）；</li>
 *   <li>无需改本类——{@link #isAvailable()} 会变为 true，各方法反射命中真实 native 实现。</li>
 * </ol>
 *
 * 官方静态方法签名（保持一致，勿改）：
 * <pre>
 *   long   NewSdk()
 *   int    Init(long sdk, String corpid, String secret)
 *   int    GetChatData(long sdk, long seq, long limit, String proxy, String passwd, long timeout, long slice)
 *   long   NewSlice()
 *   void   FreeSlice(long slice)
 *   String GetContentFromSlice(long slice)
 *   int    DecryptData(long sdk, String encrypt_key, String encrypt_msg, long slice)
 *   void   DestroySdk(long sdk)
 * </pre>
 */
@Slf4j
public final class WeWorkFinanceSdk {

    private static final String FINANCE_CLASS = "com.tencent.wework.Finance";

    private static volatile Class<?> financeClz;
    private static volatile boolean resolved = false;

    private WeWorkFinanceSdk() {
    }

    /** 官方 Finance 类是否在 classpath（jar 是否就位）。 */
    public static boolean isAvailable() {
        return finance() != null;
    }

    private static Class<?> finance() {
        if (!resolved) {
            synchronized (WeWorkFinanceSdk.class) {
                if (!resolved) {
                    try {
                        financeClz = Class.forName(FINANCE_CLASS);
                    } catch (Throwable e) {
                        // 包含 ClassNotFoundException（无 jar）与 UnsatisfiedLinkError（无 native 库，static{} loadLibrary 失败）
                        log.warn("[WeWorkFinanceSdk] 官方 SDK 未就位（{}）：{}", FINANCE_CLASS, e.toString());
                        financeClz = null;
                    }
                    resolved = true;
                }
            }
        }
        return financeClz;
    }

    private static Method method(String name, Class<?>... paramTypes) {
        Class<?> clz = finance();
        if (clz == null) {
            throw new IllegalStateException("企业微信会话存档 SDK 未就位：classpath 缺少 " + FINANCE_CLASS
                    + " 或 native 库未在 java.library.path（见 WeWorkFinanceSdk 类注释 TODO）");
        }
        try {
            return clz.getMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("SDK 方法签名不匹配：" + name, e);
        }
    }

    private static Object invoke(String name, Class<?>[] paramTypes, Object... args) {
        try {
            return method(name, paramTypes).invoke(null, args);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("调用 SDK." + name + " 失败：" + e.getMessage(), e);
        }
    }

    public static long newSdk() {
        return (long) invoke("NewSdk", new Class<?>[]{});
    }

    public static int init(long sdk, String corpid, String secret) {
        return (int) invoke("Init", new Class<?>[]{long.class, String.class, String.class}, sdk, corpid, secret);
    }

    public static int getChatData(long sdk, long seq, long limit, String proxy, String passwd, long timeout, long slice) {
        return (int) invoke("GetChatData",
                new Class<?>[]{long.class, long.class, long.class, String.class, String.class, long.class, long.class},
                sdk, seq, limit, proxy, passwd, timeout, slice);
    }

    public static long newSlice() {
        return (long) invoke("NewSlice", new Class<?>[]{});
    }

    public static void freeSlice(long slice) {
        invoke("FreeSlice", new Class<?>[]{long.class}, slice);
    }

    public static String getContentFromSlice(long slice) {
        // 注意：此处返回的是整段 JSON 明文，切勿在此整体 re-decode——GBK→UTF-8 还原会"吃掉"与中文相邻的结构字节（如 "），
        // 破坏 JSON 结构导致 parseObject 报 syntax error。乱码修复改在 JSON 解析后、对单个文本字段做（见 WorkWxMsgAuditSyncServiceImpl）。
        return (String) invoke("GetContentFromSlice", new Class<?>[]{long.class}, slice);
    }

    /**
     * 修正"UTF-8 被当 GBK 解码"产生的乱码（工单85/WXMSG-06）。
     * <b>只对单个文本字段值（如 text.content）调用，不要对整段 JSON 调用。</b>
     * 双重守卫，保证不误伤已正确的字符串：
     *  ① 无损 GBK 往返（s.getBytes("GBK") 再按 GBK 解码须等于 s——确保全部字符 GBK 可表示、无 '?' 替换，含 emoji 的正确串在此被排除）；
     *  ② 把 GBK 字节按 UTF-8 重解码后不得含替换符 U+FFFD（已正确的 GBK 可表示 CJK，其 GBK 字节按 UTF-8 解几乎必产生 �，于是被拒、保持原值）。
     * 仅两条都满足才采用还原值。生产 GBK 乱码（全 GBK 字符、往返无损、重解为合法 UTF-8）被还原；UTF-8 dev/ASCII 串保持不变。
     */
    public static String fixGbkMojibake(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        try {
            byte[] gbkBytes = s.getBytes("GBK");
            if (!new String(gbkBytes, "GBK").equals(s)) {
                return s; // GBK 往返有损（含非 GBK 字符，如 emoji）→ 本就不是 GBK 乱码，原样返回
            }
            String utf8 = new String(gbkBytes, StandardCharsets.UTF_8);
            if (utf8.indexOf('�') >= 0) {
                return s; // 重解为 UTF-8 出现替换符 → 原串本就是正确文本，原样返回
            }
            return utf8;
        } catch (Exception e) {
            return s;
        }
    }

    public static int decryptData(long sdk, String encryptKey, String encryptMsg, long slice) {
        return (int) invoke("DecryptData",
                new Class<?>[]{long.class, String.class, String.class, long.class}, sdk, encryptKey, encryptMsg, slice);
    }

    public static void destroySdk(long sdk) {
        invoke("DestroySdk", new Class<?>[]{long.class}, sdk);
    }

    /* ===== 媒体分片下载（工单 75 / WXMSG-03）。官方签名：
     *   long   NewMediaData()
     *   void   FreeMediaData(long mediaData)
     *   int    GetMediaData(long sdk, String indexbuf, String sdkFileid, String proxy, String passwd, long timeout, long mediaData)
     *   byte[] GetData(long mediaData)
     *   String GetOutIndexBuf(long mediaData)
     *   int    IsMediaDataFinish(long mediaData)
     * 分片循环：用上次 GetOutIndexBuf 作下次 indexbuf，直到 IsMediaDataFinish==1。 ===== */

    public static long newMediaData() {
        return (long) invoke("NewMediaData", new Class<?>[]{});
    }

    public static void freeMediaData(long mediaData) {
        invoke("FreeMediaData", new Class<?>[]{long.class}, mediaData);
    }

    public static int getMediaData(long sdk, String indexbuf, String sdkFileid, String proxy, String passwd, long timeout, long mediaData) {
        return (int) invoke("GetMediaData",
                new Class<?>[]{long.class, String.class, String.class, String.class, String.class, long.class, long.class},
                sdk, indexbuf, sdkFileid, proxy, passwd, timeout, mediaData);
    }

    public static byte[] getData(long mediaData) {
        return (byte[]) invoke("GetData", new Class<?>[]{long.class}, mediaData);
    }

    public static String getOutIndexBuf(long mediaData) {
        return (String) invoke("GetOutIndexBuf", new Class<?>[]{long.class}, mediaData);
    }

    public static int isMediaDataFinish(long mediaData) {
        return (int) invoke("IsMediaDataFinish", new Class<?>[]{long.class}, mediaData);
    }
}
