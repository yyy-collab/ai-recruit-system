package com.recruit.airecruitsystem.dto.job;

import lombok.Data;

@Data
public class JobStatusUpdateRequest {
    private Integer id;
    private Integer status;
}
