package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.ResumeParseResult;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ResumeParseResultMapper {
    @Select("SELECT * FROM resume_parse_result WHERE resume_id = #{resumeId}")
    ResumeParseResult selectByResumeId(Integer resumeId);

    @Insert("INSERT INTO resume_parse_result (resume_id, keyword_coverage, basic_info, work_experience, skills, work_history, ai_summary, improvement_suggestions, create_time, update_time) " +
            "VALUES (#{resumeId}, #{keywordCoverage}, #{basicInfo}, #{workExperience}, #{skills}, #{workHistory}, #{aiSummary}, #{improvementSuggestions}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ResumeParseResult result);

    @Update("UPDATE resume_parse_result SET keyword_coverage = #{keywordCoverage}, basic_info = #{basicInfo}, work_experience = #{workExperience}, skills = #{skills}, work_history = #{workHistory}, ai_summary = #{aiSummary}, improvement_suggestions = #{improvementSuggestions}, update_time = NOW() WHERE resume_id = #{resumeId}")
    int updateByResumeId(ResumeParseResult result);

    @Delete("DELETE FROM resume_parse_result WHERE resume_id = #{resumeId}")
    int deleteByResumeId(Integer resumeId);
}