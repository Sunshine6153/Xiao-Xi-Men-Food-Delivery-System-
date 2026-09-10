package com.sky.utils;

import jakarta.servlet.http.HttpServletRequest;

public class JwtUtil {

    public static String getToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        return token;
    }

    public static Long getUserIdFromToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            String[] parts = token.split("\\.");
            return Long.valueOf(parts[0]);
        } catch (Exception e) {
            return null;
        }
    }
}
