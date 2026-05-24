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
public class Hr {
    private Integer id;
    private String username;
    private String password;
    private String realName;
    private String avatarUrl;
    private String companyName;
    private String email;
    private String phone;
    private Integer refreshCount;
    private LocalDateTime lastRefreshTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}