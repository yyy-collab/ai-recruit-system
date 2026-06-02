package com.recruit.airecruitsystem.service.resume;

import com.recruit.airecruitsystem.vo.hr.HrResumeDetailVO;
import com.recruit.airecruitsystem.vo.resume.ResumeAiDetailVO;
import com.recruit.airecruitsystem.vo.resume.ResumePreviewVO;
import com.recruit.airecruitsystem.vo.resume.ResumeSummaryVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {
    int uploadResume(Integer seekerId, MultipartFile file, ResumeSummaryVO vo);

    List<ResumeSummaryVO> getMyResumeList(Integer seekerId);

    int getResumeAiDetail(Integer seekerId, Integer resumeId, ResumeAiDetailVO vo);

    int previewResume(Integer seekerId, Integer resumeId, ResumePreviewVO vo);

    int deleteResume(Integer seekerId, Integer resumeId);

    int reparseResume(Integer seekerId, Integer resumeId, ResumeSummaryVO vo);

    HrResumeDetailVO getHrDeliveryDetail(Integer hrId, Integer deliveryId);
}
