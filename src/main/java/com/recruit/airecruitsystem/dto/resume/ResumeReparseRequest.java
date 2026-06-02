package com.recruit.airecruitsystem.dto.resume;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResumeReparseRequest {
    @NotNull(message = "resumeId 不能为空")
    private Integer resumeId;
}
