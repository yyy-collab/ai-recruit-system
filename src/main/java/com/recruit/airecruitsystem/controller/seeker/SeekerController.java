package com.recruit.airecruitsystem.controller.seeker;


import com.github.pagehelper.PageInfo;
import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.dto.seeker.*;
import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.service.common.InterviewMessageService;
import com.recruit.airecruitsystem.service.seeker.SeekerService;
import com.recruit.airecruitsystem.utils.JwtUtil;
import com.recruit.airecruitsystem.vo.common.PageResult;
import com.recruit.airecruitsystem.vo.common.TokenRefreshVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerInfoVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerLoginVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerMessageDetailVO;
import com.recruit.airecruitsystem.vo.seeker.SeekerMessageListItemVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/seeker")
public class SeekerController {

    @Autowired
    private SeekerService seekerService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private InterviewMessageService interviewMessageService;

    @PostMapping("/register")
    public Result<String> register(@Validated @RequestBody SeekerRegisterRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();

        int code = seekerService.register(username, password);
        if (code == ResultCode.SUCCESS) {
            return Result.success("注册成功，请登录后完善个人信息");
        } else if (code == ResultCode.USERNAME_EXIST) {
            return Result.error(ResultCode.USERNAME_EXIST, "用户名已存在");
        } else {
            return Result.error(ResultCode.PARAM_ERROR, "参数格式错误");
        }
    }

    @PostMapping("/login")
    public Result<SeekerLoginVO> login(@Validated @RequestBody SeekerLoginRequest request) {
        SeekerLoginVO vo=new SeekerLoginVO();

        int code=seekerService.login(request.getUsername(), request.getPassword(),vo);
        if(code==ResultCode.SUCCESS){
            return Result.success("登录成功",vo);
        }else{
            return Result.error(ResultCode.LOGIN_ERROR,"用户名或密码错误");
        }
    }

    @PostMapping("/refreshToken")
    public Result<TokenRefreshVO> refreshToken(@RequestHeader("Authorization") String authorization) {

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(ResultCode.PARAM_ERROR, "无效的Authorization头");
        }
        String oldToken = authorization.substring(7);

        TokenRefreshVO vo = new TokenRefreshVO();
        int code = seekerService.refreshToken(oldToken, vo);
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

