package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.Seeker;
import org.apache.ibatis.annotations.*;

@Mapper
public interface SeekerMapper {

    //根据用户名查询求职者
    @Select("select * from seeker where username=#{username}")
    Seeker findByUsername(String username);

    //插入求职者
    @Insert("insert into seeker (username,password,create_time,update_time)" +
            "values (#{username},#{password},now(),now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")     //自动回填主键
    int insert(Seeker seeker);

    //根据id查找
    @Select("SELECT * FROM seeker WHERE id = #{id}")
    Seeker findById(Integer id);

    @Select("SELECT * FROM seeker WHERE email = #{email}")
    Seeker findByEmail(String email);

    @Update("UPDATE seeker SET password = #{password}, update_time = NOW() WHERE id = #{id}")
    int updatePassword(@Param("id") Integer id, @Param("password") String password);

    //根据ID删除
    @Delete("DELETE FROM seeker WHERE id = #{id}")
    int deleteById(Integer id);

    /**
     * 增加刷新次数，并更新最后刷新时间
     * @param id 求职者ID
     * @return 受影响行数
     */
    @Update("UPDATE seeker SET refresh_count = refresh_count + 1, last_refresh_time = NOW() WHERE id = #{id}")
    int incrementRefreshCount(Integer id);

    /**
     * 重置刷新次数（用于跨日重置）
     */
    @Update("UPDATE seeker SET refresh_count = 0, last_refresh_time = NULL WHERE id = #{id}")
    int resetRefreshCount(Integer id);

    int updateSeeker(Seeker seeker);
}
