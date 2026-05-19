package com.recruit.airecruitsystem.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component   // 注册为Spring Bean，可被其他组件自动注入
public class JwtUtil {

    @Value("${jwt.secret}")    // 从配置文件读取jwt.secret
    private String secret;

    @Value("${jwt.expiration}") // 读取有效期
    private Long expiration;

    // 根据密钥字符串生成HMAC-SHA256签名密钥
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // 生成JWT token，存入用户id和角色
    public String generateToken(Integer userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        Date now = new Date();
        Date expireDate = new Date(now.getTime() + expiration * 1000);
        return Jwts.builder()
                .setClaims(claims)          // 自定义载荷
                .setIssuedAt(now)           // 签发时间
                .setExpiration(expireDate)  // 过期时间
                .signWith(getSigningKey(), SignatureAlgorithm.HS256) // 签名算法
                .compact();                 // 生成紧凑字符串token
    }

    // 解析token，获取载荷内容
    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 从token中提取用户id
    public Integer getUserId(String token) {
        return parseToken(token).get("userId", Integer.class);
    }

    // 提取角色
    public String getRole(String token) {
        return parseToken(token).get("role", String.class);
    }

    // 判断token是否已过期
    public boolean isTokenExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }
}