    @GetMapping("/userInfo")
    public Result<SeekerInfoVO> getUserInfo(HttpServletRequest request) {
        Integer seekerId;
        try {
            seekerId = jwtUtil.getUserIdFromRequest(request);
        } catch (Exception e) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }
        SeekerInfoVO vo = seekerService.getCurrentUserInfo(seekerId);
        if (vo == null) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        }
        return Result.success("操作成功", vo);
    }

    @PutMapping("/update")
    public Result<String> updateSeekerInfo(@Valid @RequestBody SeekerUpdateRequest request,
                                         HttpServletRequest httpRequest) {
        Integer seekerId;
        try {
            seekerId = jwtUtil.getUserIdFromRequest(httpRequest);
        } catch (Exception e) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }

        int code = seekerService.updateSeekerInfo(seekerId, request);
        if (code == ResultCode.SUCCESS) {
            return Result.success("信息更新成功");
        } else if (code == ResultCode.NOT_FOUND) {
            return Result.error(ResultCode.NOT_FOUND, "用户不存在");
        } else if (code == ResultCode.EMAIL_INVALID) {
            return Result.error(ResultCode.EMAIL_INVALID, "邮箱已被其他用户使用");
        } else {
            return Result.error(ResultCode.PARAM_ERROR, "更新失败，请检查参数");
        }
    }

    @PatchMapping("/updatePwd")
    public Result<String> updatePassword(@Valid @RequestBody SeekerUpdatePwdRequest request,
                                       HttpServletRequest httpRequest,
                                       @RequestHeader("Authorization") String authorization) {
        // 提取 Token
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(ResultCode.PARAM_ERROR, "未提供有效的认证令牌");
        }
        String token = authorization.substring(7);

        // 获取当前用户 ID
        Integer seekerId;
        try {
            seekerId = jwtUtil.getUserIdFromRequest(httpRequest);
        } catch (Exception e) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }

        // 调用 Service
        int code = seekerService.updatePassword(seekerId,
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

    @PostMapping("/logout")
    public Result<String> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(ResultCode.PARAM_ERROR, "未提供有效的认证令牌");
        }
        String token = authorization.substring(7);
        int code = seekerService.logout(token);
        if (code == ResultCode.SUCCESS) {
            return Result.success("登出成功");
        } else {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }
    }

    @DeleteMapping("/delete")
    public Result<String> deleteAccount(@Valid @RequestBody SeekerDeleteRequest request,
                                      @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Result.error(ResultCode.PARAM_ERROR, "未提供有效的认证令牌");
        }
        String token = authorization.substring(7);
        Integer seekerId;
        try {
            seekerId = jwtUtil.getUserId(token);
        } catch (Exception e) {
            return Result.error(ResultCode.TOKEN_EXPIRED, "令牌无效或已过期");
        }

        int code = seekerService.deleteAccount(seekerId, request.getPassword(), token);
        switch (code) {
            case ResultCode.SUCCESS:
                return Result.success("账号注销成功");
            case ResultCode.LOGIN_ERROR:
                return Result.error(ResultCode.LOGIN_ERROR, "密码错误，注销失败");
            case ResultCode.NOT_FOUND:
                return Result.error(ResultCode.NOT_FOUND, "用户不存在");
            default:
                return Result.error(ResultCode.PARAM_ERROR, "注销失败");
        }
    }

    @PostMapping("/resetPwd")
    public Result<String> resetPassword(@Valid @RequestBody SeekerResetPwdRequest request) {
        int code = seekerService.resetPassword(
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
                return Result.error(ResultCode.PARAM_ERROR, "重置失败，请检查参数");
        }
    }


    @GetMapping("/message/list")
    public Result<PageInfo<SeekerMessageListItemVO>> getSeekerMessageList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            HttpServletRequest httpRequest) {
        Integer seekerId = jwtUtil.getUserIdFromRequest(httpRequest);
        PageInfo<SeekerMessageListItemVO> pageInfo = interviewMessageService.getSeekerMessageList(seekerId, status, pageNum, pageSize);
        return Result.success("操作成功", pageInfo);
    }

    @GetMapping("/message/detail")
    public Result<SeekerMessageDetailVO> getSeekerMessageDetail(@RequestParam("message_id") Integer messageId, HttpServletRequest httpRequest) {
        Integer seekerId = jwtUtil.getUserIdFromRequest(httpRequest);
        SeekerMessageDetailVO vo = interviewMessageService.getSeekerMessageDetail(seekerId, messageId);
        return vo == null ? Result.error(ResultCode.INTERVIEW_NOT_EXIST, "面试邀请不存在或无权限") : Result.success("操作成功", vo);
    }

    @PostMapping("/interview/handle")
    public Result<Void> handleInterview(@Valid @RequestBody HandleInterviewRequest request, HttpServletRequest httpRequest) {
        Integer seekerId = jwtUtil.getUserIdFromRequest(httpRequest);
        int code = interviewMessageService.handleInterview(seekerId, request);
        switch (code) {
            case ResultCode.SUCCESS:
                return Result.success("操作成功", null);
            case ResultCode.INTERVIEW_NOT_EXIST:
                return Result.error(ResultCode.INTERVIEW_NOT_EXIST, "面试邀请不存在或无权限");
            case ResultCode.INTERVIEW_ALREADY_HANDLED:
                return Result.error(ResultCode.INTERVIEW_ALREADY_HANDLED, "面试邀请已处理，无法重复操作");
            case ResultCode.REJECT_REASON_EMPTY:
                return Result.error(ResultCode.REJECT_REASON_EMPTY, "拒绝原因不能为空");
            default:
                return Result.error(ResultCode.PARAM_ERROR, "操作失败");
        }
    }
}
