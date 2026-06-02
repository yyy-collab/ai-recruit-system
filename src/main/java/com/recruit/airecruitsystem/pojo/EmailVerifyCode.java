package com.recruit.airecruitsystem.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 邮箱验证码实体类，对应表 email_verify_code
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerifyCode {
    private Integer id;
    private String email;           // 接收验证码的邮箱（用户的邮箱）
    private String code;            // 6位数字验证码
    private String type;            // 验证码类型：seeker_reset 或 hr_reset
    private LocalDateTime expireTime;   // 过期时间
    private LocalDateTime createTime;
}