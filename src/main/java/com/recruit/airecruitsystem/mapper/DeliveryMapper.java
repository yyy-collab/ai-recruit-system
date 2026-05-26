package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.Delivery;
import org.apache.ibatis.annotations.*;

import java.util.List;

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

    /**
     * 查询某个岗位下的所有投递记录（HR查看）
     */
    @Select("SELECT * FROM delivery WHERE job_id = #{jobId} ORDER BY delivery_time DESC")
    List<Delivery> selectByJobId(Integer jobId);
}