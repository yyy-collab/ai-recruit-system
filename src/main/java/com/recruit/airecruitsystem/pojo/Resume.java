package com.recruit.airecruitsystem.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Resume {
    private Integer id;
    private Integer seekerId;
    private String fileName;
    private String fileUrl;
    private Integer isParsed;
    private String parseFailReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
