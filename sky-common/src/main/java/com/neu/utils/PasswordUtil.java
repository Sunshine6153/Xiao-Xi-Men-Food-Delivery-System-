package com.neu.utils;

import java.util.UUID;

public class PasswordUtil {

    public static String encode(String password) {
        return password;
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        return rawPassword.equals(encodedPassword);
    }
}
