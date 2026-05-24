package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.EmailVerifyCode;
import org.apache.ibatis.annotations.*;

@Mapper
public interface EmailVerifyCodeMapper {

    /**
     * 插入验证码记录（发送验证码时调用）
     */
    @Insert("INSERT INTO email_verify_code (email, code, type, expire_time) " +
            "VALUES (#{email}, #{code}, #{type}, #{expireTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(EmailVerifyCode record);

    /**
     * 查询最新且未过期的验证码（按 email 和 type）
     */
    @Select("SELECT * FROM email_verify_code WHERE email = #{email} AND type = #{type} " +
            "AND expire_time > NOW() ORDER BY create_time DESC LIMIT 1")
    EmailVerifyCode selectLatestValid(@Param("email") String email, @Param("type") String type);

    /**
     * 删除验证码（使用后立即失效，防止重用）
     */
    @Delete("DELETE FROM email_verify_code WHERE id = #{id}")
    int deleteById(Integer id);
}