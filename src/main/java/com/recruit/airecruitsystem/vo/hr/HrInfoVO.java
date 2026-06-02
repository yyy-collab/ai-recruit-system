package com.recruit.airecruitsystem.vo.hr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrInfoVO {
    private Integer id;
    private String username;
    private String realName;
    private String avatarUrl;
    private String companyName;
    private String email;
    private String phone;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}