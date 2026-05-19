package com.recruit.airecruitsystem.service;

import java.util.List;
import java.util.Map;

public interface AiService {
    // 简历分词
    List<String> wordSegment(String text);
    // 关键词提取
    List<String> extractKeyword(String text);
    // 岗位-简历匹配度计算
    Map<String, Object> calculateMatch(Long jobId, Long resumeId);
    // 简历总结（AI Prompt）
    String generateResumeSummary(String resumeText);
    // 简历优化建议（AI Prompt）
    String generateResumeAdvice(String resumeText);
}
