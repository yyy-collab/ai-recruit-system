package com.recruit.airecruitsystem.pojo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resume")
public class Resume {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private JobSeekerUser user;

    @Column(length = 100)
    private String resumeFileName;

    @Column(nullable = false, length = 255)
    private String resumeFileUrl;

    @Column(columnDefinition = "TINYINT(1) DEFAULT 0")
    private Boolean isParsed = false;

    private Integer totalScore;
    private Integer keywordCoverage;
    private Integer semanticComplete;
    private Integer competitiveness;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createTime;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updateTime;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public JobSeekerUser getUser() { return user; }
    public void setUser(JobSeekerUser user) { this.user = user; }

    public String getResumeFileName() { return resumeFileName; }
    public void setResumeFileName(String resumeFileName) { this.resumeFileName = resumeFileName; }

    public String getResumeFileUrl() { return resumeFileUrl; }
    public void setResumeFileUrl(String resumeFileUrl) { this.resumeFileUrl = resumeFileUrl; }

    public Boolean getIsParsed() { return isParsed; }
    public void setIsParsed(Boolean isParsed) { this.isParsed = isParsed; }

    public Integer getTotalScore() { return totalScore; }
    public void setTotalScore(Integer totalScore) { this.totalScore = totalScore; }

    public Integer getKeywordCoverage() { return keywordCoverage; }
    public void setKeywordCoverage(Integer keywordCoverage) { this.keywordCoverage = keywordCoverage; }

    public Integer getSemanticComplete() { return semanticComplete; }
    public void setSemanticComplete(Integer semanticComplete) { this.semanticComplete = semanticComplete; }

    public Integer getCompetitiveness() { return competitiveness; }
    public void setCompetitiveness(Integer competitiveness) { this.competitiveness = competitiveness; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}