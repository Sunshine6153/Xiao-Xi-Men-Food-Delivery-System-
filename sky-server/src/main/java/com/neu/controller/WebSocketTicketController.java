package com.neu.controller;

import com.neu.constant.JwtClaimsConstant;
import com.neu.result.Result;
import com.neu.websocket.WebSocketTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "WebSocket连接")
@RestController
@RequestMapping({"/admin/websocket", "/user/websocket"})
@SecurityRequirement(name = "token")
public class WebSocketTicketController {

    private final WebSocketTicketService ticketService;

    public WebSocketTicketController(WebSocketTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Operation(summary = "获取WebSocket一次性连接票据")
    @PostMapping("/ticket")
    public Result<String> ticket(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(JwtClaimsConstant.USER_ID);
        if (userId != null) {
            return Result.success(ticketService.issue(clientKey("USER", userId)));
        }

        Long accountId = (Long) request.getAttribute(JwtClaimsConstant.MERCHANT_ID);
        String role = (String) request.getAttribute(JwtClaimsConstant.ROLE);
        return Result.success(ticketService.issue(clientKey(role, accountId)));
    }

    private String clientKey(String clientType, Long clientId) {
        return clientType + ":" + clientId;
    }
}
