package com.qykj.finmaster.config;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.qykj.finmaster.security.JwtTokenProvider;
import com.qykj.finmaster.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 移动端原生 WebSocket 处理器（注册于 /ws/mobile）
 *
 * 连接认证：通过 URL 参数 ?token=xxx 传入 JWT，服务端解析并校验。
 *
 * 消息格式（JSON）：
 *   客户端 → 服务端: {"type":"CALL_STATUS","payload":{...}}
 *   服务端 → 客户端: {"type":"CALL","payload":{"phoneNumber":"xxx","callRecordId":1,"leadId":1,"name":"xxx"}}
 */
@Slf4j
@Component
public class MobileWebSocketHandler extends TextWebSocketHandler {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private MobileWebSocketRegistry registry;

    /** session 属性 key：userId */
    public static final String ATTR_USER_ID = "userId";

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = resolveUserId(session);
        if (userId == null) {
            log.warn("[MobileWS] Token 无效，拒绝连接 sessionId={}", session.getId());
            try { session.close(CloseStatus.NOT_ACCEPTABLE); } catch (Exception ignored) {}
            return;
        }
        session.getAttributes().put(ATTR_USER_ID, userId);
        registry.register(userId, session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            JSONObject json = JSONUtil.parseObj(message.getPayload());
            String type = json.getStr("type");
            if ("PING".equals(type)) {
                session.sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
            }
            // 其他消息类型（CALL_STATUS 等）由具体业务扩展处理
        } catch (Exception e) {
            log.error("[MobileWS] 处理消息异常", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        if (userId != null) {
            registry.remove(userId);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[MobileWS] 传输错误 sessionId={}", session.getId(), exception);
        Long userId = (Long) session.getAttributes().get(ATTR_USER_ID);
        if (userId != null) {
            registry.remove(userId);
        }
    }

    /** 从 URL 查询参数中解析 token，返回 userId；失败返回 null */
    private Long resolveUserId(WebSocketSession session) {
        try {
            String query = session.getUri() != null ? session.getUri().getQuery() : null;
            if (!StringUtils.hasText(query)) return null;

            String token = null;
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    token = param.substring(6);
                    break;
                }
            }
            if (!StringUtils.hasText(token) || !jwtTokenProvider.validateToken(token)) {
                return null;
            }
            String username = jwtTokenProvider.getUsernameFromJWT(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (userDetails instanceof LoginUser) {
                return ((LoginUser) userDetails).getUserId();
            }
        } catch (Exception e) {
            log.warn("[MobileWS] Token 解析失败", e);
        }
        return null;
    }
}
