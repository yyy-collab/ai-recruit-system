package com.recruit.airecruitsystem.mapper;
import com.recruit.airecruitsystem.pojo.InterviewMessage;
import com.recruit.airecruitsystem.vo.hr.HrMessageListItemVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerMessageListItemVO;
import org.apache.ibatis.annotations.*;
import java.util.List;
@Mapper
public interface InterviewMessageMapper {
    // 插入面试邀请
    @Insert("INSERT INTO interview_message (delivery_id, hr_id, seeker_id, interview_date, interview_time, " +
            "interview_type, interview_round, interview_address, contact_name, contact_phone, remark, status) " +
            "VALUES (#{deliveryId}, #{hrId}, #{seekerId}, #{interviewDate}, #{interviewTime}, #{interviewType}, " +
            "#{interviewRound}, #{interviewAddress}, #{contactName}, #{contactPhone}, #{remark}, #{status})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(InterviewMessage message);

    // 根据ID查询消息（详情查询完全不变）
    @Select("SELECT * FROM interview_message WHERE id = #{id}")
    InterviewMessage selectById(Integer id);

    // 查询HR发送的消息列表：恢复单表查询，返回InterviewMessage
    @Select("<script>" +
            "SELECT * FROM interview_message WHERE hr_id = #{hrId} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<InterviewMessage> selectByHrId(@Param("hrId") Integer hrId, @Param("status") Integer status);

    // 查询求职者收到的消息列表：恢复单表查询，返回InterviewMessage
    @Select("<script>" +
            "SELECT * FROM interview_message WHERE seeker_id = #{seekerId} " +
            "<if test='status != null'> AND status = #{status} </if>" +
            "ORDER BY create_time DESC" +
            "</script>")
    List<InterviewMessage> selectBySeekerId(@Param("seekerId") Integer seekerId, @Param("status") Integer status);

    // 更新消息状态
    @Update("UPDATE interview_message SET status = #{status}, reject_reason = #{rejectReason}, update_time = NOW() WHERE id = #{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status, @Param("rejectReason") String rejectReason);

    // 检查消息是否属于指定HR
    @Select("SELECT COUNT(*) > 0 FROM interview_message WHERE id = #{id} AND hr_id = #{hrId}")
    boolean existsByHrId(@Param("id") Integer id, @Param("hrId") Integer hrId);

    // 检查消息是否属于指定求职者
    @Select("SELECT COUNT(*) > 0 FROM interview_message WHERE id = #{id} AND seeker_id = #{seekerId}")
    boolean existsBySeekerId(@Param("id") Integer id, @Param("seekerId") Integer seekerId);
}
