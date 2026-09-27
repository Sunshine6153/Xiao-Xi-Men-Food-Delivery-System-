package com.neu.vo;

import java.io.Serializable;

public class AccountIdentityVO implements Serializable {
    private final Long id;
    private final String role;

    public AccountIdentityVO(Long id, String role) {
        this.id = id;
        this.role = role;
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }
}
