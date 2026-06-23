package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.Resume;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * 简历表MyBatis数据访问层接口
 * 负责简历主表的增删改查、统计投递关联数量等数据库操作
 */
@Mapper
public interface ResumeMapper {

    /**
     * 根据简历主键ID查询单条简历完整记录
     * @param id 简历主键id
     * @return 简历实体对象，无数据返回null
     */
    @Select("SELECT * FROM resume WHERE id = #{id}")
    Resume selectById(Integer id);

    /**
     * 根据求职者ID查询最新一条简历（按id倒序取1条）
     * @param seekerId 求职者用户ID
     * @return 该求职者最新简历实体
     */
    @Select("SELECT * FROM resume WHERE seeker_id = #{seekerId} ORDER BY id DESC LIMIT 1")
    Resume selectCurrentBySeekerId(Integer seekerId);

    /**
     * 根据求职者ID查询全部简历，按创建id倒序排列
     * @param seekerId 求职者用户ID
     * @return 简历实体集合
     */
    @Select("SELECT * FROM resume WHERE seeker_id = #{seekerId} ORDER BY id DESC")
    List<Resume> selectBySeekerIdOrderByIdDesc(Integer seekerId);

    /**
     * 新增简历记录，自动回填自增主键id到实体
     * @param resume 简历实体
     * @return 数据库受影响行数
     */
    @Insert("INSERT INTO resume (seeker_id, file_name, file_url, is_parsed, parse_fail_reason, create_time, update_time) " +
            "VALUES (#{seekerId}, #{fileName}, #{fileUrl}, #{isParsed}, #{parseFailReason}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Resume resume);

    /**
     * 更新简历文件相关信息（文件名、文件地址、解析状态、失败原因、更新时间）
     * @param resume 携带更新字段的简历实体
     * @return 数据库受影响行数
     */
    @Update("UPDATE resume SET file_name = #{fileName}, file_url = #{fileUrl}, is_parsed = #{isParsed}, parse_fail_reason = #{parseFailReason}, update_time = NOW() WHERE id = #{id}")
    int updateFileInfo(Resume resume);

    /**
     * 仅更新简历解析状态与解析失败原因，自动刷新更新时间
     * @param id 简历主键ID
     * @param isParsed 解析状态：2=解析中 1=解析成功 3=解析失败
     * @param parseFailReason 解析失败备注信息，成功时传null
     * @return 数据库受影响行数
     */
    @Update("UPDATE resume SET is_parsed = #{isParsed}, parse_fail_reason = #{parseFailReason}, update_time = NOW() WHERE id = #{id}")
    int updateParseStatus(@Param("id") Integer id,
                          @Param("isParsed") Integer isParsed,
                          @Param("parseFailReason") String parseFailReason);

    /**
     * 根据简历ID删除简历主记录
     * @param id 简历主键ID
     * @return 数据库受影响行数
     */
    @Delete("DELETE FROM resume WHERE id = #{id}")
    int deleteById(Integer id);

    /**
     * 统计使用该简历的投递记录数量
     * 用于删除简历前置校验：存在投递记录则禁止删除
     * @param resumeId 简历ID
     * @return 关联投递记录条数
     */
    @Select("SELECT COUNT(*) FROM delivery WHERE resume_id = #{resumeId}")
    int countDeliveriesUsingResume(Integer resumeId);
}