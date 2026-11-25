package com.qykj.finmaster.third.workWx.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.qykj.finmaster.common.core.domain.constansts.WxConstants;
import com.qykj.finmaster.third.workWx.entity.WorkWxGroupChat;
import com.qykj.finmaster.third.workWx.entity.WorkWxMessage;
import com.qykj.finmaster.third.workWx.entity.WorkWxMsgCursor;
import com.qykj.finmaster.third.workWx.manager.WorkWxGroupChatManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMessageManager;
import com.qykj.finmaster.third.workWx.manager.WorkWxMsgCursorManager;
import com.qykj.finmaster.third.workWx.sdk.WeWorkFinanceSdk;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 企业微信「会话内容存档」阶段0 PoC（工单 73 / WXMSG-01）。
 *
 * <p>独立于 {@code WorkWxServiceImpl}：只读 {@code work_wx_group_chat} 取群名/成员，
 * 不调用任何 customer / workorder / analytics / 业务模块。</p>
 *
 * <p>验证 4 件事：① native SDK 能加载并 Init；② 能拉取并解密 ≥1 条群消息；
 * ③ 用 roomid 关联群名并落库 1 条；④ 用 from 解析发送人名，并日志确认
 * 外部成员 from(external_userid) 是否与 member_list type=2 的 userid 一致。</p>
 *
 * 物料未就位时（secret/私钥/SDK 任一缺失）返回 PREREQ 状态、不实跑（骨架）。
 */
@Slf4j
@Service
public class WorkWxMsgAuditPocServiceImpl {

    /** PoC 临时租户ID（@PermitAll 无登录用户）。TODO：阶段1 接入正式租户来源。 */
    private static final long POC_TENANT_ID = 1L;
    private static final String CURSOR_KEY = "default";

    @Autowired
    private WorkWxGroupChatManager workWxGroupChatManager;
    @Autowired
    private WorkWxMessageManager workWxMessageManager;
    @Autowired
    private WorkWxMsgCursorManager workWxMsgCursorManager;

