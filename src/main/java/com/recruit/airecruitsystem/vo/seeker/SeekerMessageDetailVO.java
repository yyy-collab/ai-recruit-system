package com.recruit.airecruitsystem.vo.seeker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeekerMessageDetailVO {
    private Integer messageId;
    private HrSimpleInfo hrInfo;
    private JobSimpleInfo jobInfo;
    private InterviewInfo interviewInfo;
    private Integer status;
    private String statusText;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Data
    public static class HrSimpleInfo {
        private String realName;
        private String companyName;
        private String contactName;
        private String contactPhone;
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
        private String remark;
    }
}
