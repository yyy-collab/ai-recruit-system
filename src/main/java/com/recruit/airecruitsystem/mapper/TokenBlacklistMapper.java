package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.TokenBlacklist;
import org.apache.ibatis.annotations.*;

@Mapper
public interface TokenBlacklistMapper {

    /**
     * 插入黑名单记录
     */
    @Insert("INSERT INTO token_blacklist (token, expire_time, create_time) VALUES (#{token}, #{expireTime}, NOW())")
    int insert(TokenBlacklist blacklist);

    /**
     * 检查 token 是否在黑名单中
     * @param token JWT 字符串
     * @return 是否存在
     */
    @Select("SELECT COUNT(*) > 0 FROM token_blacklist WHERE token = #{token}")
    boolean isBlacklisted(String token);

}