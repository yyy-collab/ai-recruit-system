package com.recruit.airecruitsystem.pojo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume_ai_analysis")
public class ResumeAIAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne
    @JoinColumn(name = "job_post_id", nullable = false)
    private Job job;

    private Integer matchScore;

    @Column(length = 20)
    private String matchLevel;

    @Column(columnDefinition = "TEXT")
    private String aiComment;

    @Column(columnDefinition = "TEXT")
    private String coreAdvantages;

    @Column(columnDefinition = "TEXT")
    private String potentialRisks;

    @Column(length = 255)
    private String skillTags;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime analysisTime;

    @Column(length = 20, columnDefinition = "VARCHAR(20) DEFAULT '待初筛'")
    private String status = "待初筛";

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createTime;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updateTime;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }

    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }

    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }

    public String getMatchLevel() { return matchLevel; }
    public void setMatchLevel(String matchLevel) { this.matchLevel = matchLevel; }

    public String getAiComment() { return aiComment; }
    public void setAiComment(String aiComment) { this.aiComment = aiComment; }

    public String getCoreAdvantages() { return coreAdvantages; }
    public void setCoreAdvantages(String coreAdvantages) { this.coreAdvantages = coreAdvantages; }

    public String getPotentialRisks() { return potentialRisks; }
    public void setPotentialRisks(String potentialRisks) { this.potentialRisks = potentialRisks; }

    public String getSkillTags() { return skillTags; }
    public void setSkillTags(String skillTags) { this.skillTags = skillTags; }

    public LocalDateTime getAnalysisTime() { return analysisTime; }
    public void setAnalysisTime(LocalDateTime analysisTime) { this.analysisTime = analysisTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}