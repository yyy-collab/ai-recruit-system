package com.recruit.airecruitsystem.dto.hr;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HrDeleteRequest {
    @NotBlank(message = "密码不能为空")
    @JsonProperty("password")
    private String password;
}