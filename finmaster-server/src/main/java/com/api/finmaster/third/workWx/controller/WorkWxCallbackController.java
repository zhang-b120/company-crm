package com.qykj.finmaster.third.workWx.controller;

import com.qykj.finmaster.common.core.domain.constansts.WxConstants;
import com.qykj.finmaster.common.utils.third.WxBizMsgCrypt;
import com.qykj.finmaster.system.bizConfig.service.SysBizConfigService;
import com.qykj.finmaster.third.workWx.service.WorkWxMsgAuditSyncServiceImpl;
import com.qykj.finmaster.third.workWx.service.WorkWxServiceImpl;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 企业微信回调控制器（工单 WCB-02）。
 *
 * <p>两路径：
 * <ul>
 *   <li>{@code /workWx/callback/app}：自建应用「接收消息」回调（APP Token/AESKey）。
 *       GET 验签返回 echostr 明文；POST 解密后取 Event=msgaudit_notify → 异步触发会话存档增量同步。</li>
 *   <li>{@code /workWx/callback/contact}：客户联系「客户变更回调」（CONTACT Token/AESKey）。
 *       GET 同上；POST 取 InfoType=change_external_chat 的 ChatId：dismiss 软删该群，否则刷新群信息。</li>
 * </ul>
 *
 * <p>健壮性：验签失败 / receiveId≠CorpID 拒绝（返回空串）；事件按业务键排重；
 * 异常一律 catch 后返回空串 200 + log；Token/AESKey 未配置时忽略该路径、不抛异常。
 * 全程只读/写 work_wx_group_chat 与触发既有服务，不改任何业务模块。
 */
@RestController
@RequestMapping("/workWx/callback")
@PermitAll
public class WorkWxCallbackController {

    private static final Logger log = LoggerFactory.getLogger(WorkWxCallbackController.class);

    /** 回调成功后须返回的空串（企业微信要求返回空或 success，且 5s 内）。 */
    private static final String EMPTY = "";

    /** 同步模式配置键（工单 83 / WXSYNC-01），与 WorkWxMsgAuditSyncTask 一致。 */
    private static final String CFG_SYNC_MODE = "wecom.msgaudit.sync_mode";
    private static final String MODE_CALLBACK = "CALLBACK";

    @Autowired
    private WorkWxMsgAuditSyncServiceImpl workWxMsgAuditSyncService;

    @Autowired
    private WorkWxServiceImpl workWxService;

    @Autowired
    private SysBizConfigService bizConfigService;

