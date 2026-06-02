package com.recruit.airecruitsystem.dto.seeker;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 求职者登录请求参数
 */

@Data
public class SeekerLoginRequest {

    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{5,16}$", message = "用户名格式错误")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;   //密码格式校验在业务层做，因为密码需要加密处理，此处仅限制非空
}
