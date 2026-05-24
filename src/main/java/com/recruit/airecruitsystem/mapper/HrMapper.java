package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.Hr;
import org.apache.ibatis.annotations.*;

@Mapper
public interface HrMapper {

    // 根据用户名查询
    @Select("SELECT * FROM hr WHERE username = #{username}")
    Hr selectByUsername(String username);

    // 根据邮箱查询（用于重置密码时校验邮箱是否存在）
    @Select("SELECT * FROM hr WHERE email = #{email}")
    Hr selectByEmail(String email);

    // 根据用户名和邮箱查询（重置密码时校验身份）
    @Select("SELECT * FROM hr WHERE username = #{username} AND email = #{email}")
    Hr selectByUsernameAndEmail(@Param("username") String username, @Param("email") String email);

    // 根据ID查询
    @Select("SELECT * FROM hr WHERE id = #{id}")
    Hr selectById(Integer id);

    // 注册插入
    @Insert("INSERT INTO hr (username, password, create_time, update_time) " +
            "VALUES (#{username}, #{password}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Hr hr);

    // 动态更新HR信息（仅更新非空字段）
    int updateHr(Hr hr);

    // 修改密码
    @Update("UPDATE hr SET password = #{password}, update_time = NOW() WHERE id = #{id}")
    int updatePasswordById(@Param("id") Integer id, @Param("password") String encodedPwd);


    //删除 HR 账号（由数据库外键级联删除关联数据）
    @Delete("DELETE FROM hr WHERE id = #{id}")
    int deleteById(Integer id);

    //查询该 HR 名下是否存在上线状态的岗位
    @Select("SELECT COUNT(*) FROM job WHERE hr_id = #{hrId} AND status = 1")
    int countOnlineJobs(Integer hrId);

    // 增加刷新次数
    @Update("UPDATE hr SET refresh_count = refresh_count + 1, last_refresh_time = NOW() WHERE id = #{id}")
    int incrementRefreshCount(Integer id);

    // 重置刷新次数（跨日）
    @Update("UPDATE hr SET refresh_count = 0, last_refresh_time = NULL WHERE id = #{id}")
    int resetRefreshCount(Integer id);
}