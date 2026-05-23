package com.recruit.airecruitsystem.dto.hr;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class HrRegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{5,16}$", message = "用户名必须为5~16位字母数字下划线")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$",
            message = "密码必须为8~16位且包含字母和数字")
    private String password;
}