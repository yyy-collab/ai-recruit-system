package com.recruit.airecruitsystem.mapper;

import com.recruit.airecruitsystem.pojo.Job;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JobMapper {

    //根据id查询
    @Select("SELECT * FROM job WHERE id = #{id}")
    Job selectById(Integer id);
}
