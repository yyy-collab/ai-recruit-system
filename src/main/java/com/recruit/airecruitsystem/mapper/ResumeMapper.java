package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.Resume;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ResumeMapper {
    @Select("SELECT * FROM resume WHERE id = #{resumeId}")
    Resume selectById(Integer resumeId);
}