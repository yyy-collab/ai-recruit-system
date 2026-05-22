package com.recruit.airecruitsystem.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenBlacklist {
    private Integer id;
    private String token;        // JWT 令牌（存储完整字符串）
    private LocalDateTime expireTime;  // 令牌过期时间，用于自动清理
    private LocalDateTime createTime;
}