package com.recruit.airecruitsystem.vo.seeker;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
/*
 * 求职者消息列表
 * */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeekerMessageListItemVO {
    private Integer messageId;
    private String hrName;
    // 新增HR头像
    private String hrAvatar;
    private String companyName;
    private String jobName;
    private Integer status;
    private String statusText;
    private LocalDate interviewDate;
    private LocalTime interviewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String rejectReason;
}