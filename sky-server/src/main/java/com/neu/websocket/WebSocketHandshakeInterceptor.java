package com.neu.websocket;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;
import com.neu.mapper.UserMapper;
import com.neu.mapper.MerchantMapper;
import com.neu.entity.Merchant;

import java.util.Map;

@Component
public class WebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String CLIENT_KEY = "websocketClientKey";

    private final WebSocketTicketService ticketService;
    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;

    public WebSocketHandshakeInterceptor(WebSocketTicketService ticketService,
                                         UserMapper userMapper,
                                         MerchantMapper merchantMapper) {
        this.ticketService = ticketService;
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        MultiValueMap<String, String> parameters = UriComponentsBuilder
                .fromUri(request.getURI())
                .build()
                .getQueryParams();
        String clientKey = ticketService.consume(parameters.getFirst("ticket"));
        if (clientKey == null || !isAccountEnabled(clientKey)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(CLIENT_KEY, clientKey);
        return true;
    }

    private boolean isAccountEnabled(String clientKey) {
        String[] parts = clientKey.split(":", 2);
        if (parts.length != 2) return false;
        try {
            Long id = Long.valueOf(parts[1]);
            if ("USER".equals(parts[0])) {
                return Integer.valueOf(1).equals(userMapper.getStatus(id));
            }
            Merchant merchant = merchantMapper.getById(id);
            return merchant != null && parts[0].equals(merchant.getRole())
                    && Integer.valueOf(1).equals(merchant.getStatus());
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
    }
}
