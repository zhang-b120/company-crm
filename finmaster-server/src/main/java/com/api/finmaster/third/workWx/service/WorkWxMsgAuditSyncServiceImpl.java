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
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 企业微信会话存档 · 阶段1：增量同步入库引擎（工单 75 / WXMSG-02）。
 *
 * <p>相对工单 73 PoC 的升级：① 定时增量、分页拉到空；② 全消息类型入库（raw_content 留底）；
 * ③ <b>游标推进到本批已成功处理的最大 seq</b>（修复 PoC 单条推进缺陷）；④ 任一条失败即停、不推进游标
 * （下轮从 last_seq 重拉，msg_id 唯一键去重兜底，保证不丢不重）；⑤ 媒体消息标记 has_media 交 WXMSG-03 下载。</p>
 *
 * <p>仍只读 {@code work_wx_group_chat} 取群名/成员，不碰任何业务模块。物料未就位时由定时任务探测后跳过。</p>
 *
 * <p>⚠️ 会话存档在企微侧仅约 5 天可拉取窗口，过期不可补拉——故定时任务 2 分钟一轮，远小于窗口。</p>
 */
@Slf4j
@Service
public class WorkWxMsgAuditSyncServiceImpl {

    private static final String CURSOR_KEY = "default";
    /** PoC/阶段1 临时租户ID（@PermitAll/定时任务无登录用户）。TODO：阶段后期接正式租户来源。 */
    private static final long POC_TENANT_ID = 1L;
    private static final long BATCH_LIMIT = 1000L;
    /** 媒体消息类型（交 WXMSG-03 分片下载） */
    private static final Set<String> MEDIA_TYPES = new HashSet<>(Arrays.asList("image", "voice", "video", "file"));
    /** 新建群按需补档负缓存 TTL：同群 30 分钟内至多触发 1 次 refreshOneGroup（成功/失败都退避）。 */
    private static final long GROUP_REFRESH_TTL_MS = 30 * 60 * 1000L;

    @Autowired
    private WorkWxGroupChatManager workWxGroupChatManager;
    @Autowired
    private WorkWxMessageManager workWxMessageManager;
    @Autowired
    private WorkWxMsgCursorManager workWxMsgCursorManager;
    /** 媒体下载服务（WXMSG-03）；required=false 保证未就位也能编译/运行 */
    @Autowired(required = false)
    private WorkWxMediaDownloadService mediaDownloadService;
    /** 新建群按需补群档案（工单 79 / WXGRP-01）。@Lazy 防潜在循环依赖。 */
    @Lazy
    @Autowired
    private WorkWxServiceImpl workWxService;
    /** 回复指标重算（工单 84 / WXRESP-10）：入库后异步触发 recomputeRecent。@Lazy 防潜在循环依赖。 */
    @Lazy
    @Autowired
    private WorkWxResponseMetricServiceImpl responseMetricService;

    /** 新建群补档负缓存：roomId → 上次尝试补档的时间戳(ms)。避免每条消息都打客户群 API。 */
    private final ConcurrentHashMap<String, Long> groupRefreshAttempts = new ConcurrentHashMap<>();
    /** 实例级同步锁（工单 83 / WXSYNC-02）：防回调与定时轮询、手动接口并发重入 syncIncremental。 */
    private final ReentrantLock syncLock = new ReentrantLock();
    /** 上次"真正执行"同步的时间戳（ms）（工单83/WXSYNC-01）：回调/轮询/手动任一次真同步都刷新，供 CALLBACK 安全兜底判断。 */
    private volatile long lastRunMs = 0L;

    /** 上次真正执行同步的时间戳（ms）；SKIPPED_RUNNING 不计。 */
    public long getLastRunMs() {
        return lastRunMs;
    }

