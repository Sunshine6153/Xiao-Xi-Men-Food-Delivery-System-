package com.neu.config;

import com.neu.properties.WebSocketProperties;
import com.neu.websocket.WebSocketHandshakeInterceptor;
import com.neu.websocket.WebSocketServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfiguration implements WebSocketConfigurer {

    private final WebSocketServer webSocketServer;
    private final WebSocketHandshakeInterceptor handshakeInterceptor;
    private final WebSocketProperties webSocketProperties;

    public WebSocketConfiguration(WebSocketServer webSocketServer,
                                  WebSocketHandshakeInterceptor handshakeInterceptor,
                                  WebSocketProperties webSocketProperties) {
        this.webSocketServer = webSocketServer;
        this.handshakeInterceptor = handshakeInterceptor;
        this.webSocketProperties = webSocketProperties;
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketServer, "/ws")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOriginPatterns(webSocketProperties.getAllowedOriginPatterns().toArray(String[]::new));
    }
}
