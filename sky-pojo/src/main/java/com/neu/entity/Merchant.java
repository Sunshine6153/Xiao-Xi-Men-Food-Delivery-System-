package com.neu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("merchant")
public class Merchant implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String merchantName;
    private String phone;
    private String location;
    private String role;
    // 账号状态：1-启用，0-禁用。禁用后不能登录。
    private Integer status;
    // 营业状态：1-营业，0-暂停营业。暂停营业不影响商户登录。
    private Integer businessStatus;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String username;
        private String password;
        private String merchantName;
        private String phone;
        private String location;
        private String role;
        private Integer status;
        private Integer businessStatus;
        private LocalDateTime createTime;
        private LocalDateTime updateTime;

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

        public Builder location(String location) {
            this.location = location;
            return this;
        }
        public Builder role(String role) {
            this.role = role;
            return this;
        }
        public Builder status(Integer status) {
            this.status = status;
            return this;
        }
        public Builder businessStatus(Integer businessStatus) {
            this.businessStatus = businessStatus;
            return this;
        }

        public Builder createTime(LocalDateTime date) {
            this.createTime = date;
            return this;
        }
        public Builder updateTime(LocalDateTime date) {
            this.updateTime = date;
            return this;
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }
        public Merchant build() {
            Merchant merchant = new Merchant();
            merchant.id = id;
            merchant.username = username;
            merchant.password = password;
            merchant.merchantName = merchantName;
            merchant.phone = phone;
            merchant.location = location;
            merchant.role = role;
            merchant.status = status;
            merchant.businessStatus = businessStatus;
            merchant.createTime = createTime;
            merchant.updateTime = updateTime;
            return merchant;
        }
    }
}
