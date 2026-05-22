package com.recruit.airecruitsystem.dto.seeker;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.*;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SeekerUpdateRequest {

    @Size(min = 2, max = 20, message = "真实姓名长度必须为2~20字符")
    private String realName;      // 自动映射 JSON 中的 real_name

    @URL(message = "头像URL格式不正确")
    private String avatarUrl;     // 映射 avatar_url

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Min(16) @Max(65)
    private Integer age;

    private String address;

    @Pattern(regexp = "^(专科|本科|硕士|博士)$", message = "学历只能是：专科、本科、硕士、博士")
    private String eduBack;       // 映射 edu_back

    private String almaMater;     // 映射 alma_mater

    @Pattern(regexp = "^(在职|离职|应届毕业生)$", message = "求职状态只能是：在职、离职、应届毕业生")
    private String state;

    private String exPosition;    // 映射 ex_position

    private String exCity;        // 映射 ex_city

    @Min(0)
    private Integer exSalaryMin;   // 映射 ex_salary_min

    @Min(0)
    private Integer exSalaryMax;   // 映射 ex_salary_max
}