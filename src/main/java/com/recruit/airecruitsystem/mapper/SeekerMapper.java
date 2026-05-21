package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.Seeker;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

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
}
