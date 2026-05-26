package com.recruit.airecruitsystem.dto.job;

import lombok.Data;

@Data
public class JobUpdateRequest {
    private Integer id;
    private String jobName;
    private String jobDesc;
    private String requirement;
    private String keywords;
    private String salary;
    private String workAddress;
    private String workExperience;
}
