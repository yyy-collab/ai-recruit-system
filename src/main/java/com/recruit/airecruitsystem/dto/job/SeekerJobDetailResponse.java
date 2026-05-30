package com.recruit.airecruitsystem.dto.job;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeekerJobDetailResponse {
    private Integer id;
    private String jobName;
    private String jobDesc;
    private String requirement;
    private String keywords;
    private String salary;
    private String companyName;
    private String workAddress;
    private String workExperience;
    private String hrName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
