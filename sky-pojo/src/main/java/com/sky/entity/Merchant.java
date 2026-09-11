package com.sky.entity;

import com.baomidou.mybatisplus.annotation.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("merchant")
public class Merchant implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String merchantName;
    private String contactName;
    private String phone;
    private String location;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public Integer getStatus() {
        return status;
    }

    public String getPhone() {
        return phone;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String username;
        private String password;
        private String merchantName;
        private String phone;
        private Integer status;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder merchantName(String merchantName) {
            this.merchantName = merchantName;
            return this;
        }

        public Builder phone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder status(Integer status) {
            this.status = status;
            return this;
        }

        public Merchant build() {
            Merchant merchant = new Merchant();
            merchant.username = username;
            merchant.password = password;
            merchant.merchantName = merchantName;
            merchant.phone = phone;
            merchant.status = status;
            return merchant;
        }
    }
}
