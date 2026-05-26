package com.recruit.airecruitsystem.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.dto.common.PageResponse;
import com.recruit.airecruitsystem.dto.job.HrJobListItemResponse;
import com.recruit.airecruitsystem.dto.job.JobAddRequest;
import com.recruit.airecruitsystem.dto.job.JobCreateResponse;
import com.recruit.airecruitsystem.dto.job.JobStatusUpdateRequest;
import com.recruit.airecruitsystem.dto.job.JobUpdateRequest;
import com.recruit.airecruitsystem.dto.job.SeekerJobDetailResponse;
import com.recruit.airecruitsystem.dto.job.SeekerJobListItemResponse;
import com.recruit.airecruitsystem.exception.BusinessException;
import com.recruit.airecruitsystem.mapper.HrMapper;
import com.recruit.airecruitsystem.mapper.JobMapper;
import com.recruit.airecruitsystem.pojo.Hr;
import com.recruit.airecruitsystem.pojo.Job;
import com.recruit.airecruitsystem.service.JobService;
import com.recruit.airecruitsystem.utils.UserContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobServiceImpl implements JobService {

    private static final int DEFAULT_PAGE_NUM = 1;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final JobMapper jobMapper;
    private final HrMapper hrMapper;

    public JobServiceImpl(JobMapper jobMapper, HrMapper hrMapper) {
        this.jobMapper = jobMapper;
        this.hrMapper = hrMapper;
    }

    @Override
    public JobCreateResponse addJob(JobAddRequest request) {
        Integer hrId = requireCurrentUserId();
        Hr hr = hrMapper.selectById(hrId);
        if (hr == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "HR信息不存在");
        }
        if (!StringUtils.hasText(hr.getCompanyName())) {
            throw new BusinessException(ResultCode.INFO_INCOMPLETE, "请先完善个人信息后再进行此操作");
        }

        Job job = new Job();
        job.setHrId(hrId);
        job.setJobName(requireText(request.getJobName(), "岗位名称不能为空"));
        job.setJobDesc(requireText(request.getJobDesc(), "岗位描述不能为空"));
        job.setRequirement(requireText(request.getRequirement(), "任职要求不能为空"));
        job.setKeywords(normalizeKeywords(request.getKeywords()));
        job.setSalary(normalizeOptionalText(request.getSalary()));
        job.setWorkAddress(normalizeOptionalText(request.getWorkAddress()));
        job.setWorkExperience(normalizeOptionalText(request.getWorkExperience()));
        job.setStatus(1);
        job.setDeleted(0);
        job.setDeliveryCount(0);
        jobMapper.insert(job);

        JobCreateResponse response = new JobCreateResponse();
        response.setJobId(job.getId());
        return response;
    }

    @Override
    public void updateJob(JobUpdateRequest request) {
        Integer hrId = requireCurrentUserId();
        Integer jobId = requirePositiveId(request.getId(), "岗位ID不能为空");
        Job existingJob = requireOwnedJob(jobId, hrId);

        Job updateJob = new Job();
        updateJob.setId(existingJob.getId());
        updateJob.setJobName(normalizePatchRequiredText(request.getJobName(), "岗位名称不能为空"));
        updateJob.setJobDesc(normalizePatchRequiredText(request.getJobDesc(), "岗位描述不能为空"));
        updateJob.setRequirement(normalizePatchRequiredText(request.getRequirement(), "任职要求不能为空"));
        updateJob.setKeywords(request.getKeywords() == null ? null : normalizeKeywords(request.getKeywords()));
        updateJob.setSalary(normalizePatchOptionalText(request.getSalary()));
        updateJob.setWorkAddress(normalizePatchOptionalText(request.getWorkAddress()));
        updateJob.setWorkExperience(normalizePatchOptionalText(request.getWorkExperience()));

        if (!hasAnyUpdatableField(updateJob)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "至少需要提供一个可更新字段");
        }

        jobMapper.updateSelective(updateJob);
    }

    @Override
    public void changeStatus(JobStatusUpdateRequest request) {
        Integer hrId = requireCurrentUserId();
        Integer jobId = requirePositiveId(request.getId(), "岗位ID不能为空");
        Integer status = request.getStatus();
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "岗位状态不合法");
        }
        requireOwnedJob(jobId, hrId);

        Job job = new Job();
        job.setId(jobId);
        job.setStatus(status);
        jobMapper.updateSelective(job);
    }

    @Override
    public PageResponse<HrJobListItemResponse> getMyJobs(Integer pageNum, Integer pageSize, Integer status, String jobName) {
        Integer hrId = requireCurrentUserId();
        int validPageNum = normalizePageNum(pageNum);
        int validPageSize = normalizePageSize(pageSize);
        Integer validStatus = normalizeStatus(status);
        String validJobName = normalizeOptionalText(jobName);

        PageHelper.startPage(validPageNum, validPageSize);
        List<HrJobListItemResponse> jobs = jobMapper.selectHrJobList(hrId, validStatus, validJobName);
        return PageResponse.of(new PageInfo<>(jobs));
    }

    @Override
    public PageResponse<SeekerJobListItemResponse> getOnlineJobs(Integer pageNum, Integer pageSize, String jobName, String sort) {
        Integer seekerId = requireCurrentUserId();
        int validPageNum = normalizePageNum(pageNum);
        int validPageSize = normalizePageSize(pageSize);
        String validJobName = normalizeOptionalText(jobName);

        PageHelper.startPage(validPageNum, validPageSize);
        PageHelper.orderBy(resolveSeekerJobOrderBy(sort));
        List<SeekerJobListItemResponse> jobs = jobMapper.selectSeekerJobList(seekerId, validJobName);
        return PageResponse.of(new PageInfo<>(jobs));
    }

    @Override
    public SeekerJobDetailResponse getOnlineJobDetail(Integer jobId) {
        requireCurrentUserId();
        Integer validJobId = requirePositiveId(jobId, "岗位ID不能为空");
        SeekerJobDetailResponse detail = jobMapper.selectOnlineJobDetail(validJobId);
        if (detail == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        return detail;
    }

    @Override
    public void deleteJob(Integer id) {
        Integer hrId = requireCurrentUserId();
        Integer validJobId = requirePositiveId(id, "岗位ID不能为空");
        requireOwnedJob(validJobId, hrId);
        Job job = new Job();
        job.setId(validJobId);
        job.setStatus(0);
        job.setDeleted(1);
        jobMapper.updateSelective(job);
    }

    private Integer requireCurrentUserId() {
        Integer userId = UserContext.getUserId();
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "未登录");
        }
        return userId;
    }

    private Job requireOwnedJob(Integer jobId, Integer hrId) {
        Job job = jobMapper.selectById(jobId);
        if (job == null || Integer.valueOf(1).equals(job.getDeleted())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "岗位不存在");
        }
        if (!hrId.equals(job.getHrId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权访问");
        }
        return job;
    }

    private Integer requirePositiveId(Integer id, String message) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, message);
        }
        return id;
    }

    private int normalizePageNum(Integer pageNum) {
        int target = pageNum == null ? DEFAULT_PAGE_NUM : pageNum;
        if (target < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "pageNum参数不合法");
        }
        return target;
    }

    private int normalizePageSize(Integer pageSize) {
        int target = pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
        if (target < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "pageSize参数不合法");
        }
        return Math.min(target, MAX_PAGE_SIZE);
    }

    private Integer normalizeStatus(Integer status) {
        if (status == null) {
            return null;
        }
        if (status != 0 && status != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "岗位状态不合法");
        }
        return status;
    }

    private String requireText(String value, String message) {
        String normalized = normalizeOptionalText(value);
        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, message);
        }
        return normalized;
    }

    private String normalizePatchRequiredText(String value, String message) {
        if (value == null) {
            return null;
        }
        return requireText(value, message);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizePatchOptionalText(String value) {
        return value == null ? null : normalizeOptionalText(value);
    }

    private String normalizeKeywords(String keywords) {
        String rawKeywords = requireText(keywords, "核心关键词不能为空");
        List<String> keywordList = Arrays.stream(rawKeywords.replace('，', ',').split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (keywordList.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "核心关键词不能为空");
        }
        return new LinkedHashSet<>(keywordList).stream().collect(Collectors.joining(","));
    }

    private boolean hasAnyUpdatableField(Job job) {
        return job.getJobName() != null
                || job.getJobDesc() != null
                || job.getRequirement() != null
                || job.getKeywords() != null
                || job.getSalary() != null
                || job.getWorkAddress() != null
                || job.getWorkExperience() != null;
    }

    private String resolveSeekerJobOrderBy(String sort) {
        String normalized = normalizeOptionalText(sort);
        if (!StringUtils.hasText(normalized)) {
            return "j.create_time DESC, j.id DESC";
        }
        return switch (normalized) {
            case "salary_desc" ->
                    "CAST(SUBSTRING_INDEX(REPLACE(UPPER(j.salary), 'K', ''), '-', -1) AS DECIMAL(10,2)) DESC, j.create_time DESC, j.id DESC";
            case "time_desc", "时间降序" -> "j.create_time DESC, j.id DESC";
            case "match_score_desc", "match_desc", "匹配度降序" ->
                    "amr.match_score DESC, j.create_time DESC, j.id DESC";
            default -> throw new BusinessException(ResultCode.PARAM_ERROR, "排序参数不合法");
        };
    }
}
