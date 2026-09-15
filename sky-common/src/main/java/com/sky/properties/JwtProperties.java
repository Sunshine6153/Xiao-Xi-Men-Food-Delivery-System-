package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
//记录JWT相关配置
@Data
@Component
@ConfigurationProperties(prefix = "sky.jwt")
public class JwtProperties {

    private String adminSecretKey;
    private long adminTtl;
    private String userSecretKey;
    private long userTtl;
    private String adminTokenName = "token";
    private String userTokenName = "token";
}
