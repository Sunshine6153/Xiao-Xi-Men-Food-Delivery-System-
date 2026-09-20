package com.neu.vo;

import java.io.Serializable;

public class UserLoginVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String openid;
    private String token;

    private UserLoginVO(Long id, String openid, String token) {
        this.id = id;
        this.openid = openid;
        this.token = token;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public String getOpenid() { return openid; }
    public String getToken() { return token; }

    public static class Builder {
        private Long id;
        private String openid;
        private String token;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder openid(String openid) { this.openid = openid; return this; }
        public Builder token(String token) { this.token = token; return this; }
        public UserLoginVO build() { return new UserLoginVO(id, openid, token); }
    }
}
