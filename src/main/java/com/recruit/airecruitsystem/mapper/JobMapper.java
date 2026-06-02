package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.dto.job.HrJobListItemResponse;
import com.recruit.airecruitsystem.dto.job.SeekerJobDetailResponse;
import com.recruit.airecruitsystem.dto.job.SeekerJobListItemResponse;
import com.recruit.airecruitsystem.pojo.Job;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface JobMapper {

    /**
     * 插入岗位
     */
    int insert(Job job);

    /**
     * 根据主键查询岗位
     */
    Job selectById(@Param("id") Integer id);

    /**
     * 选择性更新（只更新非 null 字段）
     */
    int updateSelective(Job job);

    /**
     * HR 端分页查询自己的岗位列表
     */
    List<HrJobListItemResponse> selectHrJobList(@Param("hrId") Integer hrId,
                                                @Param("status") Integer status,
                                                @Param("jobName") String jobName);

    /**
     * 求职者端分页查询在线岗位列表
     */
    List<SeekerJobListItemResponse> selectSeekerJobList(@Param("seekerId") Integer seekerId,
                                                        @Param("jobName") String jobName);

    /**
     * 求职者端查询岗位详情
     */
    SeekerJobDetailResponse selectOnlineJobDetail(@Param("jobId") Integer jobId);

    /**
     * 根据主键删除（物理删除）
     */
    int deleteById(@Param("id") Integer id);

    @Select("SELECT id, job_name, job_desc, requirement, keywords, salary, status, work_address, work_experience, create_time, update_time FROM job WHERE status = 1")
    List<Job> selectAllOnline();
    @Select("SELECT * FROM job WHERE hr_id = #{hrId} ORDER BY id LIMIT 1")
    Job selectFirstByHrId(Integer hrId);
}