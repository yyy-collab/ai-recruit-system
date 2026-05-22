package com.recruit.airecruitsystem.dto.seeker;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SeekerDeleteRequest {

    @NotBlank(message = "密码不能为空")
    private String password;
}