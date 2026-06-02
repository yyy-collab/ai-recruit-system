package com.recruit.airecruitsystem.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeParseResult {
    private Integer id;
    private Integer resumeId;
    private Integer keywordCoverage;
    private String basicInfo;
    private String workExperience;
    private String skills;
    private String workHistory;
    private String aiSummary;
    private String improvementSuggestions;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
