package com.neu.interceptor;

import com.neu.constant.AccountRole;
import com.neu.constant.JwtClaimsConstant;
import com.neu.exception.MerchantBusinessException;
import com.neu.exception.UserNotLoginException;
import com.neu.properties.JwtProperties;
import com.neu.entity.Merchant;
import com.neu.mapper.MerchantMapper;
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
public class JwtTokenAdminInterceptor implements HandlerInterceptor {
    //根据Jwt令牌拦截请求
    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private MerchantMapper merchantMapper;

    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) throws Exception {
        //非动态方法直接放行
        if(!(handler instanceof HandlerMethod)){
            return true;
        }

        String token = request.getHeader(jwtProperties.getAdminTokenName());

        try{
            log.info("开始校验管理端令牌");
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long merchantId = Long.valueOf(claims.get(JwtClaimsConstant.MERCHANT_ID).toString());
            String role = claims.get(JwtClaimsConstant.ROLE, String.class);
            if (!AccountRole.ADMIN.equals(role) && !AccountRole.MERCHANT.equals(role)) {
                throw new UserNotLoginException("登录凭证无效，请重新登录");
            }
            Merchant merchant = merchantMapper.getById(merchantId);
            if (merchant == null || !role.equals(merchant.getRole())) {
                throw new UserNotLoginException("账号身份已变化，请重新登录");
            }
            if (!Integer.valueOf(1).equals(merchant.getStatus())) {
                throw new UserNotLoginException("账号已被禁用，请联系管理员");
            }

            String path = request.getServletPath();
            if (!"/admin/merchant/logout".equals(path)
                    && !"/admin/auth/me".equals(path)
                    && !"/admin/websocket/ticket".equals(path)) {
                boolean categoryPath = "/admin/category".equals(path)
                        || path.startsWith("/admin/category/");
                boolean dishPath = "/admin/dish".equals(path)
                        || path.startsWith("/admin/dish/");
                boolean allowed;
                if (categoryPath) {
                    allowed = "GET".equals(request.getMethod()) || AccountRole.ADMIN.equals(role);
                } else if (path.equals("/admin/merchant/profile")
                        || path.startsWith("/admin/merchant/business-status/")) {
                    allowed = AccountRole.MERCHANT.equals(role);
                } else if (path.startsWith("/admin/merchant/")) {
                    allowed = AccountRole.ADMIN.equals(role);
                } else if (path.equals("/admin/user") || path.startsWith("/admin/user/")) {
                    allowed = AccountRole.ADMIN.equals(role);
                } else if (path.startsWith("/admin/report/")) {
                    allowed = AccountRole.ADMIN.equals(role);
                } else if (path.startsWith("/admin/platform/order/")) {
                    allowed = AccountRole.ADMIN.equals(role);
                } else if (path.startsWith("/admin/workspace/")) {
                    allowed = AccountRole.MERCHANT.equals(role);
                } else if (path.equals("/admin/order") || path.startsWith("/admin/order/")) {
                    allowed = AccountRole.MERCHANT.equals(role);
                } else if (dishPath) {
                    boolean queryOperation = "GET".equals(request.getMethod())
                            && ("/admin/dish/page".equals(path)
                            || path.matches("/admin/dish/\\d+"));
                    boolean statusOperation = "POST".equals(request.getMethod())
                            && path.startsWith("/admin/dish/status/");
                    allowed = AccountRole.MERCHANT.equals(role)
                            || (AccountRole.ADMIN.equals(role) && (queryOperation || statusOperation));
                } else {
                    allowed = false;
                }
                if (!allowed) {
                    throw new MerchantBusinessException(403, "没有权限访问该接口");
                }
            }
            request.setAttribute(JwtClaimsConstant.MERCHANT_ID, merchantId);
            request.setAttribute(JwtClaimsConstant.ROLE, role);
            log.info("当前账号ID：{}，角色：{}", merchantId, role);
            return true;
        } catch (ExpiredJwtException e) {
            throw new UserNotLoginException("登录状态已过期，请重新登录");
        } catch (JwtException | IllegalArgumentException e) {
            throw new UserNotLoginException("登录凭证无效，请重新登录");
        }
    }
}
