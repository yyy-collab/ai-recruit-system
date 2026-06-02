package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.AiMatchResult;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AiMatchResultMapper {

    @Select("SELECT * FROM ai_match_result WHERE delivery_id = #{deliveryId}")
    AiMatchResult selectByDeliveryId(Integer deliveryId);

    @Insert("INSERT INTO ai_match_result(delivery_id, match_score, match_level, analysis_time, update_time) " +
            "VALUES(#{deliveryId}, #{matchScore}, #{matchLevel}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiMatchResult record);

    @Update("UPDATE ai_match_result SET match_score = #{matchScore}, match_level = #{matchLevel}, update_time = NOW() WHERE delivery_id = #{deliveryId}")
    int updateByDeliveryId(AiMatchResult record);
}