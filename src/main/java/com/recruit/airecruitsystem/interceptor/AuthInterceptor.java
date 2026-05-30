package com.recruit.airecruitsystem.interceptor;

import com.recruit.airecruitsystem.mapper.TokenBlacklistMapper;
import com.recruit.airecruitsystem.utils.JwtUtil;
import com.recruit.airecruitsystem.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistMapper tokenBlacklistMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String uri = request.getRequestURI();
        if (uri.equals("/seeker/register") || uri.equals("/seeker/login") ||
                uri.equals("/hr/register") || uri.equals("/hr/login") ||
                uri.startsWith("/common/")) {
            return true;
        }

        // 预检请求（OPTIONS）直接放行，用于跨域
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 设置响应编码为 UTF-8，避免中文乱码
        response.setContentType("application/json;charset=UTF-8");

        // 1. 获取 Authorization 头
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"msg\":\"未提供有效的认证令牌\"}");
            return false;
        }

        // 2. 提取纯 token（去掉 "Bearer " 前缀）
        String token = authHeader.substring(7);

        // 3. 黑名单检查
        if (tokenBlacklistMapper.isBlacklisted(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":10005,\"msg\":\"令牌已登出，请重新登录\"}");
            return false;
        }

        // 4. 检查 token 是否过期
        if (jwtUtil.isTokenExpired(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":10005,\"msg\":\"令牌已过期\"}");
            return false;
        }

        // 5. 解析用户信息存入 ThreadLocal
        try {
            Integer userId = jwtUtil.getUserId(token);
            String role = jwtUtil.getRole(token);
            String requestUri = request.getRequestURI();
            if (requestUri.startsWith("/hr") && !isRole(role, "hr")) {
                writeJsonError(response, 403, "无权访问");
                return false;
            }
            if (requestUri.startsWith("/seeker") && !isRole(role, "seeker")) {
                writeJsonError(response, 403, "无权访问");
                return false;
            }
            UserContext.setUserId(userId);
            UserContext.setRole(role);
            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":10005,\"msg\":\"无效令牌\"}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 请求结束后清理 ThreadLocal，防止内存泄漏
        UserContext.clear();
    }

    private boolean isRole(String actualRole, String expectedRole) {
        return actualRole != null && expectedRole.equalsIgnoreCase(actualRole.trim());
    }

    private void writeJsonError(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format("{\"code\":%d,\"msg\":\"%s\",\"data\":null}", status, message));
    }
}
