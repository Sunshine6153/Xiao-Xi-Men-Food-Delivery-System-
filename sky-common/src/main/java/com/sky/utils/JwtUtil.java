package com.sky.utils;
//JWT token 的相关工具类
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

public class JwtUtil {

    public static String getToken(HttpServletRequest request) {
        return request.getHeader("token");
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

    /**
     * Generate JWT token
     * @param secretKey Secret key for signing
     * @param ttl Time to live in milliseconds
     * @param claims JWT claims
     * @return JWT token
     */
    public static String createJWT(String secretKey, long ttl, Map<String, Object> claims) {
        long expiration = System.currentTimeMillis() + ttl;
        SecretKeySpec key = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");

        String token = Jwts.builder()
                .claims(claims)
                .expiration(new Date(expiration))
                .signWith(key)
                .compact();

        return token;
    }

    /**
     * Parse JWT token
     * @param secretKey Secret key for verification
     * @param token JWT token
     * @return JWT claims
     */
    public static Claims parseJWT(String secretKey, String token) {
        Claims claims = Jwts.parser()
                .verifyWith(new javax.crypto.spec.SecretKeySpec(
                        secretKey.getBytes(StandardCharsets.UTF_8), 
                        0, 
                        secretKey.getBytes(StandardCharsets.UTF_8).length,
                        "HmacSHA256"))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims;
    }
}
