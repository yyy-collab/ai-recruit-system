package com.recruit.airecruitsystem.vo.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenRefreshVO {
    private String token;
    private Integer expiresIn;   // 秒
}

