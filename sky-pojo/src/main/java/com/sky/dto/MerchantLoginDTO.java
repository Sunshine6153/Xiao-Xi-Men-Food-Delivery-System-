package com.sky.dto;

import java.io.Serializable;
import lombok.Data;

@Data
//接受前端传输的商户登录数据
public class MerchantLoginDTO implements Serializable {

    private String username;
    private String password;

    public MerchantLoginDTO() {
    }

    public MerchantLoginDTO(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String password;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public MerchantLoginDTO build() {
            return new MerchantLoginDTO(username, password);
        }
    }
}
