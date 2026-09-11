package com.sky.vo;

import java.io.Serializable;

//返回给前端的商户登录数据
public class MerchantLoginVO implements Serializable {

    private Long id;
    private String name;
    private String token;

    public MerchantLoginVO() {
    }

    private MerchantLoginVO(Long id, String name, String token) {
        this.id = id;
        this.name = name;
        this.token = token;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getToken() {
        return token;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String token;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public MerchantLoginVO build() {
            return new MerchantLoginVO(id, name, token);
        }
    }
}
