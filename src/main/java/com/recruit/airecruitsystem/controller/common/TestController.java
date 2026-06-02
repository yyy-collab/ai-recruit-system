package com.recruit.airecruitsystem.controller.common;

import com.recruit.airecruitsystem.result.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
public class TestController {

    /**
     * 测试接口：返回 pong，验证服务是否正常
     * 访问路径：GET /test/ping
     */
    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("pong");
    }

    /**
     * 健康检查接口（可选）
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("ok");
    }
}