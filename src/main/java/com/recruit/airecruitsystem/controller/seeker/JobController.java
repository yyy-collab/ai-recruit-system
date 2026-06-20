package com.recruit.airecruitsystem.controller.seeker;

import com.recruit.airecruitsystem.dto.common.PageResponse;
import com.recruit.airecruitsystem.dto.job.SeekerJobDetailResponse;
import com.recruit.airecruitsystem.dto.job.SeekerJobListItemResponse;
import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.service.JobService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("seekerJobController")
@RequestMapping("/seeker/job")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping("/list")
    public Result<PageResponse<SeekerJobListItemResponse>> list(
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "job_name", required = false) String jobName,
            @RequestParam(value = "salary", required = false) String salary,
            @RequestParam(value = "work_address", required = false) String workAddress,
            @RequestParam(value = "sort", required = false) String sort) {
        return Result.success(jobService.getOnlineJobs(pageNum, pageSize, jobName, salary, workAddress, sort));
    }

    @GetMapping("/detail")
    public Result<SeekerJobDetailResponse> detail(@RequestParam("job_id") Integer jobId) {
        return Result.success(jobService.getOnlineJobDetail(jobId));
    }
}
