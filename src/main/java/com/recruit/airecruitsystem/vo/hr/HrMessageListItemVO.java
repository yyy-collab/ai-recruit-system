package com.recruit.airecruitsystem.vo.hr;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
/**
 * HR端消息列表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrMessageListItemVO {
    private Integer messageId;
    private String seekerName;
    // 新增求职者头像
    private String seekerAvatar;
    private String jobName;
    private Integer status;
    private String statusText;      // 由枚举转换
    private LocalDate interviewDate;
    private LocalTime interviewTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String rejectReason;
}