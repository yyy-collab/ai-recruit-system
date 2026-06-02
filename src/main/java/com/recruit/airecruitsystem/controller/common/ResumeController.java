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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/resume")
public class ResumeController {

    @Autowired
    private ResumeService resumeService;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/upload")
    public Result<ResumeSummaryVO> uploadResume(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        Integer seekerId = jwtUtil.getUserIdFromRequest(request);
        ResumeSummaryVO vo = new ResumeSummaryVO();
        int code = resumeService.uploadResume(seekerId, file, vo);
        return switch (code) {
            case ResultCode.SUCCESS -> Result.success("简历上传成功", vo);
            case ResultCode.INFO_INCOMPLETE -> Result.error(ResultCode.INFO_INCOMPLETE, "请先完善真实姓名、电话和邮箱后再上传简历");
            case ResultCode.RESUME_EXIST -> Result.error(ResultCode.RESUME_EXIST, "已上传过简历，请先删除后再重新上传");
            case ResultCode.FILE_FORMAT_ERROR -> Result.error(ResultCode.FILE_FORMAT_ERROR, "仅支持 PDF / DOC / DOCX 格式");
            case ResultCode.FILE_TOO_LARGE -> Result.error(ResultCode.FILE_TOO_LARGE, "文件大小不能超过 20MB");
            default -> Result.error(ResultCode.PARAM_ERROR, "简历上传失败");
        };
    }

    @GetMapping("/myList")
    public Result<List<ResumeSummaryVO>> getMyResumeList(HttpServletRequest request) {
        Integer seekerId = jwtUtil.getUserIdFromRequest(request);
        return Result.success("操作成功", resumeService.getMyResumeList(seekerId));
    }

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