    /** 物料是否就位（SDK + secret + RSA 私钥）。供定时任务探测。 */
    public boolean materialsReady() {
        return WeWorkFinanceSdk.isAvailable()
                && StringUtils.isNotBlank(WxConstants.WORK_WX_MSGAUDIT_SECRET)
                && StringUtils.isNotBlank(WxConstants.WORK_WX_MSGAUDIT_PRIVATE_KEY);
    }

    /**
     * 增量同步：从 last_seq 起分页拉取、解密、全类型入库，游标推进到本批成功处理的最大 seq。
     * @return 过程汇总
     */
    public Map<String, Object> syncIncremental() {
        // WXSYNC-02：tryLock 防并发重入（回调 runAsync / 定时轮询 / 手动接口共用本锁）
        if (!syncLock.tryLock()) {
            Map<String, Object> busy = new LinkedHashMap<>();
            busy.put("ok", true); // 跳过不是失败：正在跑的那次会 loop 到拉空、带走新消息
            busy.put("stage", "SKIPPED_RUNNING");
            busy.put("message", "已有同步正在执行，跳过本次触发（防回调与轮询并发重入）");
            return busy;
        }
        try {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!materialsReady()) {
            result.put("ok", false);
            result.put("stage", "PREREQ");
            result.put("message", "会话存档物料未就位（SDK/secret/私钥任一缺失），跳过");
            return result;
        }

        long sdk = 0L;
        long fromSeq = currentLastSeq();
        long lastGoodSeq = fromSeq;
        int saved = 0, skippedSingle = 0, mediaMarked = 0, pages = 0, skippedBadField = 0, skippedError = 0;
        boolean failed = false;
        String failMsg = null;
        // WXMSG-04：捕获 GetChatData 返回体的业务码，便于 pages=0 时判断真因（到期/无权限/secret不符）；
        // 原始报文走日志 [RAW] 行，不再入 result
        Integer lastErrcode = null;
        String lastErrmsg = null;
        Map<String, WorkWxGroupChat> groupCache = new HashMap<>();
        try {
            sdk = WeWorkFinanceSdk.newSdk();
            int initRet = WeWorkFinanceSdk.init(sdk, WxConstants.WORK_WX_CORP_ID, WxConstants.WORK_WX_MSGAUDIT_SECRET);
            if (initRet != 0) {
                result.put("ok", false);
                result.put("stage", "INIT");
                result.put("message", "Finance.Init 失败，返回码=" + initRet);
                return result;
            }

            outer:
            while (true) {
                long batchSlice = WeWorkFinanceSdk.newSlice();
                JSONArray chatData;
                try {
                    int ret = WeWorkFinanceSdk.getChatData(sdk, lastGoodSeq, BATCH_LIMIT, "", "", 10, batchSlice);
                    if (ret != 0) {
                        failed = true;
                        failMsg = "GetChatData 返回码=" + ret;
                        break;
                    }
                    String batchJson = WeWorkFinanceSdk.getContentFromSlice(batchSlice);
                    JSONObject batch = JSON.parseObject(batchJson);
                    // WXMSG-04：C 返回码 ret==0 不代表成功——会话存档到期/无权限/secret不符等会是
                    // {"errcode":xxxx,"errmsg":"..."} 且无 chatdata。先检查返回体业务码，避免错误被当"空"静默吞掉。
                    Integer errcode = batch == null ? null : batch.getInteger("errcode");
                    String errmsg = batch == null ? null : batch.getString("errmsg");
                    if (errcode != null && errcode != 0) {
                        failed = true;
                        // failMsg 精简，不内嵌 errmsg 全文（errmsg 已独立放入 result；原始报文见 [RAW] 行）
                        failMsg = "GetChatData 业务错误(errcode=" + errcode + ")";
                        lastErrcode = errcode;
                        lastErrmsg = errmsg;
                        // 原始报文单独成行 + [RAW] 专属前缀，便于 grep；企微 errmsg 自带逗号/URL 不再淹没它
                        log.warn("[msgaudit-sync][RAW] errcode={} 原始报文={}", errcode, truncate(batchJson, 500));
                        break;
                    }
                    chatData = batch == null ? null : batch.getJSONArray("chatdata");
                    if (chatData == null || chatData.isEmpty()) {
                        // 空结果：记录 errcode/errmsg，让 pages=0 也能看真因；原始报文单独成 [RAW] 行
                        lastErrcode = errcode;
                        lastErrmsg = errmsg;
                        log.info("[msgaudit-sync][RAW] 本批无数据 errcode={} 原始报文={}", errcode, truncate(batchJson, 500));
                    }
                } finally {
                    safeFree(batchSlice);
                }
                if (chatData == null || chatData.isEmpty()) {
                    break; // 拉到空（或上面已记录真因），结束
                }
                pages++;

                for (int i = 0; i < chatData.size(); i++) {
                    JSONObject enc = chatData.getJSONObject(i);
                    long seq = enc.getLongValue("seq");
                    String msgId = enc.getString("msgid");
                    try {
                        // WXMSG-05：官方 chatdata 字段名为 encrypt_random_key / encrypt_chat_msg
                        // （此前误用 encrypt_random / encrypt_chat_data 取到 null，真实消息一来即解密失败）。
                        // TODO(publickey_ver)：多公钥版本时应按 enc.getInteger("publickey_ver") 选对应私钥；当前单钥(版本1)不实现。
                        String encryptRandomKey = enc.getString("encrypt_random_key");
                        String encryptChatMsg = enc.getString("encrypt_chat_msg");
                        if (StringUtils.isBlank(encryptRandomKey) || StringUtils.isBlank(encryptChatMsg)) {
                            // 防御：字段缺失不抛 NPE、不中断整批；推进游标避免卡死。只打 msgid+字段名集合，不打密文值。
                            log.warn("[msgaudit-sync] 解密字段缺失，跳过 msgId={}, seq={}, encKeys={}", msgId, seq, enc.keySet());
                            lastGoodSeq = seq;
                            skippedBadField++;
                            continue;
                        }
                        String aesKey = rsaDecrypt(encryptRandomKey);
                        long msgSlice = WeWorkFinanceSdk.newSlice();
                        String plain;
                        try {
                            int decRet = WeWorkFinanceSdk.decryptData(sdk, aesKey, encryptChatMsg, msgSlice);
                            if (decRet != 0) {
                                throw new IllegalStateException("DecryptData 返回码=" + decRet);
                            }
                            plain = WeWorkFinanceSdk.getContentFromSlice(msgSlice);
                        } finally {
                            safeFree(msgSlice);
                        }

                        JSONObject msg = JSON.parseObject(plain);
                        String roomId = msg.getString("roomid");
                        if (StringUtils.isBlank(roomId)) {
                            // 单聊消息本期忽略，但 seq 视为已处理，推进游标避免卡住
                            lastGoodSeq = seq;
                            skippedSingle++;
                            continue;
                        }
                        WorkWxMessage entity = persistMessage(seq, msgId, msg, plain, roomId, groupCache);
                        if (entity.getHasMedia() != null && entity.getHasMedia() == 1) {
                            mediaMarked++;
                            tryDownloadMedia(entity, msg, sdk);
                        }
                        lastGoodSeq = seq; // 仅成功才推进
                        saved++;
                    } catch (Throwable e) {
                        // 单条处理失败（解析/解密/入库异常）：跳过该条并推进游标越过它，继续处理后续。
                        // 切勿 break/停在此条——否则一条"毒消息"会永久卡住整条同步流（5 天窗口内其后所有消息全部拉不到，
                        // 回调与轮询两种模式都同步不了）。宁可丢这一条，也不能堵死全部。
                        lastGoodSeq = seq;
                        skippedError++;
                        log.warn("[msgaudit-sync] seq={} msgId={} 处理失败，已跳过该条：{}", seq, msgId, e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            failed = true;
            failMsg = e.getMessage();
            log.error("[msgaudit-sync] 同步异常", e);
        } finally {
            if (sdk != 0L) {
                try { WeWorkFinanceSdk.destroySdk(sdk); } catch (Throwable ignore) { }
            }
        }

        // 推进游标到最后一条成功处理的 seq（失败也保留已成功部分，不丢不重）
        if (lastGoodSeq > fromSeq) {
            updateCursor(lastGoodSeq);
        }
        result.put("ok", !failed);
        result.put("fromSeq", fromSeq);
        result.put("lastSeq", lastGoodSeq);
        result.put("pages", pages);
        result.put("saved", saved);
        result.put("skippedSingle", skippedSingle);
        result.put("skippedBadField", skippedBadField);
        result.put("skippedError", skippedError);
        result.put("mediaMarked", mediaMarked);
        if (failed) result.put("failMsg", failMsg);
        // WXMSG-04：pages==0（一条都没拉到）时，把返回体业务码带回，手动端点 HTTP 响应直接可见真因；
        // 原始报文不再放 result（已由日志 [RAW] 行单独承载，避免 errcode/errmsg 三重重复）
        if (pages == 0) {
            result.put("errcode", lastErrcode);
            result.put("errmsg", lastErrmsg);
        }
        // WXRESP-10：本轮有新消息入库 → 异步触发最近 7 天回复指标重算（与 5 分钟定时兜底共用 recompute 锁、幂等）；
        // 异常只 warn、不阻塞本次同步返回。
        if (saved > 0) {
            CompletableFuture.runAsync(() -> {
                try {
                    responseMetricService.recomputeRecent();
                } catch (Throwable e) {
                    log.warn("[msgaudit-sync] 入库后触发回复指标重算失败：{}", e.getMessage());
                }
            });
        }
        // WXSYNC-01：本次真正执行了同步（非 SKIPPED_RUNNING）→ 刷新时间戳，供 CALLBACK 安全兜底判断；
        // 回调/轮询/手动任一路真同步都更新同一戳，兜底只在真"静默 N 分钟"时才触发。
        lastRunMs = System.currentTimeMillis();
        return result;
        } finally {
            syncLock.unlock();
        }
    }

    /** 截断字符串（≤maxLen），仅用于异常/空结果时打印 GetChatData 原始返回（密文层，不含解密明文）。 */
    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...(truncated," + s.length() + ")";
    }

    /**
     * 一次性回填客户名（工单 82 / WXRESP-03 修复）：把 work_wx_message 中 from_type=2 且 from_name=from_user
     * 的历史行，按其群 work_wx_group_chat.member_list 重映射 from_name；按 room_id 批处理，命中才更新、查不到保持原值。
     */
    public Map<String, Object> backfillCustomerNames() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<WorkWxMessage> targets = workWxMessageManager.lambdaQuery()
                .eq(WorkWxMessage::getFromType, 2)
                .isNotNull(WorkWxMessage::getRoomId).ne(WorkWxMessage::getRoomId, "")
                .apply("from_name = from_user") // 名字仍是 id（外部客户未解析出昵称）
                .list();
        int scanned = targets == null ? 0 : targets.size();
        int updated = 0, roomsWithDetail = 0;
        if (targets != null && !targets.isEmpty()) {
            Map<String, List<WorkWxMessage>> byRoom = new LinkedHashMap<>();
            for (WorkWxMessage m : targets) {
                byRoom.computeIfAbsent(m.getRoomId(), k -> new ArrayList<>()).add(m);
            }
            for (Map.Entry<String, List<WorkWxMessage>> e : byRoom.entrySet()) {
                WorkWxGroupChat group = workWxGroupChatManager.lambdaQuery()
                        .eq(WorkWxGroupChat::getChatId, e.getKey()).last("limit 1").one();
                if (group == null || StringUtils.isBlank(group.getChatDetail())) continue; // 群档无 member_list → 跳过该群
                roomsWithDetail++;
                for (WorkWxMessage m : e.getValue()) {
                    FromMatch fm = resolveFromInMembers(group, m.getFromUser());
                    if (fm != null && fm.name != null && !fm.name.equals(m.getFromUser())) {
                        workWxMessageManager.lambdaUpdate()
                                .eq(WorkWxMessage::getId, m.getId())
                                .set(WorkWxMessage::getFromName, fm.name)
                                .update();
                        updated++;
                    }
                }
            }
        }
        result.put("ok", true);
        result.put("scanned", scanned);
        result.put("roomsWithDetail", roomsWithDetail);
        result.put("updated", updated);
        return result;
    }

    /**
     * 历史乱码行直接回刷（工单85/WXMSG-06）：对库里已存的 work_wx_message.content 应用守卫式
     * {@link WeWorkFinanceSdk#fixGbkMojibake}——把 GBK 乱码还原为正确 UTF-8。
     * 不依赖企微 5 天可拉窗口、不重拉、与回调/轮询模式无关（纯 DB 内修正）；已正确的行被守卫跳过、不误伤。
     * <b>只回刷 content 单个文本字段，不动 raw_content（整段 JSON 不可整体 re-decode）。</b>
     * 按 id 分页处理，仅 content 有变化的行才更新。
     */
    public Map<String, Object> backfillMojibakeContent() {
        Map<String, Object> result = new LinkedHashMap<>();
        long lastId = 0L;
        int batch = 500, scanned = 0, updated = 0;
        while (true) {
            List<WorkWxMessage> rows = workWxMessageManager.lambdaQuery()
                    .gt(WorkWxMessage::getId, lastId)
                    .orderByAsc(WorkWxMessage::getId)
                    .last("limit " + batch)
                    .list();
            if (rows == null || rows.isEmpty()) break;
            for (WorkWxMessage m : rows) {
                lastId = m.getId();
                scanned++;
                String oldContent = m.getContent();
                String newContent = WeWorkFinanceSdk.fixGbkMojibake(oldContent);
                if (!Objects.equals(oldContent, newContent)) {
                    workWxMessageManager.lambdaUpdate()
                            .eq(WorkWxMessage::getId, m.getId())
                            .set(WorkWxMessage::getContent, newContent)
                            .update();
                    updated++;
                }
            }
            if (rows.size() < batch) break;
        }
        result.put("ok", true);
        result.put("scanned", scanned);
        result.put("updated", updated);
        return result;
    }

    /** 落库一条群消息（全类型，按 msg_id 去重 saveOrUpdate）。 */
    private WorkWxMessage persistMessage(long seq, String msgId, JSONObject msg, String plain,
                                         String roomId, Map<String, WorkWxGroupChat> groupCache) {
        String fromUser = msg.getString("from");
        String msgType = msg.getString("msgtype");
        Long msgTimeMs = msg.getLong("msgtime");

        // 群档查找（含新建群按需补档，工单 79 / WXGRP-01）：先本轮缓存，未命中查库；
        // 仍为 null 且负缓存允许 → 触发 refreshOneGroup 拉群名/成员后重查；补档须在解析群名/发送人之前。
        WorkWxGroupChat group;
        if (groupCache.containsKey(roomId)) {
            group = groupCache.get(roomId);
        } else {
            group = workWxGroupChatManager.lambdaQuery().eq(WorkWxGroupChat::getChatId, roomId).last("limit 1").one();
            if (group == null && shouldAttemptRefresh(roomId)) {
                groupRefreshAttempts.put(roomId, System.currentTimeMillis()); // 成功/失败都记，30 分钟退避
                try {
                    Boolean refreshed = workWxService.refreshOneGroup(roomId);
                    if (Boolean.TRUE.equals(refreshed)) {
                        group = workWxGroupChatManager.lambdaQuery()
                                .eq(WorkWxGroupChat::getChatId, roomId).last("limit 1").one();
                        // 未知→已知：回填该群历史 chat_name 为空的消息行
                        if (group != null) backfillChatName(roomId, group.getChatName());
                    }
                } catch (Throwable e) {
                    // 补档异常只 warn，不中断入库与游标推进
                    log.warn("[msgaudit-sync] 新建群按需补档失败 roomId={}：{}", roomId, e.getMessage());
                }
            }
            groupCache.put(roomId, group); // 无论是否 null 都缓存本轮，避免同轮重复查/重复补
        }
        String chatName = group != null ? group.getChatName() : null;
        int fromType = 0;
        String fromName = fromUser;
        FromMatch fm = resolveFromInMembers(group, fromUser);
        if (fm != null) {
            if (fm.name != null) fromName = fm.name;
            fromType = fm.type;
        }

        // WXGRP-02：老群进新成员——group 已知但 member_list 是旧的、不含该 from（解析不到 → fromName 仍是 wm… id）。
        // 非机器人(wb 开头)、from 非空、负缓存允许时，刷新 member_list 再解析一次。复用 WXGRP-01 同一 30min 负缓存
        // （同群 30 分钟至多刷 1 次，WXGRP-01 刚补过的不会重复刷）。best-effort：仍未命中则回退存 from(external_userid)，不反复打 API。
        if (group != null && fm == null && StringUtils.isNotBlank(fromUser)
                && !fromUser.startsWith("wb") && shouldAttemptRefresh(roomId)) {
            groupRefreshAttempts.put(roomId, System.currentTimeMillis()); // 成功/失败都记，退避
            try {
                Boolean refreshed = workWxService.refreshOneGroup(roomId);
                if (Boolean.TRUE.equals(refreshed)) {
                    WorkWxGroupChat fresh = workWxGroupChatManager.lambdaQuery()
                            .eq(WorkWxGroupChat::getChatId, roomId).last("limit 1").one();
                    if (fresh != null) {
                        group = fresh;
                        groupCache.put(roomId, fresh); // 更新本轮缓存为最新群档
                        if (StringUtils.isBlank(chatName)) {
                            chatName = fresh.getChatName();
                            backfillChatName(roomId, fresh.getChatName());
                        }
                        FromMatch fm2 = resolveFromInMembers(fresh, fromUser); // 用新 member_list 重解析
                        if (fm2 != null) {
                            fromName = fm2.name != null ? fm2.name : fromUser;
                            fromType = fm2.type;
                        }
                    }
                }
            } catch (Throwable e) {
                log.warn("[msgaudit-sync] 老群新成员按需刷 member_list 失败 roomId={}：{}", roomId, e.getMessage());
            }
        }

        String content = null;
        if ("text".equals(msgType) && msg.getJSONObject("text") != null) {
            // 工单85/WXMSG-06：JSON 解析后对单个文本值做守卫式 GBK→UTF-8 还原（不碰整段 JSON，避免破坏结构）
            content = WeWorkFinanceSdk.fixGbkMojibake(msg.getJSONObject("text").getString("content"));
        }
        int hasMedia = (msgType != null && MEDIA_TYPES.contains(msgType)) ? 1 : 0;
        String toList = msg.containsKey("tolist") ? JSON.toJSONString(msg.get("tolist")) : null;

        WorkWxMessage exist = workWxMessageManager.lambdaQuery()
                .eq(WorkWxMessage::getMsgId, msgId).last("limit 1").one();
        WorkWxMessage entity = exist != null ? exist : new WorkWxMessage();
        entity.setMsgId(msgId);
        entity.setSeq(seq);
        entity.setRoomId(roomId);
        entity.setChatName(chatName);
        entity.setFromUser(fromUser);
        entity.setFromName(fromName);
        entity.setFromType(fromType);
        entity.setMsgType(msgType);
        entity.setContent(content);
        entity.setRawContent(plain);
        entity.setToList(toList);
        entity.setHasMedia(hasMedia);
        entity.setMsgTime(msgTimeMs != null ? new Date(msgTimeMs) : null);
        entity.setTenantId(POC_TENANT_ID);
        workWxMessageManager.saveOrUpdate(entity);
        return entity;
    }

    /** 负缓存判定：该群从未尝试过补档、或上次尝试已超过 30 分钟 TTL，才允许再次触发 refreshOneGroup。 */
    private boolean shouldAttemptRefresh(String roomId) {
        Long last = groupRefreshAttempts.get(roomId);
        return last == null || (System.currentTimeMillis() - last) > GROUP_REFRESH_TTL_MS;
    }

    /** 补档成功后回填：把该 roomId 历史中 chat_name 为空（NULL/空串）的消息行刷成真实群名。 */
    private void backfillChatName(String roomId, String chatName) {
        if (StringUtils.isBlank(chatName)) return;
        try {
            workWxMessageManager.lambdaUpdate()
                    .eq(WorkWxMessage::getRoomId, roomId)
                    .and(w -> w.isNull(WorkWxMessage::getChatName).or().eq(WorkWxMessage::getChatName, ""))
                    .set(WorkWxMessage::getChatName, chatName)
                    .update();
        } catch (Throwable e) {
            log.warn("[msgaudit-sync] 回填历史 chat_name 失败 roomId={}：{}", roomId, e.getMessage());
        }
    }

    private void tryDownloadMedia(WorkWxMessage entity, JSONObject msg, long sdk) {
        if (mediaDownloadService == null) return; // WXMSG-03 未就位则跳过，不影响文本入库
        try {
            mediaDownloadService.downloadForMessage(entity, msg, sdk);
        } catch (Throwable e) {
            log.warn("[msgaudit-sync] 媒体下载失败 msgId={}：{}", entity.getMsgId(), e.getMessage());
        }
    }

    /** 群成员解析结果：命中成员的显示名（need_name=1：内部真名/外部昵称）+ 成员类型（1内部/2外部）。 */
    private static class FromMatch {
        final String name;
        final int type;
        FromMatch(String name, int type) { this.name = name; this.type = type; }
    }

    /**
     * 在群的 member_list 中按 from(userid) 匹配成员，命中返回 {name,type}，未命中/无群档返回 null。
     * 首解析与（WXGRP-02 刷新后的）重解析共用。
     */
    private FromMatch resolveFromInMembers(WorkWxGroupChat group, String fromUser) {
        if (group == null || fromUser == null || StringUtils.isBlank(group.getChatDetail())) return null;
        JSONArray members = parseMembers(group.getChatDetail());
        if (members == null) return null;
        for (int i = 0; i < members.size(); i++) {
            JSONObject m = members.getJSONObject(i);
            if (m != null && fromUser.equals(m.getString("userid"))) {
                String name = StringUtils.isNotBlank(m.getString("name")) ? m.getString("name") : null;
                int type = m.getInteger("type") != null ? m.getInteger("type") : 0;
                return new FromMatch(name, type);
            }
        }
        return null;
    }

    private JSONArray parseMembers(String chatDetail) {
        try {
            String s = chatDetail.trim();
            if (s.startsWith("[")) return JSON.parseArray(s);
            JSONObject obj = JSON.parseObject(s);
            return obj != null ? obj.getJSONArray("member_list") : null;
        } catch (Exception e) {
            return null;
        }
    }

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
                .eq(WorkWxMsgCursor::getCursorKey, CURSOR_KEY).last("limit 1").one();
        return cursor != null && cursor.getLastSeq() != null ? cursor.getLastSeq() : 0L;
    }

    private void updateCursor(long seq) {
        WorkWxMsgCursor cursor = workWxMsgCursorManager.lambdaQuery()
                .eq(WorkWxMsgCursor::getCursorKey, CURSOR_KEY).last("limit 1").one();
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
