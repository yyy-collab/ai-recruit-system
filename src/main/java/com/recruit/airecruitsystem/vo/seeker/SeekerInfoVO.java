package com.recruit.airecruitsystem.vo.seeker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeekerInfoVO {
    private Integer id;
    private String username;
    private String realName;
    private String avatarUrl;
    private String phone;
    private String email;
    private Integer age;
    private String address;
    private String eduBack;
    private String almaMater;
    private String state;
    private String exPosition;
    private String exCity;
    private Integer exSalaryMin;
    private Integer exSalaryMax;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
