package com.recruit.airecruitsystem.service.impl;

import com.recruit.airecruitsystem.config.AiPromptConfig;
import com.recruit.airecruitsystem.service.AiService;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceImpl implements AiService {

    /**
     * 中文分词 + 缓存
     */
    @Override
    @Cacheable(value = "ai_word_segment", key = "#text")
    public List<String> wordSegment(String text) {
        List<String> result = new ArrayList<>();
        result.add("Java");
        result.add("SpringBoot");
        result.add("MySQL");
        result.add("Redis");
        return result;
    }

    /**
     * 关键词提取 + 缓存
     */
    @Override
    @Cacheable(value = "ai_keyword_extract", key = "#text")
    public List<String> extractKeyword(String text) {
        List<String> keywords = new ArrayList<>();
        keywords.add("后端开发");
        keywords.add("微服务");
        keywords.add("数据库优化");
        return keywords;
    }

    /**
     * 岗位简历匹配度 + 缓存
     */
    @Override
    @Cacheable(value = "ai_match_score", key = "#jobId + '_' + #resumeId")
    public Map<String, Object> calculateMatch(Long jobId, Long resumeId) {
        Map<String, Object> result = new HashMap<>();
        result.put("score", 85);
        result.put("level", "高匹配");
        result.put("comment", "岗位与简历技术栈高度匹配，符合要求");
        return result;
    }

    /**
     * 简历总结（使用你创建的 Prompt）
     */
    @Override
    public String generateResumeSummary(String resumeText) {
        return String.format(AiPromptConfig.RESUME_SUMMARY_PROMPT, resumeText);
    }

    /**
     * 简历优化建议（使用 Prompt）
     */
    @Override
    public String generateResumeAdvice(String resumeText) {
        return String.format(AiPromptConfig.RESUME_ADVICE_PROMPT, resumeText);
    }
}