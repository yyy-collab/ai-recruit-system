package com.recruit.airecruitsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching//缓存功能
public class AiRecruitSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiRecruitSystemApplication.class, args);
    }

}
