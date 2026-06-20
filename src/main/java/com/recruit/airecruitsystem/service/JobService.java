package com.recruit.airecruitsystem.service;

import com.recruit.airecruitsystem.dto.common.PageResponse;
import com.recruit.airecruitsystem.dto.job.HrJobListItemResponse;
import com.recruit.airecruitsystem.dto.job.JobAddRequest;
import com.recruit.airecruitsystem.dto.job.JobCreateResponse;
import com.recruit.airecruitsystem.dto.job.JobStatusUpdateRequest;
import com.recruit.airecruitsystem.dto.job.JobUpdateRequest;
import com.recruit.airecruitsystem.dto.job.SeekerJobDetailResponse;
import com.recruit.airecruitsystem.dto.job.SeekerJobListItemResponse;

public interface JobService {

    JobCreateResponse addJob(JobAddRequest request);

    void updateJob(JobUpdateRequest request);

    void changeStatus(JobStatusUpdateRequest request);

    PageResponse<HrJobListItemResponse> getMyJobs(Integer pageNum, Integer pageSize, Integer status, String jobName, String salary, String workAddress);

    PageResponse<SeekerJobListItemResponse> getOnlineJobs(Integer pageNum, Integer pageSize, String jobName, String salary, String workAddress, String sort);

    SeekerJobDetailResponse getOnlineJobDetail(Integer jobId);

    void deleteJob(Integer id);
}
