package com.recruit.airecruitsystem.dto.seeker;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HandleInterviewRequest {
    @NotNull(message = "消息ID不能为空")
    @JsonProperty("message_id")
    private Integer messageId;

    @NotNull(message = "状态不能为空")
    private Integer status;   // 1-接受，2-拒绝

    @JsonProperty("reject_reason")
    private String rejectReason;
}
