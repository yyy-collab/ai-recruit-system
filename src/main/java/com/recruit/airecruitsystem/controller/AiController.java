package com.recruit.airecruitsystem.controller;

import com.recruit.airecruitsystem.mapper.JobMapper;
import com.recruit.airecruitsystem.mapper.ResumeMapper;
import com.recruit.airecruitsystem.pojo.Job;
import com.recruit.airecruitsystem.pojo.Resume;
import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.service.ai.HanLPService;
import com.recruit.airecruitsystem.service.ai.KeywordExtractService;
import com.recruit.airecruitsystem.service.ai.MatchCalculateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private HanLPService hanLPService;

    @Autowired
    private KeywordExtractService keywordExtractService;

    // 已有的分词接口
    @PostMapping("/word/segment")
    public Result segment(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        if (text == null || text.trim().isEmpty()) {
            return Result.error(10001, "text不能为空");
        }
        List<String> words = hanLPService.segment(text, true);
        Map<String, Object> data = new HashMap<>();
        data.put("words", words);
        return Result.success(data);
    }

    //关键词提取接口
    @PostMapping("/keyword/extract")
    public Result extractKeyword(@RequestBody Map<String, Object> request) {
        String type = (String) request.get("type"); // "text", "resume", "job"
        Integer resumeId = (Integer) request.get("resume_id");
        Integer jobId = (Integer) request.get("job_id");
        Integer limit = request.get("limit") == null ? 10 : (Integer) request.get("limit");
        if (limit > 20) limit = 20;

        String content = null;

        if ("text".equals(type)) {
            content = (String) request.get("content");
        } else if ("resume".equals(type)) {
            // 根据 resumeId 从数据库查询简历解析后的文本（假设你有 ResumeMapper）
            // 这里简化，先返回空
            return Result.error(10006, "简历文本获取未实现，请先完善");
        } else if ("job".equals(type)) {
            // 根据 jobId 查询岗位描述文本
            return Result.error(10006, "岗位文本获取未实现，请先完善");
        } else {
            return Result.error(10001, "type参数错误");
        }

        if (content == null || content.trim().isEmpty()) {
            return Result.error(10001, "文本内容为空");
        }

        List<String> keywords = keywordExtractService.extractKeywords(content, limit);
        Map<String, Object> data = new HashMap<>();
        data.put("keywords", keywords);
        return Result.success(data);
    }

    //匹配度计算接口
    @Autowired
    private JobMapper jobMapper;
    @Autowired
    private ResumeMapper resumeMapper;
    @Autowired
    private MatchCalculateService matchCalculateService;

    @PostMapping("/match/calculate")
    public Result calculateMatch(@RequestBody Map<String, Integer> request) {
        Integer jobId = request.get("job_id");
        Integer resumeId = request.get("resume_id");
        if (jobId == null || resumeId == null) {
            return Result.error(10001, "job_id 和 resume_id 不能为空");
        }

        // 1. 查询岗位信息
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            return Result.error(10006, "岗位不存在");
        }

        // 2. 查询简历信息
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            return Result.error(10006, "简历不存在");
        }

        // 3. 检查简历是否解析完成
        if (resume.getIsParsed() == null || resume.getIsParsed() != 1) {
            return Result.error(10016, "简历正在解析中或解析失败，请稍后再试");
        }

        // 4. 拼接待匹配的文本
        // 岗位文本：岗位名称 + 岗位描述 + 任职要求 + 关键词
        String jobText = (job.getJobName() != null ? job.getJobName() : "")
                + " " + (job.getJobDesc() != null ? job.getJobDesc() : "")
                + " " + (job.getRequirement() != null ? job.getRequirement() : "")
                + " " + (job.getKeywords() != null ? job.getKeywords() : "");

        // 简历文本：使用 AI 解析出的纯文本（如果 parsed_text 为空，可以拼接 skills + work_experience 等）
        String resumeText = resume.getParsedText();
        if (resumeText == null || resumeText.trim().isEmpty()) {
            return Result.error(10017, "简历文本为空，请重新上传并等待解析");
        }

        // 5. 调用匹配服务计算分数
        double score = matchCalculateService.calculateMatch(jobText, resumeText);
        String level = matchCalculateService.getMatchLevel(score);

        Map<String, Object> data = new HashMap<>();
        data.put("match_score", score);
        data.put("match_level", level);
        data.put("algorithm", "HanLP + TF-IDF + 余弦相似度");
        return Result.success(data);
    }
}