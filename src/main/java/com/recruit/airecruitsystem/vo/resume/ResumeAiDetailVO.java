package com.recruit.airecruitsystem.vo.resume;

import com.recruit.airecruitsystem.model.ResumeAnalysisSnapshot;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ResumeAiDetailVO {
    private Integer resumeId;
    private String resumeFileName;
    private String resumeFileUrl;
    private Integer isParsed;
    private String previewText;
    private ResumeAnalysisSnapshot analysis;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
