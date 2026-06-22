package com.recruit.airecruitsystem.service.impl.common;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.recruit.airecruitsystem.dto.hr.BatchDeliveryStatusUpdateDTO;
import com.recruit.airecruitsystem.mapper.AiMatchResultMapper;
import com.recruit.airecruitsystem.mapper.DeliveryMapper;
import com.recruit.airecruitsystem.mapper.JobMapper;
import com.recruit.airecruitsystem.mapper.ResumeMapper;
import com.recruit.airecruitsystem.mapper.ResumeParseResultMapper;
import com.recruit.airecruitsystem.mapper.SeekerMapper;
import com.recruit.airecruitsystem.pojo.AiMatchResult;
import com.recruit.airecruitsystem.pojo.Delivery;
import com.recruit.airecruitsystem.pojo.Job;
import com.recruit.airecruitsystem.pojo.Resume;
import com.recruit.airecruitsystem.pojo.ResumeParseResult;
import com.recruit.airecruitsystem.pojo.Seeker;
import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.service.ai.MatchCalculateService;
import com.recruit.airecruitsystem.service.common.DeliveryService;
import com.recruit.airecruitsystem.service.resume.ResumeService;
import com.recruit.airecruitsystem.utils.JwtUtil;
import com.recruit.airecruitsystem.vo.hr.HrResumeDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeliveryServiceImpl implements DeliveryService {

    @Autowired
    private DeliveryMapper deliveryMapper;

    @Autowired
    private SeekerMapper seekerMapper;

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private ResumeMapper resumeMapper;

    @Autowired
    private ResumeParseResultMapper resumeParseResultMapper;

    @Autowired
    private AiMatchResultMapper aiMatchResultMapper;

    @Autowired
    private MatchCalculateService matchCalculateService;

    @Autowired
    private ResumeService resumeService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result addDelivery(Delivery delivery) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return Result.error(10007, "用户未登录");
            }
            HttpServletRequest request = attrs.getRequest();
            Integer seekerId = jwtUtil.getUserIdFromRequest(request);
            if (seekerId == null) {
                return Result.error(10007, "用户未登录");
            }

            if (delivery.getJobId() == null) {
                return Result.error(10002, "岗位ID不能为空");
            }
            delivery.setSeekerId(seekerId);

            if (delivery.getResumeId() == null) {
                Resume resume = resumeMapper.selectCurrentBySeekerId(seekerId);
                if (resume == null) {
                    return Result.error(10018, "请先上传简历后再投递");
                }
                delivery.setResumeId(resume.getId());
            }

            boolean exists = deliveryMapper.existsByJobAndSeeker(delivery.getJobId(), delivery.getSeekerId());
            if (exists) {
                return Result.error(10012, "不能重复投递同一岗位");
            }

            Seeker seeker = seekerMapper.findById(delivery.getSeekerId());
            Job job = jobMapper.selectById(delivery.getJobId());
            if (seeker == null || job == null) {
                return Result.error(10006, "资源不存在");
            }
            if (job.getStatus() == 0) {
                return Result.error(10018, "该岗位已下线，无法投递");
            }

            Resume resume = resumeMapper.selectById(delivery.getResumeId());
            if (resume == null) {
                return Result.error(10006, "简历不存在");
            }
            Integer isParsed = resume.getIsParsed();
            if (isParsed == null || isParsed == 0 || isParsed == 2) {
                return Result.error(10016, "简历解析中，暂无法投递");
            }
            if (isParsed == 3) {
                return Result.error(10017, "简历解析失败，无法投递");
            }

            ResumeParseResult parseResult = resumeParseResultMapper.selectByResumeId(delivery.getResumeId());
            if (parseResult == null) {
                return Result.error(10017, "简历解析结果不存在，请重新上传");
            }

            String jobText = buildJobText(job);
            String resumeText = buildResumeText(parseResult);
            if (resumeText.isEmpty()) {
                return Result.error(10017, "简历文本为空，请重新上传并等待解析");
            }

            double scoreDouble = matchCalculateService.calculateMatch(jobText, resumeText, job.getKeywords());
            BigDecimal score = BigDecimal.valueOf(scoreDouble);
            String level = matchCalculateService.getMatchLevel(scoreDouble);

            delivery.setStatus(0);
            deliveryMapper.insert(delivery);
            jobMapper.incrementDeliveryCount(delivery.getJobId());

            AiMatchResult matchResult = new AiMatchResult();
            matchResult.setDeliveryId(delivery.getId());
            matchResult.setMatchScore(scoreDouble);
            matchResult.setMatchLevel(level);
            aiMatchResultMapper.insert(matchResult);

            Map<String, Object> map = new HashMap<>();
            map.put("deliveryId", delivery.getId());
            map.put("matchScore", score);
            map.put("matchLevel", level);
            return Result.success("投递成功", map);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(10001, "系统繁忙");
        }
    }

    @Override
    public Result getMyDelivery(Integer pageNum, Integer pageSize, Integer status) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            Integer seekerId = jwtUtil.getUserIdFromRequest(request);

            if (seekerId == null) {
                return Result.error(10007, "用户未登录");
            }

            PageHelper.startPage(pageNum, pageSize);

            List<Map<String, Object>> list = deliveryMapper.selectMyDeliveryList(seekerId, status);
            PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);

            Map<String, Object> data = new HashMap<>();
            data.put("total", pageInfo.getTotal());
            data.put("items", pageInfo.getList());

            return Result.success("操作成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(10001, "系统繁忙");
        }
    }

    @Override
    public Result getByJobId(Integer jobId, Integer pageNum, Integer pageSize, Integer status, String sort) {
        try {
            if (jobId == null || jobId <= 0) {
                return Result.error(10004, "岗位ID不合法");
            }

            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            Integer hrId = jwtUtil.getUserIdFromRequest(request);
            if (hrId == null) {
                return Result.error(10007, "用户未登录");
            }

            Job job = jobMapper.selectById(jobId);
            if (job == null || !job.getHrId().equals(hrId)) {
                return Result.error(10015, "无权查看该岗位投递数据");
            }

            PageHelper.startPage(pageNum, pageSize);

            String orderSql = "delivery_time DESC";
            orderSql = resolveHrDeliveryOrderBy(sort);
            PageHelper.orderBy(orderSql);

            List<Map<String, Object>> list = deliveryMapper.selectHrDeliveryList(jobId, status);
            PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);

            Map<String, Object> data = new HashMap<>();
            data.put("total", pageInfo.getTotal());
            data.put("items", pageInfo.getList());

            return Result.success("操作成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(10001, "系统繁忙");
        }
    }

    @Override
    public Result getDeliveryDetail(Integer deliveryId) {
        try {
            if (deliveryId == null || deliveryId <= 0) {
                return Result.error(10004, "投递ID不合法");
            }

            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            Integer hrId = jwtUtil.getUserIdFromRequest(request);
            if (hrId == null) {
                return Result.error(10007, "用户未登录");
            }

            Map<String, Object> rawData = deliveryMapper.selectDeliveryDetail(deliveryId);
            if (rawData == null) {
                return Result.error(10005, "投递记录不存在");
            }

            Integer jobId = (Integer) rawData.get("job_id");
            Job job = jobMapper.selectById(jobId);
            if (job == null || !job.getHrId().equals(hrId)) {
                return Result.error(10015, "无权查看该投递详情");
            }

            HrResumeDetailVO hrDetail = resumeService.getHrDeliveryDetail(hrId, deliveryId);
            if (hrDetail == null) {
                return Result.error(10005, "投递记录不存在");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("delivery_id", rawData.get("delivery_id"));
            data.put("job_id", rawData.get("job_id"));
            data.put("job_name", rawData.get("job_name"));
            data.put("status", rawData.get("status"));
            data.put("delivery_time", rawData.get("delivery_time"));
            data.put("update_time", rawData.get("update_time"));

            Map<String, Object> seekerInfo = new HashMap<>();
            seekerInfo.put("id", hrDetail.getSeekerInfo().getId());
            seekerInfo.put("real_name", hrDetail.getSeekerInfo().getRealName());
            seekerInfo.put("phone", hrDetail.getSeekerInfo().getPhone());
            seekerInfo.put("email", hrDetail.getSeekerInfo().getEmail());
            seekerInfo.put("age", hrDetail.getSeekerInfo().getAge());
            seekerInfo.put("edu_back", hrDetail.getSeekerInfo().getEduBack());
            seekerInfo.put("alma_mater", hrDetail.getSeekerInfo().getAlmaMater());
            data.put("seeker_info", seekerInfo);

            Map<String, Object> resumeInfo = new HashMap<>();
            resumeInfo.put("resume_id", hrDetail.getResumeInfo().getResumeId());
            resumeInfo.put("resume_file_url", hrDetail.getResumeInfo().getResumeFileUrl());
            resumeInfo.put("resume_file_name", hrDetail.getResumeInfo().getResumeFileName());
            resumeInfo.put("preview_text", hrDetail.getResumeInfo().getPreviewText());

            Map<String, Object> parsedData = new HashMap<>();
            parsedData.put("basic_info", rawData.get("basic_info"));
            parsedData.put("work_experience", hrDetail.getResumeInfo().getWorkExperience());
            parsedData.put("skills", hrDetail.getResumeInfo().getSkills());
            parsedData.put("work_history", hrDetail.getResumeInfo().getWorkHistory());
            resumeInfo.put("parsed_data", parsedData);
            data.put("resume_info", resumeInfo);

            Map<String, Object> matchInfo = new HashMap<>();
            matchInfo.put("match_score", rawData.get("match_score"));
            matchInfo.put("match_level", rawData.get("match_level"));
            matchInfo.put("ai_comment", rawData.get("ai_comment"));
            matchInfo.put("core_advantages", rawData.get("core_advantages"));
            matchInfo.put("potential_risks", rawData.get("potential_risks"));
            matchInfo.put("skill_tags", rawData.get("skill_tags"));
            matchInfo.put("analysis_time", rawData.get("analysis_time"));
            data.put("match_info", matchInfo);

            return Result.success("操作成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(10001, "系统繁忙");
        }
    }

    @Override
    public Result updateStatus(Integer deliveryId, Integer status, String comment) {
        try {
            deliveryMapper.updateStatus(deliveryId, status, comment);
            return Result.success("状态更新成功");
        } catch (Exception e) {
            return Result.error(10001, "系统繁忙");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Integer> batchUpdateStatus(BatchDeliveryStatusUpdateDTO dto) {
        List<Integer> deliveryIds = dto.getDelivery_ids();
        Integer newStatus = dto.getStatus();
        String rejectReason = dto.getReject_reason();
        List<Integer> successIds = new ArrayList<>();

        if (newStatus == 2 && (rejectReason == null || rejectReason.isBlank())) {
            throw new RuntimeException("淘汰时必须填写拒绝原因");
        }

        if (newStatus == 3) {
            throw new RuntimeException("禁止设置为待面试状态");
        }

        for (Integer deliveryId : deliveryIds) {
            Delivery currentDelivery = deliveryMapper.selectById(deliveryId);
            if (currentDelivery == null) {
                continue;
            }

            if (newStatus < currentDelivery.getStatus()) {
                continue;
            }

            deliveryMapper.updateStatus(deliveryId, newStatus, rejectReason);
            successIds.add(deliveryId);
        }

        return successIds;
    }

    private String buildJobText(Job job) {
        return (job.getJobName() != null ? job.getJobName() : "")
                + " " + (job.getJobDesc() != null ? job.getJobDesc() : "")
                + " " + (job.getRequirement() != null ? job.getRequirement() : "")
                + " " + (job.getKeywords() != null ? job.getKeywords() : "");
    }

    private String buildResumeText(ResumeParseResult parseResult) {
        StringBuilder builder = new StringBuilder();
        if (parseResult.getSkills() != null) {
            String skills = parseResult.getSkills().replaceAll("[\\[\\]\"]", "").replace(",", " ");
            builder.append(skills).append(" ");
        }
        if (parseResult.getWorkExperience() != null) {
            builder.append(parseResult.getWorkExperience()).append(" ");
        }
        if (parseResult.getWorkHistory() != null) {
            String history = parseResult.getWorkHistory().replaceAll("[\\[\\]{}\"]", "").replace(",", " ");
            builder.append(history);
        }
        return builder.toString().trim();
    }

    private String resolveHrDeliveryOrderBy(String sort) {
        String normalized = sort == null ? "" : sort.trim();
        return switch (normalized) {
            case "match_score_desc" ->
                    "CASE WHEN match_score IS NULL THEN 1 ELSE 0 END ASC, match_score DESC, delivery_time DESC, delivery_id DESC";
            case "name_asc" ->
                    "seeker_name ASC, delivery_time DESC, delivery_id DESC";
            case "time_desc", "" ->
                    "delivery_time DESC, delivery_id DESC";
            default ->
                    "delivery_time DESC, delivery_id DESC";
        };
    }
}
