package com.neu.websocket;

import com.neu.properties.WebSocketProperties;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketTicketService {

    private final WebSocketProperties webSocketProperties;
    private final ConcurrentHashMap<String, Ticket> tickets = new ConcurrentHashMap<>();

    public WebSocketTicketService(WebSocketProperties webSocketProperties) {
        this.webSocketProperties = webSocketProperties;
    }

    public String issue(String clientKey) {
        removeExpiredTickets();
        String ticket = UUID.randomUUID().toString().replace("-", "");
        long expiresAt = Instant.now().toEpochMilli() + webSocketProperties.getTicketTtl();
        tickets.put(ticket, new Ticket(clientKey, expiresAt));
        return ticket;
    }

    public String consume(String ticketValue) {
        if (ticketValue == null || ticketValue.isBlank()) {
            return null;
        }
        Ticket ticket = tickets.remove(ticketValue);
        if (ticket == null || ticket.expiresAt() < Instant.now().toEpochMilli()) {
            return null;
        }
        return ticket.clientKey();
    }

    private void removeExpiredTickets() {
        long now = Instant.now().toEpochMilli();
        tickets.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
    }

    private record Ticket(String clientKey, long expiresAt) {
    }
}