    /**
     * PoC：拉 1 批会话存档消息 → 解密 → 取第一条 roomid 非空的 text 消息 → 关联群名/发送人 → 落库 1 条。
     * @return 过程与结果（便于肉眼核对）
     */
    public Map<String, Object> syncOneMessage() {
        Map<String, Object> result = new LinkedHashMap<>();

        // ---- 物料前置校验：任一缺失则不实跑，返回骨架状态 ----
        boolean sdkReady = WeWorkFinanceSdk.isAvailable();
        boolean secretReady = StringUtils.isNotBlank(WxConstants.WORK_WX_MSGAUDIT_SECRET);
        boolean keyReady = StringUtils.isNotBlank(WxConstants.WORK_WX_MSGAUDIT_PRIVATE_KEY);
        if (!sdkReady || !secretReady || !keyReady) {
            result.put("ok", false);
            result.put("stage", "PREREQ");
            result.put("sdkAvailable", sdkReady);
            result.put("secretConfigured", secretReady);
            result.put("privateKeyConfigured", keyReady);
            result.put("message", "会话存档物料未就位（SDK jar+native / secret / RSA私钥 任一缺失），PoC 骨架已就绪，待物料到位后实跑");
            log.warn("[msgaudit-poc] 物料未就位 sdk={} secret={} key={}，跳过实跑", sdkReady, secretReady, keyReady);
            return result;
        }

        long sdk = 0L;
        long batchSlice = 0L;
        long msgSlice = 0L;
        try {
            sdk = WeWorkFinanceSdk.newSdk();
            int initRet = WeWorkFinanceSdk.init(sdk, WxConstants.WORK_WX_CORP_ID, WxConstants.WORK_WX_MSGAUDIT_SECRET);
            result.put("initRet", initRet);
            if (initRet != 0) {
                result.put("ok", false);
                result.put("stage", "INIT");
                result.put("message", "Finance.Init 失败，返回码=" + initRet);
                return result;
            }

            // 从已入库的最大 seq 之后拉取（PoC：缺省 0 起）
            long fromSeq = currentLastSeq();
            batchSlice = WeWorkFinanceSdk.newSlice();
            int dataRet = WeWorkFinanceSdk.getChatData(sdk, fromSeq, 100, "", "", 10, batchSlice);
            if (dataRet != 0) {
                result.put("ok", false);
                result.put("stage", "GET_CHAT_DATA");
                result.put("message", "Finance.GetChatData 失败，返回码=" + dataRet);
                return result;
            }
            String batchJson = WeWorkFinanceSdk.getContentFromSlice(batchSlice);
            JSONObject batch = JSON.parseObject(batchJson);
            JSONArray chatData = batch == null ? null : batch.getJSONArray("chatdata");
            if (chatData == null || chatData.isEmpty()) {
                result.put("ok", true);
                result.put("stage", "EMPTY");
                result.put("fromSeq", fromSeq);
                result.put("message", "本批无新消息（chatdata 为空）");
                return result;
            }

            // 逐条解密，取第一条 roomid 非空且 msgtype=text 的群消息落库
            for (int i = 0; i < chatData.size(); i++) {
                JSONObject enc = chatData.getJSONObject(i);
                long seq = enc.getLongValue("seq");
                String msgId = enc.getString("msgid");
                String encryptRandom = enc.getString("encrypt_random");
                String encryptChatData = enc.getString("encrypt_chat_data");

                // RSA 解 encrypt_random 得 AES key（按 publickey_ver 选私钥，PoC 单一私钥）
                String aesKey = rsaDecrypt(encryptRandom);
                msgSlice = WeWorkFinanceSdk.newSlice();
                int decRet = WeWorkFinanceSdk.decryptData(sdk, aesKey, encryptChatData, msgSlice);
                if (decRet != 0) {
                    log.warn("[msgaudit-poc] DecryptData 失败 seq={} ret={}", seq, decRet);
                    WeWorkFinanceSdk.freeSlice(msgSlice);
                    msgSlice = 0L;
                    continue;
                }
                String plain = WeWorkFinanceSdk.getContentFromSlice(msgSlice);
                WeWorkFinanceSdk.freeSlice(msgSlice);
                msgSlice = 0L;

                JSONObject msg = JSON.parseObject(plain);
                String roomId = msg.getString("roomid");
                String msgType = msg.getString("msgtype");
                if (StringUtils.isBlank(roomId) || !"text".equals(msgType)) {
                    continue; // PoC 只验群文本消息
                }

                WorkWxMessage saved = persistOne(seq, msgId, msg, roomId, result);
                updateCursor(seq);
                result.put("ok", true);
                result.put("stage", "DONE");
                result.put("plainSample", plain);
                result.put("savedMessage", saved);
                return result;
            }

            result.put("ok", true);
            result.put("stage", "NO_GROUP_TEXT");
            result.put("message", "本批已解密，但无 roomid 非空的 text 群消息");
            return result;
        } catch (Throwable e) {
            log.error("[msgaudit-poc] 执行异常", e);
            result.put("ok", false);
            result.put("stage", "ERROR");
            result.put("message", e.getMessage());
            return result;
        } finally {
            if (msgSlice != 0L) safeFree(msgSlice);
            if (batchSlice != 0L) safeFree(batchSlice);
            if (sdk != 0L) {
                try { WeWorkFinanceSdk.destroySdk(sdk); } catch (Throwable ignore) { }
            }
        }
    }

