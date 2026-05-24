package com.recruit.airecruitsystem.config;

import com.recruit.airecruitsystem.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    private AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/seeker/**", "/hr/**", "/resume/**", "/ai/**")
                .excludePathPatterns(
                        "/seeker/register", "/seeker/login", "/seeker/resetPwd",
                        "/hr/register", "/hr/login", "/hr/resetPwd",
                        "/common/**"
                );
    }

}
