package com.recruit.airecruitsystem.vo.seeker;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 求职者登录响应数据
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeekerLoginVO {

    private String token;//JWT令牌
    private Integer expiresIn;//有效期（秒）
    private Boolean isInfoComplete;//判断信息是否完善
}
