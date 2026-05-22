package com.recruit.airecruitsystem.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 求职者实体类，映射数据库表 seeker
 * 仅用于持久化，校验请使用对应的 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seeker {
    private Integer id;
    private String username;
    private String password;         // 加密后存储
    private String realName;
    private String avatarUrl;
    private String phone;
    private String email;
    private Integer age;
    private String address;
    private String eduBack;          // 学历
    private String almaMater;        // 毕业院校
    private String state;            // 求职状态
    private String exPosition;       // 期望职位
    private String exCity;           // 期望城市
    private Integer exSalaryMin;     // 期望最低薪资(K)
    private Integer exSalaryMax;     // 期望最高薪资(K)
    private Integer refreshCount;    // 当日刷新次数
    private LocalDateTime lastRefreshTime; // 上次刷新时间
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}