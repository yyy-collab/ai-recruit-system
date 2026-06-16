package com.recruit.airecruitsystem.vo.resume;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResumeSummaryVO {
    private Integer resumeId;
    private String resumeFileName;
    private String resumeFileUrl;
    private Integer isParsed;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
