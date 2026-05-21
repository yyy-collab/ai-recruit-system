package com.recruit.airecruitsystem.controller.seeker;


import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.service.seeker.SeekerService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/seeker")
public class SeekerController {

    @Autowired
    private SeekerService seekerService;

    @PostMapping("/register")
    public Result<String> register(@Validated @RequestBody RegisterRequest registerReq) {
        String username = registerReq.getUsername();
        String password = registerReq.getPassword();

        int code = seekerService.register(username, password);
        if (code == ResultCode.SUCCESS) {
            return Result.success("注册成功，请登录后完善个人信息");
        } else if (code == ResultCode.USERNAME_EXIST) {
            return Result.error(ResultCode.USERNAME_EXIST, "用户名已存在");
        } else {
            return Result.error(ResultCode.PARAM_ERROR, "参数格式错误");
        }
    }

    // 内部类接收请求参数
    @Data
    static class RegisterRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }
}
