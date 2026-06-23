package com.recruit.airecruitsystem.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 简历解析结构化快照模型
 * 用于封装Word文档解析后提取的完整结构化简历信息
 * 后端序列化JSON存入resume_parse_result表，前端质量评分、雷达图接口统一读取该模型数据
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeAnalysisSnapshot {
    private BasicInfo basicInfo;
    private String workExperience;
    private List<String> skills;
    private List<WorkHistoryItem> workHistory;

    /**
     * 简历基础信息内部静态类
     * 存储姓名、联系方式、学历、年龄等核心求职必填字段
     */
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

    /**
     * 单条工作经历明细内部静态类
     * 存储单段任职公司、岗位、时间、工作描述、项目核心技能
     */
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