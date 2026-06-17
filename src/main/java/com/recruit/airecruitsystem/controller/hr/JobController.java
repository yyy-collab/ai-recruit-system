package com.recruit.airecruitsystem.controller.hr;

import com.recruit.airecruitsystem.dto.common.PageResponse;
import com.recruit.airecruitsystem.dto.job.HrJobListItemResponse;
import com.recruit.airecruitsystem.dto.job.JobAddRequest;
import com.recruit.airecruitsystem.dto.job.JobCreateResponse;
import com.recruit.airecruitsystem.dto.job.JobStatusUpdateRequest;
import com.recruit.airecruitsystem.dto.job.JobUpdateRequest;
import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.service.JobService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("hrJobController")
@RequestMapping("/hr/job")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/add")
    public Result<JobCreateResponse> add(@RequestBody JobAddRequest request) {
        return Result.success("岗位发布成功", jobService.addJob(request));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody JobUpdateRequest request) {
        jobService.updateJob(request);
        return Result.success("岗位更新成功", null);
    }

    @PatchMapping("/changeStatus")
    public Result<Void> changeStatus(@RequestBody JobStatusUpdateRequest request) {
        jobService.changeStatus(request);
        return Result.success("状态修改成功", null);
    }

    @GetMapping("/myList")
    public Result<PageResponse<HrJobListItemResponse>> myList(
            @RequestParam(value = "pageNum", required = false) Integer pageNum,
            @RequestParam(value = "pageSize", required = false) Integer pageSize,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "job_name", required = false) String jobName,
            @RequestParam(value = "salary", required = false) String salary,
            @RequestParam(value = "work_address", required = false) String workAddress) {
        return Result.success(jobService.getMyJobs(pageNum, pageSize, status, jobName, salary, workAddress));
    }

    @DeleteMapping("/delete")
    public Result<Void> delete(@RequestParam("id") Integer id) {
        jobService.deleteJob(id);
        return Result.success("岗位删除成功", null);
    }
}
