package com.recruit.airecruitsystem.dto.seeker;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class SeekerUpdateRequest {

    @JsonProperty("real_name")
    @Size(min = 2, max = 20, message = "真实姓名长度必须为2~20字符")
    private String realName;

    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("phone")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @JsonProperty("email")
    @Email(message = "邮箱格式不正确")
    private String email;

    @JsonProperty("age")
    @Min(16) @Max(65)
    private Integer age;

    @JsonProperty("address")
    private String address;

    @JsonProperty("edu_back")
    @Pattern(regexp = "^(专科|本科|硕士|博士)$", message = "学历只能是：专科、本科、硕士、博士")
    private String eduBack;

    @JsonProperty("alma_mater")
    private String almaMater;

    @JsonProperty("state")
    @Pattern(regexp = "^(在职|离职|应届毕业生)$", message = "求职状态只能是：在职、离职、应届毕业生")
    private String state;

    @JsonProperty("ex_position")
    private String exPosition;

    @JsonProperty("ex_city")
    private String exCity;

    @JsonProperty("ex_salary_min")
    @Min(0)
    private Integer exSalaryMin;

    @JsonProperty("ex_salary_max")
    @Min(0)
    private Integer exSalaryMax;
}