package com.recruit.airecruitsystem.pojo;

import java.util.Date;

public class ResumeParseResult {
    private Integer id;
    private Integer resumeId;
    private Integer keywordCoverage;
    private String basicInfo;
    private String workExperience;
    private String skills;
    private String workHistory;
    private String aiSummary;
    private String improvementSug;
    private Date createTime;
    private Date updateTime;

    // 生成所有 getter 和 setter（必须）
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getResumeId() { return resumeId; }
    public void setResumeId(Integer resumeId) { this.resumeId = resumeId; }

    public Integer getKeywordCoverage() { return keywordCoverage; }
    public void setKeywordCoverage(Integer keywordCoverage) { this.keywordCoverage = keywordCoverage; }

    public String getBasicInfo() { return basicInfo; }
    public void setBasicInfo(String basicInfo) { this.basicInfo = basicInfo; }

    public String getWorkExperience() { return workExperience; }
    public void setWorkExperience(String workExperience) { this.workExperience = workExperience; }

    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }

    public String getWorkHistory() { return workHistory; }
    public void setWorkHistory(String workHistory) { this.workHistory = workHistory; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getImprovementSug() { return improvementSug; }
    public void setImprovementSug(String improvementSug) { this.improvementSug = improvementSug; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Date getUpdateTime() { return updateTime; }
    public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
}