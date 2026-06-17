package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.Resume;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ResumeMapper {
    @Select("SELECT * FROM resume WHERE id = #{id}")
    Resume selectById(Integer id);

    @Select("SELECT * FROM resume WHERE seeker_id = #{seekerId} ORDER BY id DESC LIMIT 1")
    Resume selectCurrentBySeekerId(Integer seekerId);

    @Select("SELECT * FROM resume WHERE seeker_id = #{seekerId} ORDER BY id DESC")
    List<Resume> selectBySeekerIdOrderByIdDesc(Integer seekerId);

    @Insert("INSERT INTO resume (seeker_id, file_name, file_url, is_parsed, parse_fail_reason, create_time, update_time) " +
            "VALUES (#{seekerId}, #{fileName}, #{fileUrl}, #{isParsed}, #{parseFailReason}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Resume resume);

    @Update("UPDATE resume SET file_name = #{fileName}, file_url = #{fileUrl}, is_parsed = #{isParsed}, parse_fail_reason = #{parseFailReason}, update_time = NOW() WHERE id = #{id}")
    int updateFileInfo(Resume resume);

    @Update("UPDATE resume SET is_parsed = #{isParsed}, parse_fail_reason = #{parseFailReason}, update_time = NOW() WHERE id = #{id}")
    int updateParseStatus(@Param("id") Integer id,
                          @Param("isParsed") Integer isParsed,
                          @Param("parseFailReason") String parseFailReason);

    @Delete("DELETE FROM resume WHERE id = #{id}")
    int deleteById(Integer id);

    @Select("SELECT COUNT(*) FROM delivery WHERE resume_id = #{resumeId}")
    int countDeliveriesUsingResume(Integer resumeId);
}
