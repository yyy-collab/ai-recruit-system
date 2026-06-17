package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.Delivery;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeliveryMapper {

    /**
     * 根据ID查询投递记录
     * 用于面试邀请中获取投递关联的岗位和求职者信息
     */
    @Select("SELECT * FROM delivery WHERE id = #{id}")
    Delivery selectById(Integer id);

    /**
     * 更新投递状态（用于发送面试邀请时将状态改为3-待面试，或HR淘汰/通过时修改）
     * @param id 投递ID
     * @param status 新状态
     * @param comment 备注（淘汰时必填，其他情况可选）
     */
    @Update("UPDATE delivery SET status = #{status}, hr_comment = #{comment}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status, @Param("comment") String comment);

    /**
     * 仅更新投递状态（不修改备注），用于快速设置状态
     */
    @Update("UPDATE delivery SET status = #{status}, update_time = NOW() WHERE id = #{id}")
    int updateStatusOnly(@Param("id") Integer id, @Param("status") Integer status);

    /**
     * 检查求职者是否已投递过某岗位（幂等校验）
     * 投递接口中使用，避免重复投递
     */
    @Select("SELECT COUNT(*) > 0 FROM delivery WHERE job_id = #{jobId} AND seeker_id = #{seekerId}")
    boolean existsByJobAndSeeker(@Param("jobId") Integer jobId, @Param("seekerId") Integer seekerId);

    /**
     * 插入投递记录（求职者投递时调用）
     */
    @Insert("INSERT INTO delivery (job_id, seeker_id, resume_id, status, delivery_time, update_time) " +
            "VALUES (#{jobId}, #{seekerId}, #{resumeId}, 0, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Delivery delivery);

    /**
     * 查询求职者的投递记录列表（用于分页展示）
     */
    @Select("SELECT * FROM delivery WHERE seeker_id = #{seekerId} ORDER BY delivery_time DESC")
    List<Delivery> selectBySeekerId(Integer seekerId);

    @Select("SELECT COUNT(*) FROM delivery WHERE seeker_id = #{seekerId} AND status = 0")
    int countPendingBySeekerId(Integer seekerId);

    /**
     * 查询某个岗位下的所有投递记录（HR查看）
     */
    @Select("SELECT * FROM delivery WHERE job_id = #{jobId} ORDER BY delivery_time DESC")
    List<Delivery> selectByJobId(Integer jobId);

    @Select("SELECT * FROM delivery WHERE seeker_id = #{seekerId} AND status = #{status}")
    List<Delivery> selectBySeekerIdAndStatus(
            @Param("seekerId") Integer seekerId,
            @Param("status") Integer status
    );

    /**
     * 5.2 求职者查看我的投递记录（关联查询：岗位、公司、简历、AI结果）
     */
    @Select("<script>"
            + "SELECT "
            + "d.id                  AS delivery_id, "
            + "d.job_id              AS job_id, "
            + "j.job_name            AS job_name, "
            + "h.company_name        AS company_name, "
            + "j.salary              AS salary, "
            + "r.file_name           AS resume_file_name, "
            + "(SELECT am.match_score FROM ai_match_result am WHERE am.delivery_id = d.id ORDER BY am.update_time DESC, am.id DESC LIMIT 1) AS match_score, "
            + "(SELECT am.match_level FROM ai_match_result am WHERE am.delivery_id = d.id ORDER BY am.update_time DESC, am.id DESC LIMIT 1) AS match_level, "
            + "d.status              AS status, "
            + "d.delivery_time       AS delivery_time, "
            + "d.update_time         AS update_time "
            + "FROM delivery d "
            + "LEFT JOIN job j ON d.job_id = j.id "
            + "LEFT JOIN hr h ON j.hr_id = h.id "
            + "LEFT JOIN resume r ON d.resume_id = r.id "
            + "WHERE d.seeker_id = #{seekerId} "
            + "<if test='status != null'>AND d.status = #{status}</if> "
            + "ORDER BY d.delivery_time DESC"
            + "</script>")
    List<Map<String, Object>> selectMyDeliveryList(
            @Param("seekerId") Integer seekerId,
            @Param("status") Integer status
    );
    /**
     * 5.3HR查询岗位投递列表（严格匹配目标返回字段）
     */
    @Select("<script>"
            + "SELECT "
            + "d.id AS delivery_id, "
            + "d.seeker_id, "
            + "s.real_name AS seeker_name, "
            + "d.resume_id, "
            + "r.file_name AS resume_file_name, "
            + "(SELECT am.match_score FROM ai_match_result am WHERE am.delivery_id = d.id ORDER BY am.update_time DESC, am.id DESC LIMIT 1) AS match_score, "
            + "(SELECT am.match_level FROM ai_match_result am WHERE am.delivery_id = d.id ORDER BY am.update_time DESC, am.id DESC LIMIT 1) AS match_level, "
            + "d.status, "
            + "d.delivery_time "
            + "FROM delivery d "
            + "LEFT JOIN seeker s ON d.seeker_id = s.id "
            + "LEFT JOIN resume r ON d.resume_id = r.id "
            + "WHERE d.job_id = #{jobId} "
            + "<if test='status != null'>AND d.status = #{status}</if>"
            + "</script>")
    List<Map<String, Object>> selectHrDeliveryList(
            @Param("jobId") Integer jobId,
            @Param("status") Integer status
    );
    /**
     * 5.4 HR查看投递详情（包含求职者、简历、AI匹配结果）
     */
    @Select("""
SELECT 
    d.id AS delivery_id,
    d.job_id,
    j.job_name,
    d.status,
    d.delivery_time,
    d.update_time,
    s.id AS seeker_id,
    s.real_name,
    s.phone,
    s.email,
    s.age,
    s.edu_back,
    s.alma_mater,
    r.id AS resume_id,
    r.file_url AS resume_file_url,
    rp.keyword_coverage,
    rp.work_experience,
    rp.skills,
    am.match_score,
    am.match_level,
    am.core_advantages,
    am.potential_risks,
    am.skill_tags,
    am.analysis_time
FROM delivery d
LEFT JOIN job j ON d.job_id = j.id
LEFT JOIN seeker s ON d.seeker_id = s.id
LEFT JOIN resume r ON d.resume_id = r.id
LEFT JOIN resume_parse_result rp ON r.id = rp.resume_id
LEFT JOIN ai_match_result am ON d.id = am.delivery_id
WHERE d.id = #{deliveryId}
""")
    Map<String, Object> selectDeliveryDetail(@Param("deliveryId") Integer deliveryId);
    
}
