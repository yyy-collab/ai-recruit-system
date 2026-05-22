package com.recruit.airecruitsystem.dto.seeker;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SeekerUpdatePwdRequest {

    @NotBlank(message = "原密码不能为空")
    @JsonProperty("old_pwd")
    private String oldPwd;

    @NotBlank(message = "新密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$",
            message = "新密码必须为8~16位，且同时包含字母和数字")
    @JsonProperty("new_pwd")
    private String newPwd;

    @NotBlank(message = "确认密码不能为空")
    @JsonProperty("re_pwd")
    private String rePwd;
}