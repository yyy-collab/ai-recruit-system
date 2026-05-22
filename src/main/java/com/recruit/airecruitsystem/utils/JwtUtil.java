package com.recruit.airecruitsystem.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 * 用于生成、解析、验证 JSON Web Token
 * 支持自定义载荷（userId, role 等）
 */
@Component
public class JwtUtil {

    // 从配置文件读取签名密钥（必须足够长，至少32字符）
    @Value("${jwt.secret}")
    private String secret;

    // Token 有效期（秒），从配置文件读取，例如 7200（2小时）
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 根据配置的密钥字符串生成 HMAC-SHA256 签名密钥
     * 要求 secret 长度至少为 32 字节，否则会抛出异常
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token（通用方法）
     * @param userId 用户ID（求职者ID或HR ID）
     * @param role 用户角色（"seeker" 或 "hr"）
     * @return JWT 字符串
     */
    public String generateToken(Integer userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration * 1000);

        return Jwts.builder()
                .setClaims(claims)              // 自定义载荷
                .setIssuedAt(now)               // 签发时间
                .setExpiration(expireDate)      // 过期时间
                .signWith(getSigningKey())      // 签名算法（默认 HS256）
                .compact();                     // 生成紧凑字符串
    }

    /**
     * 解析 JWT Token，获取 Claims 对象
     * @param token JWT 字符串
     * @return Claims 载荷
     */
    public Claims parseToken(String token) {

        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 Token 中提取用户 ID
     * @param token JWT 字符串
     * @return 用户ID（Integer）
     */
    public Integer getUserId(String token) {
        return parseToken(token).get("userId", Integer.class);
    }

    /**
     * 从 Token 中提取用户角色
     * @param token JWT 字符串
     * @return 角色（"seeker" 或 "hr"）
     */
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    /**
     * 判断 Token 是否已过期
     * @param token JWT 字符串
     * @return true-已过期，false-未过期
     */
    public boolean isTokenExpired(String token) {
        Date expirationDate = parseToken(token).getExpiration();
        return expirationDate.before(new Date());
    }

    /**
     * 验证 Token 是否有效（未过期且格式正确）
     * @param token JWT 字符串
     * @return true-有效，false-无效
     */
    public boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取 Token 的有效期（秒），用于返回给前端
     * @return 有效期（秒）
     */
    public Long getExpiration() {
        return expiration;
    }

    /**
     * 刷新 Token：生成一个新的 Token（内容与原 Token 相同，但重新计算过期时间）
     * 注意：此方法仅用于刷新接口，调用前需确保原 Token 未过期且在允许刷新窗口内。
     * @param oldToken 原 JWT Token（用于提取用户ID和角色）
     * @return 新的 JWT 字符串
     */
    public String refreshToken(String oldToken) {
        Integer userId = getUserId(oldToken);
        String role = getRole(oldToken);
        return generateToken(userId, role);
    }

    /**
     * 从 HttpServletRequest 的 Authorization 头中解析出用户 ID
     * @param request HTTP 请求对象
     * @return 用户 ID
     * @throws RuntimeException 如果 token 无效或过期
     */
    public Integer getUserIdFromRequest(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new RuntimeException("无效的 Authorization 头");
        }
        String token = authorization.substring(7);
        return getUserId(token);
    }

}