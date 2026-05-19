package com.recruit.airecruitsystem.interceptor;

import com.recruit.airecruitsystem.utils.JwtUtil;
import com.recruit.airecruitsystem.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    // 在请求处理前执行，返回true继续，false中断请求
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 预检请求（OPTIONS）直接放行，用于跨域
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        // 检查token是否存在且格式正确
        if (token == null || !token.startsWith("Bearer ")) {
            response.setStatus(401);   // 未授权
            return false;
        }

        token = token.substring(7);    // 去掉 "Bearer " 前缀
        try {
            // 检查token是否过期
            if (jwtUtil.isTokenExpired(token)) {
                response.setStatus(401);
                return false;
            }
            // 解析用户信息存入ThreadLocal，后续Controller可通过UserContext获取
            Integer userId = jwtUtil.getUserId(token);
            String role = jwtUtil.getRole(token);
            UserContext.setUserId(userId);
            UserContext.setRole(role);
            return true;
        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    // 请求结束后清理ThreadLocal
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserContext.clear();
    }
}