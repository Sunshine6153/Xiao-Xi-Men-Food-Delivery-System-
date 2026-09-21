package com.neu.interceptor;

import com.neu.constant.JwtClaimsConstant;
import com.neu.context.BaseContext;
import com.neu.exception.UserNotLoginException;
import com.neu.properties.JwtProperties;
import com.neu.utils.JwtUtil;
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
public class JwtTokenUserInterceptor implements HandlerInterceptor {
    //根据Jwt令牌拦截请求
    @Autowired
    private JwtProperties jwtProperties;

    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        //非动态方法直接放行
        if(!(handler instanceof HandlerMethod)){
            return true;
        }

        String token = request.getHeader(jwtProperties.getUserTokenName());

        try{
            log.info("开始校验令牌：{}", token);
            Claims claims = JwtUtil.parseJWT(jwtProperties.getUserSecretKey(), token);
            Long userId = Long.valueOf(claims.get(JwtClaimsConstant.USER_ID).toString());
            request.setAttribute(JwtClaimsConstant.USER_ID, userId);
            BaseContext.setCurrentId(userId);
            log.info("登陆用户ID为：{}", userId);
            return true;
        } catch (ExpiredJwtException e) {
            throw new UserNotLoginException("登录状态已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new UserNotLoginException("登录凭证无效，请重新登录");
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        BaseContext.remove();
    }
}
