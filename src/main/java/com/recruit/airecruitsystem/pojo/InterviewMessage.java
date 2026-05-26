package com.recruit.airecruitsystem.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewMessage {
    private Integer id;                // 消息ID
    private Integer deliveryId;       // 关联投递ID
    private Integer hrId;             // HR ID
    private Integer seekerId;         // 求职者ID
    private LocalDate interviewDate;  // 面试日期
    private LocalTime interviewTime;  // 面试时间
    private String interviewType;     // 面试形式：线上面试 / 现场面试 / 电话面试
    private String interviewRound;    // 面试轮次：初试 / 复试 / 终试
    private String interviewAddress;  // 面试地点/链接
    private String contactName;       // 联系人
    private String contactPhone;      // 联系人电话
    private String remark;            // 备注
    private Integer status;           // 0-待确认，1-已接受，2-已拒绝
    private String rejectReason;      // 拒绝原因
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}