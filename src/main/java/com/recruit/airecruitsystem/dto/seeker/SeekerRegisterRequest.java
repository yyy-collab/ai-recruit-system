package com.recruit.airecruitsystem.dto.seeker;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;

/**
 * 求职者注册请求参数DTO
 * 仅包含接口需要的 username 和 password
 */

@Data
public class SeekerRegisterRequest {

    /**
     * 用户名
     * 规则：5~16位字母、数字、下划线，全局唯一
     */
    @NotBlank(message = "用户名不能为空")
    @Size(min = 5, max = 16, message = "用户名长度必须为5~16位")
    @Pattern(regexp = "^[a-zA-Z0-9_]{5,16}$", message = "用户名只能包含字母、数字、下划线")
    private String username;

    /**
     * 密码（加密存储）
     * 规则：8~16位字符，必须包含字母和数字
     * 注意：@JsonProperty(access = WRITE_ONLY) 使得该字段只写入（接收请求参数），
     *       不会出现在响应 JSON 中，防止密码泄露。
     */
    @Getter(onMethod_ = @JsonProperty(access = JsonProperty.Access.WRITE_ONLY))
    @NotBlank(message = "密码不能为空")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{8,16}$",
            message = "密码必须为8~16位，且同时包含字母和数字")
    private String password;



}
