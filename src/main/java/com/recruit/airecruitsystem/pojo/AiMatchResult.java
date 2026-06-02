package com.recruit.airecruitsystem.pojo;

import lombok.Data;
import java.util.Date;

@Data
public class AiMatchResult {
    private Integer id;
    private Integer deliveryId;
    private Double matchScore;
    private String matchLevel;
    private String jobKeywords;
    private String resumeKeywords;
    private String matchDetail;
    private String coreAdvantages;
    private String potentialRisks;
    private String skillTags;
    private Date analysisTime;
}