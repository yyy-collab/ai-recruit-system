package com.recruit.airecruitsystem.dto.job;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SeekerJobListItemResponse {
    private Integer id;
    private String jobName;
    private String jobDesc;
    private String salary;
    private String workAddress;
    private String workExperience;
    private String keywords;
    private String companyName;
    private Integer status;
    private Boolean isDelivered;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    private BigDecimal matchScore;
}
