package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.ResumeParseResult;
import org.apache.ibatis.annotations.*;

/**
 * 简历解析结果表 MyBatis Mapper接口
 * 存储简历解析后的结构化JSON数据，与简历主表一对一关联
 * 提供根据简历ID查询、新增、更新、删除解析记录操作
 */
@Mapper
public interface ResumeParseResultMapper {

    /**
     * 根据简历ID查询对应的完整解析结构化数据
     * @param resumeId 简历主键ID
     * @return 简历解析结果实体，无记录返回null
     */
    @Select("SELECT * FROM resume_parse_result WHERE resume_id = #{resumeId}")
    ResumeParseResult selectByResumeId(Integer resumeId);

    /**
     * 新增简历解析结构化记录，自动回填自增主键id至实体
     * @param result 简历解析结果实体，包含各模块JSON字符串
     * @return 数据库受影响行数
     */
    @Insert("INSERT INTO resume_parse_result (resume_id, basic_info, work_experience, skills, work_history, create_time, update_time) " +
            "VALUES (#{resumeId}, #{basicInfo}, #{workExperience}, #{skills}, #{workHistory}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ResumeParseResult result);

    /**
     * 根据简历ID更新全部解析结构化字段，自动刷新更新时间
     * @param result 携带更新数据的解析结果实体
     * @return 数据库受影响行数
     */
    @Update("UPDATE resume_parse_result SET basic_info = #{basicInfo}, work_experience = #{workExperience}, skills = #{skills}, work_history = #{workHistory}, update_time = NOW() WHERE resume_id = #{resumeId}")
    int updateByResumeId(ResumeParseResult result);

    /**
     * 根据简历ID删除对应解析记录
     * 删除简历时同步调用，清理关联结构化数据
     * @param resumeId 简历主键ID
     * @return 数据库受影响行数
     */
    @Delete("DELETE FROM resume_parse_result WHERE resume_id = #{resumeId}")
    int deleteByResumeId(Integer resumeId);
}