package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.InterviewMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface InterviewMessageMapper {

    // 插入面试邀请
    @Insert("INSERT INTO interview_message (delivery_id, hr_id, seeker_id, interview_date, interview_time, " +
            "interview_type, interview_round, interview_address, contact_name, contact_phone, remark, status) " +
            "VALUES (#{deliveryId}, #{hrId}, #{seekerId}, #{interviewDate}, #{interviewTime}, #{interviewType}, " +
            "#{interviewRound}, #{interviewAddress}, #{contactName}, #{contactPhone}, #{remark}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InterviewMessage message);

    // 根据ID查询消息
    @Select("SELECT * FROM interview_message WHERE id = #{id}")
    InterviewMessage selectById(Integer id);

    // 查询HR发送的消息列表（支持分页和状态筛选）
    @Select("<script>" +
            "SELECT * FROM interview_message WHERE hr_id = #{hrId} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<InterviewMessage> selectByHrId(@Param("hrId") Integer hrId, @Param("status") Integer status);

    // 查询求职者收到的消息列表（支持分页和状态筛选）
    @Select("<script>" +
            "SELECT * FROM interview_message WHERE seeker_id = #{seekerId} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<InterviewMessage> selectBySeekerId(@Param("seekerId") Integer seekerId, @Param("status") Integer status);

    // 更新消息状态（接受/拒绝）
    @Update("UPDATE interview_message SET status = #{status}, reject_reason = #{rejectReason}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status, @Param("rejectReason") String rejectReason);

    // 检查消息是否属于指定HR（权限校验）
    @Select("SELECT COUNT(*) > 0 FROM interview_message WHERE id = #{id} AND hr_id = #{hrId}")
    boolean existsByHrId(@Param("id") Integer id, @Param("hrId") Integer hrId);

    // 检查消息是否属于指定求职者（权限校验）
    @Select("SELECT COUNT(*) > 0 FROM interview_message WHERE id = #{id} AND seeker_id = #{seekerId}")
    boolean existsBySeekerId(@Param("id") Integer id, @Param("seekerId") Integer seekerId);
}