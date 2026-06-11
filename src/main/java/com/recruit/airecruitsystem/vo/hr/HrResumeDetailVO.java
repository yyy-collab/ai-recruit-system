package com.recruit.airecruitsystem.vo.hr;

import com.recruit.airecruitsystem.model.ResumeAnalysisSnapshot;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HrResumeDetailVO {
    private Integer deliveryId;
    private Integer jobId;
    private String jobName;
    private CandidateProfile seekerInfo;
    private ResumeInfo resumeInfo;
    private MatchInfo matchInfo;
    private Integer status;
    private LocalDateTime deliveryTime;
    private LocalDateTime updateTime;

    @Data
    public static class CandidateProfile {
        private Integer id;
        private String realName;
        private String targetTitle;
        private String avatarUrl;
        private String phone;
        private String email;
        private Integer age;
        private String eduBack;
        private String almaMater;
        private String city;
    }

    @Data
    public static class ResumeInfo {
        private Integer resumeId;
        private String resumeFileUrl;
        private String resumeFileName;
        private String workExperience;
        private List<String> skills;
        private List<ResumeAnalysisSnapshot.WorkHistoryItem> workHistory;
        private String previewText;
    }

    @Data
    public static class MatchInfo {
        private Integer matchScore;
        private String matchLevel;
        private String aiComment;
        private List<String> coreAdvantages;
        private List<String> potentialRisks;
        private List<String> skillTags;
    }
}
