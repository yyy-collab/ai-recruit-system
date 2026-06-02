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
    private Integer keywordCoverage;
    private BasicInfo basicInfo;
    private String workExperience;
    private List<String> skills;
    private List<WorkHistoryItem> workHistory;
    private String aiSummary;
    private String improvementSuggestions;
    private QualityReport qualityReport;
    private List<SuggestionCard> suggestionCards;
    private List<RadarMetric> radarMetrics;
    private MatchInsight matchInsight;

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QualityReport {
        private Integer totalScore;
        private Integer keywordRichness;
        private Integer structureCompleteness;
        private Integer benchmarkScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SuggestionCard {
        private String title;
        private String detail;
        private String emphasis;
        private String actionLabel;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RadarMetric {
        private String label;
        private Integer userScore;
        private Integer benchmarkScore;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MatchInsight {
        private Integer score;
        private String level;
        private String headline;
        private List<String> strengths;
        private List<String> concerns;
        private List<String> tags;
    }
}
