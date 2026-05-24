package com.recruit.airecruitsystem.vo.hr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
* HR端消息列表详情
* */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrMessageDetailVO {
    private Integer messageId;
    private SeekerSimpleInfo seekerInfo;
    private JobSimpleInfo jobInfo;
    private InterviewInfo interviewInfo;
    private Integer status;
    private String statusText;
    private String rejectReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Data
    public static class SeekerSimpleInfo {
        private Integer id;
        private String realName;
        private String phone;
        private String email;
    }

    @Data
    public static class JobSimpleInfo {
        private Integer id;
        private String jobName;
    }

    @Data
    public static class InterviewInfo {
        private LocalDate interviewDate;
        private LocalTime interviewTime;
        private String interviewType;
        private String interviewRound;
        private String interviewAddress;
        private String contactName;
        private String contactPhone;
        private String remark;
    }
}
