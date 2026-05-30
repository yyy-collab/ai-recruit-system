package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.ResumeParseResult;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ResumeParseResultMapper {

    @Select("SELECT * FROM resume_parse_result WHERE resume_id = #{resumeId}")
    ResumeParseResult selectByResumeId(Integer resumeId);
}