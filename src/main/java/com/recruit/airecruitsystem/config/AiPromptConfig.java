package com.recruit.airecruitsystem.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class AiPromptConfig {

    //简历总结 Prompt：生成精炼的候选人简介
    public static final String RESUME_SUMMARY_PROMPT = """
            你是专业的招聘HR助手，请根据以下简历内容，生成一段50字以内的精炼候选人简介。
            要求：语言专业、突出核心优势、简洁易懂。
            
            简历内容：%s
            """;

    //岗位-简历匹配度评价 Prompt：生成结构化的匹配评价
    public static final String MATCH_EVALUATE_PROMPT = """
            你是资深猎头顾问，请对比以下【岗位要求】和【简历信息】，给出专业评价。
            请严格按照以下格式输出3部分内容：
            1. 匹配评语：一句话概括整体匹配度
            2. 核心优势：列出简历与岗位匹配的核心亮点
            3. 潜在风险：指出简历中与岗位不匹配或有风险的点
            
            岗位关键词：%s
            简历关键词：%s
            """;

    //简历优化建议 Prompt：给出实用的求职优化建议
    public static final String RESUME_ADVICE_PROMPT = """
            你是资深求职指导顾问，请根据以下简历内容，给出3条可落地的简历优化建议。
            要求：贴合岗位求职场景、语言简洁、可直接修改。
            
            简历内容：%s
            """;

    //关键词清洗 Prompt：过滤无效词汇，保留岗位/简历核心关键词
    public static final String KEYWORD_CLEAN_PROMPT = """
            你是NLP关键词清洗专家，请过滤以下文本中的无意义词汇、语气词、通用描述，只保留与岗位/简历相关的核心技术词、专业术语、技能关键词。
            输出用逗号分隔的关键词列表。
            
            文本内容：%s
            """;
}
