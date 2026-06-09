package com.recruit.airecruitsystem.controller.ai;

import com.recruit.airecruitsystem.mapper.*;
import com.recruit.airecruitsystem.pojo.*;
import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.service.ai.HanLPService;
import com.recruit.airecruitsystem.service.ai.KeywordExtractService;
import com.recruit.airecruitsystem.service.ai.MatchCalculateService;
import jakarta.transaction.Transactional;
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

    @Autowired
    private ResumeParseResultMapper resumeParseResultMapper;

    @Autowired
    private DeliveryMapper deliveryMapper;
    @Autowired
    private AiMatchResultMapper aiMatchResultMapper;
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
            if (resumeId == null) {
                return Result.error(10001, "resume_id 不能为空");
            }
            // 查询简历解析结果
            ResumeParseResult parseResult = resumeParseResultMapper.selectByResumeId(resumeId);
            if (parseResult == null) {
                return Result.error(10017, "简历解析结果不存在，请重新上传");
            }
            // 拼接文本（与 calculateMatch 中的逻辑一致）
            StringBuilder sb = new StringBuilder();
            if (parseResult.getSkills() != null) {
                String skills = parseResult.getSkills().replaceAll("[\\[\\]\"]", "").replace(",", " ");
                sb.append(skills).append(" ");
            }
            if (parseResult.getWorkExperience() != null) {
                sb.append(parseResult.getWorkExperience()).append(" ");
            }
            if (parseResult.getWorkHistory() != null) {
                String history = parseResult.getWorkHistory().replaceAll("[\\[\\]{}\"]", "").replace(",", " ");
                sb.append(history);
            }
            content = sb.toString().trim();
            if (content.isEmpty()) {
                return Result.error(10017, "简历文本为空，请重新上传并等待解析");
            }
        } else if ("job".equals(type)) {
            if (jobId == null) {
                return Result.error(10001, "job_id 不" +
                        "能为空");
            }
            // 查询岗位
            Job job = jobMapper.selectById(jobId);
            if (job == null) {
                return Result.error(10007, "岗位不存在");
            }
            // 拼接文本
            content = job.getJobName() + " " + job.getJobDesc() + " " + job.getRequirement();
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

        // 查询岗位信息
        Job job = jobMapper.selectById(jobId);
        if (job == null) {
            return Result.error(10006, "岗位不存在");
        }

        // 查询简历信息
        Resume resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            return Result.error(10006, "简历不存在");
        }

        //检查简历是否解析完成
        if (resume.getIsParsed() == null || resume.getIsParsed() != 1) {
            return Result.error(10016, "简历正在解析中或解析失败，请稍后再试");
        }

        // 拼接待匹配的文本
        // 岗位文本：岗位名称 + 岗位描述 + 任职要求 + 关键词
        String jobText = (job.getJobName() != null ? job.getJobName() : "")
                + " " + (job.getJobDesc() != null ? job.getJobDesc() : "")
                + " " + (job.getRequirement() != null ? job.getRequirement() : "")
                + " " + (job.getKeywords() != null ? job.getKeywords() : "");

        ResumeParseResult parseResult = resumeParseResultMapper.selectByResumeId(resumeId);
        if (parseResult == null) {
            return Result.error(10017, "简历解析结果不存在，请重新上传");
        }

        StringBuilder resumeTextBuilder = new StringBuilder();
        if (parseResult.getSkills() != null) {
            // 将 JSON 数组转为空格分隔的字符串
            String skills = parseResult.getSkills().replaceAll("[\\[\\]\"]", "").replace(",", " ");
            resumeTextBuilder.append(skills).append(" ");
        }
        if (parseResult.getWorkExperience() != null) {
            resumeTextBuilder.append(parseResult.getWorkExperience()).append(" ");
        }
        if (parseResult.getWorkHistory() != null) {
            // 简单清理 JSON 符号
            String history = parseResult.getWorkHistory().replaceAll("[\\[\\]{}\"]", "").replace(",", " ");
            resumeTextBuilder.append(history);
        }
        String resumeText = resumeTextBuilder.toString().trim();
        if (resumeText.isEmpty()) {
            return Result.error(10017, "简历文本为空，请重新上传并等待解析");
        }
        System.out.println("===== 岗位文本 =====");
        System.out.println(jobText);
        System.out.println("===== 简历文本 =====");
        System.out.println(resumeText);
        System.out.println("==================");

        // 调用匹配服务计算分数
        double score = matchCalculateService.calculateMatch(jobText, resumeText);
        String level = matchCalculateService.getMatchLevel(score);

        Map<String, Object> data = new HashMap<>();
        data.put("match_score", score);
        data.put("match_level", level);
        data.put("algorithm", "HanLP + TF-IDF + 余弦相似度");
        return Result.success(data);
    }

    //重新计算接口
    private String buildJobText(Job job) {
        return (job.getJobName() != null ? job.getJobName() : "")
                + " " + (job.getJobDesc() != null ? job.getJobDesc() : "")
                + " " + (job.getRequirement() != null ? job.getRequirement() : "")
                + " " + (job.getKeywords() != null ? job.getKeywords() : "");
    }

    private String buildResumeText(ResumeParseResult parseResult) {
        StringBuilder sb = new StringBuilder();
        if (parseResult.getSkills() != null) {
            String skills = parseResult.getSkills().replaceAll("[\\[\\]\"]", "").replace(",", " ");
            sb.append(skills).append(" ");
        }
        if (parseResult.getWorkExperience() != null) {
            sb.append(parseResult.getWorkExperience()).append(" ");
        }
        if (parseResult.getWorkHistory() != null) {
            String history = parseResult.getWorkHistory().replaceAll("[\\[\\]{}\"]", "").replace(",", " ");
            sb.append(history);
        }
        if (parseResult.getSkills() != null) {
            String skills = parseResult.getSkills()
                    .replaceAll("[\\[\\]\"]", "")
                    .replace(",", " ")
                    .replaceAll("\\s+", " ");
            sb.append(skills).append(" ");
        }

        // 4. 工作经历：只保留核心描述和技能，去掉所有JSON字段名
        if (parseResult.getWorkHistory() != null) {
            String history = parseResult.getWorkHistory()
                    .replaceAll("[\\[\\]{}\"]", "")
                    .replaceAll("(company|position|startTime|endTime|description|coreSkills):", " ")
                    .replaceAll("\\d{4}\\.\\d{2}", " ") // 去掉时间
                    .replaceAll("至今", " ")
                    .replace(",", " ")
                    .replaceAll("\\s+", " ");
            sb.append(history);
        }

        return sb.toString().trim().toLowerCase();
    }

    @PostMapping("/match/recalculate")
    @Transactional
    public Result recalculateMatch(@RequestBody Map<String, Integer> request) {
        Integer deliveryId = request.get("delivery_id");
        if (deliveryId == null) {
            return Result.error(10001, "delivery_id 不能为空");
        }

        //查询投递记录（只需要 job_id 和 resume_id）
        Delivery delivery = deliveryMapper.selectById(deliveryId);
        if (delivery == null) {
            return Result.error(10006, "投递记录不存在");
        }

        //查询岗位信息
        Job job = jobMapper.selectById(delivery.getJobId());
        if (job == null) {
            return Result.error(10006, "岗位不存在");
        }

        //查询简历解析结果
        ResumeParseResult parseResult = resumeParseResultMapper.selectByResumeId(delivery.getResumeId());
        if (parseResult == null) {
            return Result.error(10017, "简历解析结果不存在，请重新上传");
        }

        //拼接文本
        String jobText = buildJobText(job);
        String resumeText = buildResumeText(parseResult);
        if (resumeText.isEmpty()) {
            return Result.error(10017, "简历文本为空，请重新上传并等待解析");
        }

        // 清除缓存（避免使用旧的匹配分数）
        matchCalculateService.clearCache(jobText, resumeText);

        // 调用匹配服务重新计算
        double newScore = matchCalculateService.calculateMatch(jobText, resumeText);
        String newLevel = matchCalculateService.getMatchLevel(newScore);

        // 更新或插入 ai_match_result 表
        AiMatchResult existing = aiMatchResultMapper.selectByDeliveryId(deliveryId);
        if (existing == null) {
            existing = new AiMatchResult();
            existing.setDeliveryId(deliveryId);
            existing.setMatchScore(newScore);
            existing.setMatchLevel(newLevel);
            aiMatchResultMapper.insert(existing);
        } else {
            existing.setMatchScore(newScore);
            existing.setMatchLevel(newLevel);
            // 同样可以更新其他字段
            aiMatchResultMapper.updateByDeliveryId(existing);
        }

        // 返回结果
        Map<String, Object> data = new HashMap<>();
        data.put("match_score", newScore);
        data.put("match_level", newLevel);
        return Result.success(data);
    }
}

