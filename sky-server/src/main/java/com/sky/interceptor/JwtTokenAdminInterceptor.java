package com.sky.interceptor;

import com.sky.constant.JwtClaimsConstant;
import com.sky.exception.UserNotLoginException;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Component
@Slf4j
public class JwtTokenAdminInterceptor implements HandlerInterceptor {
    //根据Jwt令牌拦截请求
    @Autowired
    private JwtProperties jwtProperties;

    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        //非动态方法直接放行
        if(!(handler instanceof HandlerMethod)){
            return true;
        }

        String token = request.getHeader(jwtProperties.getAdminTokenName());

        try{
            log.info("开始校验令牌：{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long merchantId = Long.valueOf(claims.get(JwtClaimsConstant.MERCHANT_ID).toString());
            log.info("登陆商户ID为：{}", merchantId);
            return true;
        } catch (ExpiredJwtException e) {
            throw new UserNotLoginException("登录状态已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new UserNotLoginException("登录凭证无效，请重新登录");
        }
    }
}
