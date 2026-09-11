package com.sky.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
//记录JWT相关配置
@Component
@ConfigurationProperties(prefix = "sky.jwt")
public class JwtProperties {

    private String adminSecretKey;
    private long adminTtl;
    private String userSecretKey;
    private long userTtl;

    public String getAdminSecretKey() {
        return adminSecretKey;
    }

    public long getAdminTtl() {
        return adminTtl;
    }

    public String getUserSecretKey() {
        return userSecretKey;
    }

    public long getUserTtl() {
        return userTtl;
    }

    public void setAdminSecretKey(String adminSecretKey) {
        this.adminSecretKey = adminSecretKey;
    }

    public void setAdminTtl(long adminTtl) {
        this.adminTtl = adminTtl;
    }

    public void setUserSecretKey(String userSecretKey) {
        this.userSecretKey = userSecretKey;
    }

    public void setUserTtl(long userTtl) {
        this.userTtl = userTtl;
    }
}
