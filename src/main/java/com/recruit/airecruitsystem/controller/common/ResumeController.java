package com.recruit.airecruitsystem.controller.common;

import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.dto.resume.ResumeReparseRequest;
import com.recruit.airecruitsystem.result.Result;
import com.recruit.airecruitsystem.service.resume.ResumeService;
import com.recruit.airecruitsystem.utils.JwtUtil;
import com.recruit.airecruitsystem.vo.resume.ResumeAiDetailVO;
import com.recruit.airecruitsystem.vo.resume.ResumePreviewVO;
import com.recruit.airecruitsystem.vo.resume.ResumeSummaryVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 简历模块前端求职者接口控制器
 * 提供求职者简历上传、列表查询、AI解析详情、预览、删除、重新解析接口
 * 统一通过JWT从请求头获取当前登录求职者ID，校验操作归属权限
 */
@RestController
@RequestMapping("/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 简历上传接口
     */
    @PostMapping("/upload")
    public Result<ResumeSummaryVO> uploadResume(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        // 从token中获取当前登录求职者ID
        Integer seekerId = jwtUtil.getUserIdFromRequest(request);
        ResumeSummaryVO vo = new ResumeSummaryVO();
        // 调用业务层执行上传逻辑
        int code = resumeService.uploadResume(seekerId, file, vo);
        // 根据业务返回码封装统一响应信息
        return switch (code) {
            case ResultCode.SUCCESS -> Result.success("简历上传成功", vo);
            case ResultCode.INFO_INCOMPLETE -> Result.error(ResultCode.INFO_INCOMPLETE, "请先完善真实姓名、电话和邮箱后再上传简历");
            case ResultCode.RESUME_UPDATE_BLOCKED -> Result.error(ResultCode.RESUME_UPDATE_BLOCKED, "当前还有待处理投递，需等所有投递记录都被处理后才能更新简历");
            case ResultCode.FILE_FORMAT_ERROR -> Result.error(ResultCode.FILE_FORMAT_ERROR, "仅支持 DOC / DOCX 格式");
            case ResultCode.FILE_TOO_LARGE -> Result.error(ResultCode.FILE_TOO_LARGE, "文件大小不能超过 20MB");
            default -> Result.error(ResultCode.PARAM_ERROR, "简历上传失败");
        };
    }

    /**
     * 查询当前求职者本人全部简历列表
     */
    @GetMapping("/myList")
    public Result<List<ResumeSummaryVO>> getMyResumeList(HttpServletRequest request) {
        Integer seekerId = jwtUtil.getUserIdFromRequest(request);
        List<ResumeSummaryVO> resumeList = resumeService.getMyResumeList(seekerId);
        return Result.success("操作成功", resumeList);
    }

    /**
     * 获取简历AI结构化解析详情接口
     * 前端智能简历中心页面数据源，用于渲染简历质量评分、技能雷达图
     */
    @GetMapping("/ai/detail")
    public Result<ResumeAiDetailVO> getResumeAiDetail(@RequestParam("resume_id") Integer resumeId,
                                                      HttpServletRequest request) {
        Integer seekerId = jwtUtil.getUserIdFromRequest(request);
        ResumeAiDetailVO vo = new ResumeAiDetailVO();
        int code = resumeService.getResumeAiDetail(seekerId, resumeId, vo);
        return switch (code) {
            case ResultCode.SUCCESS -> Result.success("操作成功", vo);
            case ResultCode.RESUME_PARSING -> Result.error(ResultCode.RESUME_PARSING, "简历正在解析中，请稍后再试");
            case ResultCode.RESUME_PARSE_FAIL -> Result.error(ResultCode.RESUME_PARSE_FAIL, "简历解析失败，请重新上传或重新解析");
            case ResultCode.NOT_FOUND -> Result.error(ResultCode.NOT_FOUND, "简历不存在");
            default -> Result.error(ResultCode.PARAM_ERROR, "获取简历分析详情失败");
        };
    }

    /**
     * 原简历文件预览接口
     * 返回Word原文提取的纯文本、可嵌入iframe的HTML预览页面
     */
    @GetMapping("/preview")
    public Result<ResumePreviewVO> previewResume(@RequestParam("resume_id") Integer resumeId,
                                                 HttpServletRequest request) {
        Integer seekerId = jwtUtil.getUserIdFromRequest(request);
        ResumePreviewVO vo = new ResumePreviewVO();
        int code = resumeService.previewResume(seekerId, resumeId, vo);
        return code == ResultCode.SUCCESS
                ? Result.success("操作成功", vo)
                : Result.error(code, code == ResultCode.NOT_FOUND ? "简历不存在" : "获取简历预览失败");
    }

    /**
     * 删除简历接口
     * 限制：已存在投递记录的简历不可删除，同时删除本地文件、解析记录、简历主记录
     */
    @DeleteMapping("/delete")
    public Result<Void> deleteResume(@RequestParam("resume_id") Integer resumeId, HttpServletRequest request) {
        Integer seekerId = jwtUtil.getUserIdFromRequest(request);
        int code = resumeService.deleteResume(seekerId, resumeId);
        return switch (code) {
            case ResultCode.SUCCESS -> Result.success("简历删除成功", null);
            case ResultCode.RESUME_HAS_DELIVERY -> Result.error(ResultCode.RESUME_HAS_DELIVERY, "该简历已存在投递记录，无法删除");
            case ResultCode.NOT_FOUND -> Result.error(ResultCode.NOT_FOUND, "简历不存在");
            default -> Result.error(ResultCode.PARAM_ERROR, "删除简历失败");
        };
    }

    /**
     * 简历重新解析接口
     */
    @PostMapping("/ai/reparse")
    public Result<ResumeSummaryVO> reparseResume(@Valid @RequestBody ResumeReparseRequest request,
                                                 HttpServletRequest httpRequest) {
        Integer seekerId = jwtUtil.getUserIdFromRequest(httpRequest);
        ResumeSummaryVO vo = new ResumeSummaryVO();
        int code = resumeService.reparseResume(seekerId, request.getResumeId(), vo);
        return switch (code) {
            case ResultCode.SUCCESS -> Result.success("简历重新解析成功", vo);
            case ResultCode.NOT_FOUND -> Result.error(ResultCode.NOT_FOUND, "简历不存在");
            case ResultCode.RESUME_PARSING -> Result.error(ResultCode.RESUME_PARSING, "简历正在解析中，请稍后再试");
            default -> Result.error(ResultCode.PARAM_ERROR, "重新解析失败");
        };
    }
}