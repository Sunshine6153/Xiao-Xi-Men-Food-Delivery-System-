package com.neu.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "sky.websocket")
public class WebSocketProperties {

    private long ticketTtl = 30000;
    private List<String> allowedOriginPatterns = new ArrayList<>();
}
