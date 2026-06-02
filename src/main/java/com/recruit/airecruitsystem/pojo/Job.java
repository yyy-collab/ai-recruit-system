package com.recruit.airecruitsystem.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 岗位实体类，对应数据库表 job
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {
    private Integer id;                 // 岗位ID
    private Integer hrId;               // 发布HR的ID
    private String jobName;             // 岗位名称
    private String jobDesc;             // 岗位描述
    private String requirement;         // 任职要求
    private String keywords;            // 核心关键词，逗号分隔
    private String salary;              // 薪资范围，如15-25K
    private String workAddress;         // 工作地点
    private String workExperience;      // 工作经验要求，如3-5年
    private Integer status;             // 1-上线，0-下线
    private Integer deliveryCount;      // 投递数量（冗余统计）
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}