package com.recruit.airecruitsystem.config;

import com.recruit.airecruitsystem.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration   // 配置类，Spring会加载其中的配置
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor; // 注入自定义拦截器

    // 跨域配置：允许前端不同端口（如5173）访问后端
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                     // 所有路径
                .allowedOriginPatterns("*")           // 允许所有来源（生产环境应指定具体域名）
                .allowedMethods("*")                  // 允许所有HTTP方法
                .allowedHeaders("*")                  // 允许所有请求头
                .allowCredentials(true);              // 允许携带cookie
    }

    // 注册拦截器，指定哪些路径需要拦截，哪些放行
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")                // 拦截所有请求
                .excludePathPatterns(                  // 以下路径不拦截（无需登录）
                        "/seeker/register",
                        "/seeker/login",
                        "/hr/register",
                        "/hr/login",
                        "/common/verify/code",
                        "/seeker/resetPwd",
                        "/hr/resetPwd",
                        "/test/**"
                );
    }
}