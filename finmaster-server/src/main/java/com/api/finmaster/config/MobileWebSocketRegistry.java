package com.qykj.finmaster.config;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 移动端 WebSocket 会话注册表
 * 维护 userId → WebSocketSession 的映射，用于向指定用户推送拨号指令
 */
@Slf4j
@Component
public class MobileWebSocketRegistry {

    /** userId → session */
    private final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        sessions.put(userId, session);
        log.info("[MobileWS] 用户 {} 已连接，sessionId={}", userId, session.getId());
    }

    public void remove(Long userId) {
        sessions.remove(userId);
        log.info("[MobileWS] 用户 {} 已断开", userId);
    }

    /**
     * 向指定用户发送 JSON 消息
     *
     * @param userId  目标用户ID
     * @param payload 消息对象（将被序列化为 JSON）
     * @return 是否发送成功
     */
    public boolean sendToUser(Long userId, Object payload) {
        WebSocketSession session = sessions.get(userId);
        if (session == null || !session.isOpen()) {
            log.warn("[MobileWS] 用户 {} 不在线，无法推送消息", userId);
            return false;
        }
        try {
            String text = JSONUtil.toJsonStr(payload);
            session.sendMessage(new TextMessage(text));
            return true;
        } catch (Exception e) {
            log.error("[MobileWS] 向用户 {} 发送消息失败", userId, e);
            return false;
        }
    }

    public boolean isOnline(Long userId) {
        WebSocketSession session = sessions.get(userId);
        return session != null && session.isOpen();
    }
}
