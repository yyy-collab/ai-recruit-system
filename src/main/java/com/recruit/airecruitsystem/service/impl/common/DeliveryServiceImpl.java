package com.recruit.airecruitsystem.service.impl.common;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.recruit.airecruitsystem.dto.hr.BatchDeliveryStatusUpdateDTO;
import com.recruit.airecruitsystem.mapper.DeliveryMapper;
import com.recruit.airecruitsystem.mapper.JobMapper;
import com.recruit.airecruitsystem.mapper.ResumeMapper;
import com.recruit.airecruitsystem.mapper.SeekerMapper;
import com.recruit.airecruitsystem.pojo.Delivery;
import com.recruit.airecruitsystem.pojo.Job;
import com.recruit.airecruitsystem.pojo.Resume;
import com.recruit.airecruitsystem.pojo.Seeker;
import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.service.ai.HanLPService;
import com.recruit.airecruitsystem.service.ai.KeywordExtractService;
import com.recruit.airecruitsystem.service.ai.MatchCalculateService;
import com.recruit.airecruitsystem.service.common.DeliveryService;
import com.recruit.airecruitsystem.utils.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;

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

    // ====================== 注入AI接口 ======================
    @Autowired
    private HanLPService hanLPService;

    @Autowired
    private KeywordExtractService keywordExtractService;

    @Autowired
    private MatchCalculateService matchCalculateService;

    @Override
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

            // 1. 参数校验
            if (delivery.getJobId() == null) {
                return Result.error(10002, "岗位ID不能为空");
            }
            delivery.setSeekerId(seekerId);

            if (delivery.getResumeId() == null) {
                Resume resume = resumeMapper.selectBySeekerId(seekerId);
                if (resume == null) {
                    return Result.error(10018, "请先上传简历后再投递");
                }
                delivery.setResumeId(resume.getId());
            }

            // 2. 防重复投递
            boolean exists = deliveryMapper.existsByJobAndSeeker(delivery.getJobId(), delivery.getSeekerId());
            if (exists) {
                return Result.error(10012, "不能重复投递同一岗位");
            }

            // 3. 查询数据
            Seeker seeker = seekerMapper.findById(delivery.getSeekerId());
            Job job = jobMapper.selectById(delivery.getJobId());
            if (seeker == null || job == null) {
                return Result.error(10006, "资源不存在");
            }

            // ====================== 简历解析状态校验 ======================
            Resume resume = resumeMapper.selectById(delivery.getResumeId());
            if (resume == null) {
                return Result.error(10006, "简历不存在");
            }
            Integer isParsed = resume.getIsParsed();
            if (isParsed == null || isParsed == 0) {
                return Result.error(10016, "简历解析中，暂无法投递");
            }
            if (isParsed == 2) {
                return Result.error(10017, "简历解析失败，无法投递");
            }
            // =======================================================================

            // ====================== 调用AI接口 ======================
            String resumeText = seeker.getEduBack() + " " + seeker.getExPosition() + " " + seeker.getState();
            String jobText = job.getJobName() + " " + job.getJobDesc() + " " + job.getRequirement();

            //分词
            hanLPService.segment(resumeText, true);
            hanLPService.segment(jobText, true);

            // 直接计算匹配分数
            double scoreDouble = matchCalculateService.calculateMatch(jobText, resumeText);
            BigDecimal score = new BigDecimal(scoreDouble);
            String level = matchCalculateService.getMatchLevel(scoreDouble);
            // ============================================================

            // 5. 插入投递记录
            delivery.setStatus(0);
            deliveryMapper.insert(delivery);

            // 6. 返回结果
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


    // 3. 我的投递（PageHelper 分页 + 状态筛选）
    @Override
    public Result getMyDelivery(Integer pageNum, Integer pageSize, Integer status) {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            Integer seekerId = jwtUtil.getUserIdFromRequest(request);

            if (seekerId == null) {
                return Result.error(10007, "用户未登录");
            }

            // 分页
            PageHelper.startPage(pageNum, pageSize);

            List<Map<String, Object>> list = deliveryMapper.selectMyDeliveryList(seekerId, status);
            PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);

            // 封装成你要的格式
            Map<String, Object> data = new HashMap<>();
            data.put("total", pageInfo.getTotal());
            data.put("items", pageInfo.getList());

            return Result.success("操作成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(10001, "系统繁忙");
        }
    }
    // 4. HR查看岗位投递（PageHelper分页 + 动态排序 + 状态筛选 + 多表联查）
    @Override
    public Result getByJobId(Integer jobId, Integer pageNum, Integer pageSize, Integer status, String sort) {
        try {
            // 1. 校验岗位ID
            if (jobId == null || jobId <= 0) {
                return Result.error(10004, "岗位ID不合法");
            }

            // 2. 校验HR登录状态
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            Integer hrId = jwtUtil.getUserIdFromRequest(request);
            if (hrId == null) {
                return Result.error(10007, "用户未登录");
            }

            // 3. 校验当前HR是否为该岗位发布人（防越权）
            Job job = jobMapper.selectById(jobId);
            if (job == null || !job.getHrId().equals(hrId)) {
                return Result.error(10015, "无权查看该岗位投递数据");
            }

            // 4. 分页
            PageHelper.startPage(pageNum, pageSize);

            // 5. 动态排序（按前端传参设置排序规则）
            String orderSql = "delivery_time DESC"; // 默认：投递时间降序
            if ("match_score_desc".equals(sort)) {
                orderSql = "match_score DESC";
            } else if ("time_desc".equals(sort)) {
                orderSql = "delivery_time DESC";
            }
            PageHelper.orderBy(orderSql);

            // 6. 联表查询所需字段
            List<Map<String, Object>> list = deliveryMapper.selectHrDeliveryList(jobId, status);

            // 7. 封装分页结果
            PageInfo<Map<String, Object>> pageInfo = new PageInfo<>(list);

            // 8. 按要求组装响应格式：{total, items}
            Map<String, Object> data = new HashMap<>();
            data.put("total", pageInfo.getTotal());
            data.put("items", pageInfo.getList());

            return Result.success("操作成功", data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(10001, "系统繁忙");
        }
    }
    // hr查看简历详情
    @Override
    public Result getDeliveryDetail(Integer deliveryId) {
        try {
            // 1. 校验参数
            if (deliveryId == null || deliveryId <= 0) {
                return Result.error(10004, "投递ID不合法");
            }

            // 2. 校验HR登录状态
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            Integer hrId = jwtUtil.getUserIdFromRequest(request);
            if (hrId == null) {
                return Result.error(10007, "用户未登录");
            }

            // 3. 查询投递详情
            Map<String, Object> rawData = deliveryMapper.selectDeliveryDetail(deliveryId);
            if (rawData == null) {
                return Result.error(10005, "投递记录不存在");
            }

            // 4. 权限校验：检查该投递对应的岗位是否属于当前HR
            Integer jobId = (Integer) rawData.get("job_id");
            Job job = jobMapper.selectById(jobId);
            if (job == null || !job.getHrId().equals(hrId)) {
                return Result.error(10015, "无权查看该投递详情");
            }

            // 5. 按需求组装响应结构
            Map<String, Object> data = new HashMap<>();
            data.put("delivery_id", rawData.get("delivery_id"));
            data.put("job_id", rawData.get("job_id"));
            data.put("job_name", rawData.get("job_name"));
            data.put("status", rawData.get("status"));
            data.put("delivery_time", rawData.get("delivery_time"));
            data.put("update_time", rawData.get("update_time"));

            // 组装求职者信息
            Map<String, Object> seekerInfo = new HashMap<>();
            seekerInfo.put("id", rawData.get("seeker_id"));
            seekerInfo.put("real_name", rawData.get("real_name"));
            seekerInfo.put("phone", rawData.get("phone"));
            seekerInfo.put("email", rawData.get("email"));
            seekerInfo.put("age", rawData.get("age"));
            seekerInfo.put("edu_back", rawData.get("edu_back"));
            seekerInfo.put("alma_mater", rawData.get("alma_mater"));
            data.put("seeker_info", seekerInfo);

            // 组装简历信息
            Map<String, Object> resumeInfo = new HashMap<>();
            resumeInfo.put("resume_id", rawData.get("resume_id"));
            resumeInfo.put("resume_file_url", rawData.get("resume_file_url"));
            resumeInfo.put("keyword_coverage", rawData.get("keyword_coverage"));

            Map<String, Object> parsedData = new HashMap<>();
            parsedData.put("work_experience", rawData.get("work_experience"));
            parsedData.put("skills", rawData.get("skills"));
            resumeInfo.put("parsed_data", parsedData);
            data.put("resume_info", resumeInfo);

            // 组装匹配信息
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
    // 5. 修改投递状态
    @Override
    public Result updateStatus(Integer deliveryId, Integer status, String comment) {
        try {
            deliveryMapper.updateStatus(deliveryId, status, comment);
            return Result.success("状态更新成功");
        } catch (Exception e) {
            return Result.error(10001, "系统繁忙");
        }
    }
    // 6. 批量修改状态
    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Integer> batchUpdateStatus(BatchDeliveryStatusUpdateDTO dto) {
        List<Integer> deliveryIds = dto.getDelivery_ids();
        Integer newStatus = dto.getStatus();
        String rejectReason = dto.getReject_reason();
        List<Integer> successIds = new ArrayList<>();

        // 1. 校验：status=2 时 reject_reason 必填
        if (newStatus == 2 && (rejectReason == null || rejectReason.isBlank())) {
            throw new RuntimeException("淘汰时必须填写拒绝原因");
        }

        // 2. 校验：禁止设置为 3（待面试）
        if (newStatus == 3) {
            throw new RuntimeException("禁止设置为待面试状态");
        }

        // 3. 逐个更新（去掉了权限校验）
        for (Integer deliveryId : deliveryIds) {
            Delivery delivery = deliveryMapper.selectById(deliveryId);
            if (delivery == null) {
                continue; // 不存在的投递直接跳过
            }

            // 校验状态不可回退
            if (newStatus < delivery.getStatus()) {
                continue;
            }

            // 执行更新
            deliveryMapper.updateStatus(deliveryId, newStatus, rejectReason);
            successIds.add(deliveryId);
        }

        return successIds;
    }
}