    /** 关联群名/发送人并落库 1 条（按 msg_id 去重）。 */
    private WorkWxMessage persistOne(long seq, String msgId, JSONObject msg, String roomId, Map<String, Object> result) {
        String fromUser = msg.getString("from");
        Long msgTimeMs = msg.getLong("msgtime");
        String content = msg.getJSONObject("text") != null ? msg.getJSONObject("text").getString("content") : null;

        // 关联群名 + 成员名（只读 work_wx_group_chat）
        WorkWxGroupChat group = workWxGroupChatManager.lambdaQuery()
                .eq(WorkWxGroupChat::getChatId, roomId)
                .last("limit 1")
                .one();
        String chatName = group != null ? group.getChatName() : null;
        String fromName = fromUser;
        int fromType = 0;
        boolean externalMatchedType2 = false;
        if (group != null && StringUtils.isNotBlank(group.getChatDetail())) {
            JSONArray members = parseMembers(group.getChatDetail());
            if (members != null) {
                for (int i = 0; i < members.size(); i++) {
                    JSONObject m = members.getJSONObject(i);
                    if (m != null && fromUser != null && fromUser.equals(m.getString("userid"))) {
                        if (StringUtils.isNotBlank(m.getString("name"))) {
                            fromName = m.getString("name");
                        }
                        fromType = m.getInteger("type") != null ? m.getInteger("type") : 0;
                        if (fromType == 2) externalMatchedType2 = true;
                        break;
                    }
                }
            }
        }

        // 关键验证日志：外部成员 from(external_userid，wm 开头) 是否命中 member_list type=2
        boolean looksExternal = fromUser != null && fromUser.startsWith("wm");
        if (looksExternal) {
            log.info("[msgaudit-poc] 外部 from={} 是否命中 member_list type=2: {}", fromUser, externalMatchedType2);
            result.put("externalFrom", fromUser);
            result.put("externalMatchedMemberType2", externalMatchedType2);
        }

        // 去重保存
        WorkWxMessage exist = workWxMessageManager.lambdaQuery()
                .eq(WorkWxMessage::getMsgId, msgId)
                .last("limit 1")
                .one();
        WorkWxMessage entity = exist != null ? exist : new WorkWxMessage();
        entity.setMsgId(msgId);
        entity.setSeq(seq);
        entity.setRoomId(roomId);
        entity.setChatName(chatName);
        entity.setFromUser(fromUser);
        entity.setFromName(fromName);
        entity.setFromType(fromType);
        entity.setMsgType("text");
        entity.setContent(content);
        entity.setMsgTime(msgTimeMs != null ? new Date(msgTimeMs) : null);
        entity.setTenantId(POC_TENANT_ID);
        workWxMessageManager.saveOrUpdate(entity);

        result.put("roomMatched", group != null);
        result.put("fromNameResolved", fromName);
        result.put("fromType", fromType);
        return entity;
    }

    /** chat_detail 既可能是 member_list 数组、也可能被包成 {member_list:[...]}，做容错解析。 */
    private JSONArray parseMembers(String chatDetail) {
        try {
            String s = chatDetail.trim();
            if (s.startsWith("[")) {
                return JSON.parseArray(s);
            }
            JSONObject obj = JSON.parseObject(s);
            return obj != null ? obj.getJSONArray("member_list") : null;
        } catch (Exception e) {
            log.warn("[msgaudit-poc] 解析 member_list 失败：{}", e.getMessage());
            return null;
        }
    }

    /** 用会话存档 RSA 私钥解 encrypt_random(base64) → AES key 字符串。RSA/ECB/PKCS1Padding。 */
    private String rsaDecrypt(String encryptRandomB64) throws Exception {
        String pem = WxConstants.WORK_WX_MSGAUDIT_PRIVATE_KEY
                .replaceAll("-----BEGIN [A-Z ]+-----", "")
                .replaceAll("-----END [A-Z ]+-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(pem);
        PrivateKey privateKey = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        byte[] decoded = cipher.doFinal(Base64.getDecoder().decode(encryptRandomB64));
        return new String(decoded, StandardCharsets.UTF_8);
    }

    private long currentLastSeq() {
        WorkWxMsgCursor cursor = workWxMsgCursorManager.lambdaQuery()
                .eq(WorkWxMsgCursor::getCursorKey, CURSOR_KEY)
                .last("limit 1")
                .one();
        return cursor != null && cursor.getLastSeq() != null ? cursor.getLastSeq() : 0L;
    }

    private void updateCursor(long seq) {
        WorkWxMsgCursor cursor = workWxMsgCursorManager.lambdaQuery()
                .eq(WorkWxMsgCursor::getCursorKey, CURSOR_KEY)
                .last("limit 1")
                .one();
        if (cursor == null) {
            cursor = new WorkWxMsgCursor();
            cursor.setCursorKey(CURSOR_KEY);
        }
        if (cursor.getLastSeq() == null || seq > cursor.getLastSeq()) {
            cursor.setLastSeq(seq);
        }
        cursor.setUpdateTime(new Date());
        workWxMsgCursorManager.saveOrUpdate(cursor);
    }

    private void safeFree(long slice) {
        try { WeWorkFinanceSdk.freeSlice(slice); } catch (Throwable ignore) { }
    }
}
