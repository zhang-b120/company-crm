package com.qykj.finmaster.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 移动端原生 WebSocket 配置
 * 注册 /ws/mobile 端点，与 STOMP 端点 /ws 并存
 */
@Configuration
@EnableWebSocket
public class MobileWebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MobileWebSocketHandler mobileWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mobileWebSocketHandler, "/ws/mobile")
                .setAllowedOriginPatterns("*");
    }
}
