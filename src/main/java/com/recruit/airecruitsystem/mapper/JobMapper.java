package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.Job;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface JobMapper {

    /**
     * 查询所有上线状态的岗位（用于构建语料库）
     * 只查 status = 1 的，因为下线的岗位不会再有投递
     */
    @Select("SELECT id, job_name, job_desc, requirement, keywords, salary, status, work_address, work_experience, create_time, update_time FROM job WHERE status = 1")
    List<Job> selectAllOnline();

    //根据 id 查询岗位
    @Select("SELECT * FROM job WHERE id = #{id}")
    Job selectById(Integer id);
}