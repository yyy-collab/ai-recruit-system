package com.recruit.airecruitsystem.vo.seeker;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeekerInfoVO {
    @JsonProperty("id")
    private Integer id;

    @JsonProperty("username")
    private String username;

    @JsonProperty("real_name")
    private String realName;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("email")
    private String email;

    @JsonProperty("age")
    private Integer age;

    @JsonProperty("address")
    private String address;

    @JsonProperty("edu_back")
    private String eduBack;

    @JsonProperty("alma_mater")
    private String almaMater;

    @JsonProperty("state")
    private String state;

    @JsonProperty("ex_position")
    private String exPosition;

    @JsonProperty("ex_city")
    private String exCity;

    @JsonProperty("ex_salary_min")
    private Integer exSalaryMin;

    @JsonProperty("ex_salary_max")
    private Integer exSalaryMax;

    @JsonProperty("create_time")
    private LocalDateTime createTime;

    @JsonProperty("update_time")
    private LocalDateTime updateTime;
}