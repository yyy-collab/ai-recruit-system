package com.recruit.airecruitsystem.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * 投递记录实体类，对应表 delivery
 * 用于记录求职者对岗位的投递信息、状态、HR备注等
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Delivery {
    private Integer id;                // 投递ID
    private Integer jobId;             // 岗位ID
    private Integer seekerId;          // 求职者ID
    private Integer resumeId;          // 投递时的简历ID（快照）
    private Integer status;            // 0-待处理，1-通过，2-淘汰，3-待面试
    private String hrComment;          // HR备注
    private LocalDateTime deliveryTime; // 投递时间
    private LocalDateTime updateTime;   // 更新时间
}