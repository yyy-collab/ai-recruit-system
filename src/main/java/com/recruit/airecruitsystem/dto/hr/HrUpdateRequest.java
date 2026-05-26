package com.recruit.airecruitsystem.dto.hr;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.hibernate.validator.constraints.URL;

@Data
public class HrUpdateRequest {

    @Size(min = 2, max = 20, message = "真实姓名长度2~20字符")
    @JsonProperty("real_name")
    private String realName;

    @URL(message = "头像URL格式不正确")
    @JsonProperty("avatar_url")
    private String avatarUrl;

    @JsonProperty("company_name")
    private String companyName;

    @Email(message = "邮箱格式不正确")
    @JsonProperty("email")
    private String email;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @JsonProperty("phone")
    private String phone;
}