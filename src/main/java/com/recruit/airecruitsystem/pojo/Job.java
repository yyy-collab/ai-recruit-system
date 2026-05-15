package com.recruit.airecruitsystem.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "job")
@Schema(description = "Job entity")
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "Job ID", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    private Integer id;

    @Column(nullable = false, length = 100)
    @Schema(description = "Job name", example = "Java Backend Engineer")
    private String jobName;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Job description", example = "Responsible for backend API development and maintenance")
    private String jobDesc;

    @Column(columnDefinition = "TEXT")
    @Schema(description = "Job requirement", example = "Familiar with Spring Boot, MySQL and RESTful API design")
    private String requirement;

    @Column(nullable = false, columnDefinition = "TEXT")
    @Schema(description = "Keywords, separated by commas", example = "Java,Spring Boot,MySQL,Swagger")
    private String keywords;

    @Column(length = 50)
    @Schema(description = "Salary range", example = "15k-25k")
    private String salary;

    @Column(columnDefinition = "INT DEFAULT 1")
    @Schema(description = "Job status, 1 active and 0 inactive", example = "1")
    private Integer status = 1;

    @Column(columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP")
    @Schema(description = "Create time", example = "2026-05-11T13:00:00", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime createTime;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getJobName() { return jobName; }
    public void setJobName(String jobName) { this.jobName = jobName; }

    public String getJobDesc() { return jobDesc; }
    public void setJobDesc(String jobDesc) { this.jobDesc = jobDesc; }

    public String getRequirement() { return requirement; }
    public void setRequirement(String requirement) { this.requirement = requirement; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getSalary() { return salary; }
    public void setSalary(String salary) { this.salary = salary; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
