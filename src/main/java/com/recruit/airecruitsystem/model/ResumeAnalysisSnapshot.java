package com.recruit.airecruitsystem.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAnalysisSnapshot {
    private BasicInfo basicInfo;
    private String workExperience;
    private List<String> skills;
    private List<WorkHistoryItem> workHistory;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BasicInfo {
        private String realName;
        private String phone;
        private String email;
        private Integer age;
        private String eduBack;
        private String almaMater;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class WorkHistoryItem {
        private String company;
        private String position;
        private String startTime;
        private String endTime;
        private String description;
        private List<String> coreSkills;
    }
}
