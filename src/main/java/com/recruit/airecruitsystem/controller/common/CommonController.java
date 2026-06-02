package com.recruit.airecruitsystem.controller.common;


import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.service.common.EmailService;
import com.recruit.airecruitsystem.service.common.FileUploadService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/common")
@Validated
public class CommonController {

    @Autowired
    private EmailService emailService;

    @Autowired
    private FileUploadService fileUploadService;

    /**
     * 发送邮箱验证码（重置密码用）
     * @param request 包含 email 和 type
     * @return Result
     */
    @PostMapping("/verify/code")
    public Result<String> sendVerifyCode(@RequestBody VerifyCodeRequest request) {
        // 简单校验邮箱格式（前端已经校验，后端再保一次）
        if (request.getEmail() == null || !request.getEmail().matches("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$")) {
            return Result.error(ResultCode.PARAM_ERROR, "邮箱格式不正确");
        }
        if (!"seeker_reset".equals(request.getType()) && !"hr_reset".equals(request.getType())) {
            return Result.error(ResultCode.PARAM_ERROR, "无效的验证码类型");
        }

        int code = emailService.sendVerificationCode(request.getEmail(), request.getType());
        if (code == ResultCode.SUCCESS) {
            return Result.success("验证码已发送至邮箱，5分钟内有效");
        } else {
            return Result.error(ResultCode.PARAM_ERROR, "验证码发送失败，请稍后重试");
        }
    }

    @Data
    static class VerifyCodeRequest {
        @NotBlank(message = "邮箱不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$", message = "邮箱格式错误")
        private String email;

        @NotBlank(message = "类型不能为空")
        private String type;   // seeker_reset 或 hr_reset
    }

    /**
     * 公共文件上传接口（头像）
     * @param file 上传的文件
     * @param type 文件类型（avatar）
     * @return 文件URL
     */
    @PostMapping("/file/upload")
    public Result<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("type") String type) {
        try {
            String fileUrl = fileUploadService.uploadFile(file, type);
            Map<String, String> data = new HashMap<>();
            data.put("file_url", fileUrl);
            data.put("file_name", file.getOriginalFilename());
            return Result.success("文件上传成功", data);
        } catch (IllegalArgumentException e) {
            // 参数/格式/大小错误
            String msg = e.getMessage();
            if (msg.contains("5MB")) {
                return Result.error(ResultCode.FILE_TOO_LARGE, msg);
            } else if (msg.contains("格式")) {
                return Result.error(ResultCode.FILE_FORMAT_ERROR, msg);
            } else {
                return Result.error(ResultCode.PARAM_ERROR, msg);
            }
        } catch (Exception e) {
            return Result.error(ResultCode.PARAM_ERROR, "文件上传失败：" + e.getMessage());
        }
    }
}
