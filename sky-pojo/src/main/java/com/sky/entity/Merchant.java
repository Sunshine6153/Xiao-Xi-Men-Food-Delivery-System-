package com.sky.entity;

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
    //不需要在DTO中显示
    private Integer status;
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
        private Integer status;
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
        public Builder status(Integer status) {
            this.status = status;
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
            merchant.status = status;
            merchant.createTime = createTime;
            merchant.updateTime = updateTime;
            return merchant;
        }
    }
}
