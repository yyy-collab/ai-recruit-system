package com.recruit.airecruitsystem.vo.hr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrLoginVO {
    private String token;
    private Integer expiresIn;
    private Boolean isInfoComplete;   // 企业信息是否完善（公司名称不为空）
}