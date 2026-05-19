package com.recruit.airecruitsystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ResourceConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir}")   // 读取配置的上传目录
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将 /uploads/** 的请求映射到本地文件目录，实现通过URL访问上传的文件
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir);
    }
}