    /** 事件排重（有界 LRU，约 2000 条），防企业微信重推导致重复处理。 */
    private final Map<String, Boolean> dedupCache = Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>(512, 0.75f, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > 2000;
                }
            });

    /* ===================== APP 回调（msgaudit_notify） ===================== */

    @GetMapping("/app")
    public String verifyApp(@RequestParam("msg_signature") String msgSignature,
                            @RequestParam("timestamp") String timestamp,
                            @RequestParam("nonce") String nonce,
                            @RequestParam("echostr") String echostr) {
        return doVerify(appCrypt(), "app", msgSignature, timestamp, nonce, echostr);
    }

    @PostMapping("/app")
    public String receiveApp(@RequestParam("msg_signature") String msgSignature,
                             @RequestParam("timestamp") String timestamp,
                             @RequestParam("nonce") String nonce,
                             @RequestBody(required = false) String body) {
        WxBizMsgCrypt crypt = appCrypt();
        if (crypt == null) {
            return EMPTY;
        }
        try {
            String plain = crypt.decryptMsg(msgSignature, timestamp, nonce, body);
            Map<String, String> fields = parseXml(plain);
            String event = fields.get("Event");
            // 排重键：FromUserName + CreateTime
            String dedupKey = "app:" + fields.get("FromUserName") + ":" + fields.get("CreateTime") + ":" + event;
            if (!firstSeen(dedupKey)) {
                return EMPTY;
            }
            if ("msgaudit_notify".equalsIgnoreCase(event)) {
                // 工单83/WXSYNC-01：仅 CALLBACK 模式才由回调触发同步；POLL 模式下回调不触发（避免与轮询并发重入）。
                // 验签与返回 EMPTY 不受影响；读配置异常缺省不触发（按 POLL 处理，交定时任务）。
                if (isCallbackMode()) {
                    // 异步不阻塞回调返回；下游 syncIncremental 自带游标幂等 + tryLock 防重入（WXSYNC-02）
                    CompletableFuture.runAsync(() -> {
                        try {
                            workWxMsgAuditSyncService.syncIncremental();
                        } catch (Exception e) {
                            log.error("[WorkWxCallback] msgaudit_notify 异步同步失败", e);
                        }
                    });
                } else {
                    log.debug("[WorkWxCallback] 同步模式非 CALLBACK，msgaudit_notify 不触发同步（交定时轮询）");
                }
            }
        } catch (Exception e) {
            log.warn("[WorkWxCallback] app 回调处理异常: {}", e.getMessage());
        }
        return EMPTY;
    }

    /* ===================== CONTACT 回调（change_external_chat） ===================== */

    @GetMapping("/contact")
    public String verifyContact(@RequestParam("msg_signature") String msgSignature,
                                @RequestParam("timestamp") String timestamp,
                                @RequestParam("nonce") String nonce,
                                @RequestParam("echostr") String echostr) {
        return doVerify(contactCrypt(), "contact", msgSignature, timestamp, nonce, echostr);
    }

    @PostMapping("/contact")
    public String receiveContact(@RequestParam("msg_signature") String msgSignature,
                                 @RequestParam("timestamp") String timestamp,
                                 @RequestParam("nonce") String nonce,
                                 @RequestBody(required = false) String body) {
        WxBizMsgCrypt crypt = contactCrypt();
        if (crypt == null) {
            return EMPTY;
        }
        try {
            String plain = crypt.decryptMsg(msgSignature, timestamp, nonce, body);
            Map<String, String> fields = parseXml(plain);
            String infoType = fields.get("InfoType");
            if ("change_external_chat".equalsIgnoreCase(infoType)) {
                String chatId = fields.get("ChatId");
                String changeType = fields.get("ChangeType");
                // 排重键：TimeStamp + ChatId + ChangeType
                String dedupKey = "contact:" + fields.get("TimeStamp") + ":" + chatId + ":" + changeType;
                if (!firstSeen(dedupKey)) {
                    return EMPTY;
                }
                if (chatId != null && !chatId.isEmpty()) {
                    if ("dismiss".equalsIgnoreCase(changeType)) {
                        workWxService.markGroupDismissed(chatId);
                    } else {
                        // create / update / add_member / del_member / change_owner 等 → 刷新群信息
                        workWxService.refreshOneGroup(chatId);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[WorkWxCallback] contact 回调处理异常: {}", e.getMessage());
        }
        return EMPTY;
    }

    /* ===================== 内部工具 ===================== */

    /** GET 验签通用逻辑：成功返回 echostr 明文，失败/未配置返回空串。 */
    private String doVerify(WxBizMsgCrypt crypt, String tag, String msgSignature,
                            String timestamp, String nonce, String echostr) {
        if (crypt == null) {
            log.warn("[WorkWxCallback] {} Token/AESKey 未配置，验签忽略", tag);
            return EMPTY;
        }
        try {
            return crypt.verifyURL(msgSignature, timestamp, nonce, echostr);
        } catch (Exception e) {
            log.warn("[WorkWxCallback] {} URL 验签失败: {}", tag, e.getMessage());
            return EMPTY;
        }
    }

    private WxBizMsgCrypt appCrypt() {
        return buildCrypt(WxConstants.WORK_WX_APP_CALLBACK_TOKEN, WxConstants.WORK_WX_APP_CALLBACK_AESKEY);
    }

    private WxBizMsgCrypt contactCrypt() {
        return buildCrypt(WxConstants.WORK_WX_CONTACT_CALLBACK_TOKEN, WxConstants.WORK_WX_CONTACT_CALLBACK_AESKEY);
    }

    /** 构造加解密器；Token/AESKey 为空时返回 null（忽略该路径，不抛异常）。 */
    private WxBizMsgCrypt buildCrypt(String token, String aesKey) {
        if (token == null || token.trim().isEmpty() || aesKey == null || aesKey.trim().isEmpty()) {
            return null;
        }
        try {
            return new WxBizMsgCrypt(token, aesKey, WxConstants.WORK_WX_CORP_ID);
        } catch (Exception e) {
            log.error("[WorkWxCallback] 初始化加解密器失败（请检查 EncodingAESKey 是否 43 位）: {}", e.getMessage());
            return null;
        }
    }

    /** 当前同步模式是否为 CALLBACK（读配置异常缺省 false，即按 POLL 处理，不由回调触发）。 */
    private boolean isCallbackMode() {
        try {
            return MODE_CALLBACK.equalsIgnoreCase(bizConfigService.getString(CFG_SYNC_MODE, "POLL"));
        } catch (Throwable e) {
            log.warn("[WorkWxCallback] 读取同步模式失败，按非 CALLBACK 处理：{}", e.getMessage());
            return false;
        }
    }

    /** 首次出现返回 true；重复返回 false。 */
    private boolean firstSeen(String key) {
        if (key == null) {
            return true;
        }
        synchronized (dedupCache) {
            if (dedupCache.containsKey(key)) {
                return false;
            }
            dedupCache.put(key, Boolean.TRUE);
            return true;
        }
    }

    /** 解析回调明文 XML 的一级子节点为 Map（节点名 → 文本）。 */
    private Map<String, String> parseXml(String xml) {
        Map<String, String> map = new LinkedHashMap<>();
        if (xml == null || xml.isEmpty()) {
            return map;
        }
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setExpandEntityReferences(false);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Element root = db.parse(new InputSource(new StringReader(xml))).getDocumentElement();
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                if (children.item(i) instanceof Element) {
                    Element el = (Element) children.item(i);
                    map.put(el.getNodeName(), el.getTextContent());
                }
            }
        } catch (Exception e) {
            log.warn("[WorkWxCallback] 明文 XML 解析失败: {}", e.getMessage());
        }
        return map;
    }
}
