package com.recruit.airecruitsystem.dto.hr;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class SendInterviewRequest {
    @NotNull(message = "投递ID不能为空")
    @JsonProperty("delivery_id")
    private Integer deliveryId;

    @NotNull(message = "面试日期不能为空")
    @JsonProperty("interview_date")
    private LocalDate interviewDate;

    @NotNull(message = "面试时间不能为空")
    @JsonProperty("interview_time")
    private LocalTime interviewTime;

    @NotBlank(message = "面试形式不能为空")
    @JsonProperty("interview_type")
    private String interviewType;      // 线上面试 / 现场面试 / 电话面试

    @NotBlank(message = "面试轮次不能为空")
    @JsonProperty("interview_round")
    private String interviewRound;     // 初试 / 复试 / 终试

    @NotBlank(message = "面试地点不能为空")
    @JsonProperty("interview_address")
    private String interviewAddress;

    @NotBlank(message = "联系人不能为空")
    @JsonProperty("contact_name")
    private String contactName;

    @NotBlank(message = "联系人电话不能为空")
    @JsonProperty("contact_phone")
    private String contactPhone;

    private String remark;
}