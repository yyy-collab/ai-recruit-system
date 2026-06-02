package com.recruit.airecruitsystem.controller.hr;


import com.github.pagehelper.PageInfo;
import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.dto.hr.*;
import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.service.common.InterviewMessageService;
import com.recruit.airecruitsystem.service.hr.HrService;
import com.recruit.airecruitsystem.utils.JwtUtil;
import com.recruit.airecruitsystem.utils.UserContext;
import com.recruit.airecruitsystem.vo.common.TokenRefreshVO;
import com.recruit.airecruitsystem.vo.hr.HrInfoVO;
import com.recruit.airecruitsystem.vo.hr.HrLoginVO;
import com.recruit.airecruitsystem.vo.hr.HrMessageDetailVO;
import com.recruit.airecruitsystem.vo.hr.HrMessageListItemVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/hr")
public class HrController {

    @Autowired
    private HrService hrService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private InterviewMessageService interviewMessageService;

    // 注册
    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody HrRegisterRequest request) {
        int code = hrService.register(request.getUsername(), request.getPassword());
        if (code == ResultCode.SUCCESS) {
            return Result.success("注册成功，请登录完善企业信息");
        } else if (code == ResultCode.USERNAME_EXIST) {
            return Result.error(ResultCode.USERNAME_EXIST, "用户名已存在");
        } else {
            return Result.error(ResultCode.PARAM_ERROR, "参数格式错误");
        }
    }

    // 登录
    @PostMapping("/login")
    public Result<HrLoginVO> login(@Valid @RequestBody HrLoginRequest request) {
        HrLoginVO vo = new HrLoginVO();
        int code = hrService.login(request.getUsername(), request.getPassword(), vo);
        if (code == ResultCode.SUCCESS) {
            return Result.success("登录成功", vo);
        } else {
            return Result.error(ResultCode.LOGIN_ERROR, "用户名或密码错误");
        }
    }

    // 刷新Token
    @PostMapping("/refreshToken")
    public Result<TokenRefreshVO> refreshToken(@RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(ResultCode.PARAM_ERROR, "无效的Authorization头");
        }
        String oldToken = authorization.substring(7);
        TokenRefreshVO vo = new TokenRefreshVO();
        int code = hrService.refreshToken(oldToken, vo);
        if (code == ResultCode.SUCCESS) {
            return Result.success("刷新成功", vo);
        } else if (code == ResultCode.TOKEN_EXPIRED) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌已过期，请重新登录");
        } else if (code == ResultCode.TOO_FREQUENT) {
            return Result.error(ResultCode.TOO_FREQUENT, "24小时内刷新次数已达上限");
        } else {
            return Result.error(ResultCode.PARAM_ERROR, "刷新失败");
        }
    }

    // 获取当前HR信息
    @GetMapping("/userInfo")
    public Result<HrInfoVO> getUserInfo() {
        Integer hrId = UserContext.getUserId();
        if (hrId == null) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }
        HrInfoVO vo = hrService.getCurrentHrInfo(hrId);
        if (vo == null) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        return Result.success("操作成功", vo);
    }

    // 更新HR信息
    @PutMapping("/update")
    public Result<String> updateHrInfo(@Valid @RequestBody HrUpdateRequest request) {
        Integer hrId = UserContext.getUserId();
        if (hrId == null) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }
        int code = hrService.updateHrInfo(hrId, request);
        if (code == ResultCode.SUCCESS) {
            return Result.success("信息更新成功");
        } else if (code == ResultCode.EMAIL_INVALID) {
            return Result.error(ResultCode.EMAIL_INVALID, "邮箱已被其他用户使用");
        } else {
            return Result.error(ResultCode.PARAM_ERROR, "更新失败");
        }
    }

    // 修改密码
    @PatchMapping("/updatePwd")
    public Result<String> updatePassword(@Valid @RequestBody HrUpdatePwdRequest request,
                                         @RequestHeader("Authorization") String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(ResultCode.PARAM_ERROR, "未提供有效的认证令牌");
        }
        String token = authorization.substring(7);
        Integer hrId = UserContext.getUserId();
        if (hrId == null) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }

        int code = hrService.updatePassword(hrId,
                request.getOldPwd(),
                request.getNewPwd(),
                request.getRePwd(),
                token);

        switch (code) {
            case ResultCode.SUCCESS:
                return Result.success("密码修改成功，请重新登录");
            case ResultCode.NEED_RELOGIN:
                return Result.error(ResultCode.NEED_RELOGIN, "密码修改成功，请重新登录");
            case ResultCode.OLD_PWD_ERROR:
                return Result.error(ResultCode.OLD_PWD_ERROR, "原密码错误");
            case ResultCode.PWD_NOT_MATCH:
                return Result.error(ResultCode.PWD_NOT_MATCH, "两次输入密码不一致");
            default:
                return Result.error(ResultCode.PARAM_ERROR, "密码修改失败");
        }
    }

    // 登出
    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(ResultCode.PARAM_ERROR, "未提供有效的认证令牌");
        }
        String token = authorization.substring(7);
        int code = hrService.logout(token);
        if (code == ResultCode.SUCCESS) {
            return Result.success("登出成功");
        } else {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }
    }

    // 重置密码（未登录）
    @PostMapping("/resetPwd")
    public Result<String> resetPassword(@Valid @RequestBody HrResetPwdRequest request) {
        int code = hrService.resetPassword(
                request.getUsername(),
                request.getEmail(),
                request.getCode(),
                request.getNewPwd(),
                request.getRePwd()
        );
        switch (code) {
            case ResultCode.SUCCESS:
                return Result.success("密码重置成功，请重新登录");
            case ResultCode.PWD_NOT_MATCH:
                return Result.error(ResultCode.PWD_NOT_MATCH, "两次输入密码不一致");
            case ResultCode.EMAIL_NOT_REGISTERED:
                return Result.error(ResultCode.EMAIL_NOT_REGISTERED, "邮箱未注册或验证码错误/过期");
            default:
                return Result.error(ResultCode.PARAM_ERROR, "重置失败");
        }
    }

    // HR 注销账号
    @DeleteMapping("/delete")
    public Result<String> deleteAccount(@Valid @RequestBody HrDeleteRequest request,
                                        @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(ResultCode.PARAM_ERROR, "未提供有效的认证令牌");
        }
        String token = authorization.substring(7);
        Integer hrId = UserContext.getUserId();
        if (hrId == null) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }

        int code = hrService.deleteAccount(hrId, request.getPassword(), token);
        switch (code) {
            case ResultCode.SUCCESS:
                return Result.success("账号注销成功");
            case ResultCode.LOGIN_ERROR:
                return Result.error(ResultCode.LOGIN_ERROR, "密码错误，注销失败");
            case ResultCode.NOT_FOUND:
                return Result.error(ResultCode.NOT_FOUND, "用户不存在");
            case ResultCode.HR_HAS_ONLINE_JOBS:
                return Result.error(ResultCode.HR_HAS_ONLINE_JOBS, "请先下线所有岗位后再注销账号");
            default:
                return Result.error(ResultCode.PARAM_ERROR, "注销失败");
        }
    }

    // 发送面试邀请
    @PostMapping("/interview/send")
    public Result<Void> sendInterview(@Valid @RequestBody SendInterviewRequest request) {
        Integer hrId = UserContext.getUserId();
        if (hrId == null) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }
        int code = interviewMessageService.sendInterview(hrId, request);
        return code == ResultCode.SUCCESS ? Result.success("面试邀请发送成功", null) : Result.error(ResultCode.PARAM_ERROR, "发送失败");
    }

    // HR消息列表
    @GetMapping("/message/list")
    public Result<PageInfo<HrMessageListItemVO>> getHrMessageList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Integer hrId = UserContext.getUserId();
        if (hrId == null) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }
        PageInfo<HrMessageListItemVO> pageInfo = interviewMessageService.getHrMessageList(hrId, status, pageNum, pageSize);
        return Result.success("操作成功", pageInfo);
    }

    @GetMapping("/message/detail")
    public Result<HrMessageDetailVO> getHrMessageDetail(@RequestParam("message_id") Integer messageId) {
        Integer hrId = UserContext.getUserId();
        if (hrId == null) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }
        HrMessageDetailVO vo = interviewMessageService.getHrMessageDetail(hrId, messageId);
        return vo == null ? Result.error(ResultCode.INTERVIEW_NOT_EXIST, "面试邀请不存在或无权限") : Result.success("操作成功", vo);
    }
}