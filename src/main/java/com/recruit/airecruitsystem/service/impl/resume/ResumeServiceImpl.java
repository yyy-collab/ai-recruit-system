package com.recruit.airecruitsystem.service.impl.resume;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.mapper.DeliveryMapper;
import com.recruit.airecruitsystem.mapper.JobMapper;
import com.recruit.airecruitsystem.mapper.ResumeMapper;
import com.recruit.airecruitsystem.mapper.ResumeParseResultMapper;
import com.recruit.airecruitsystem.mapper.SeekerMapper;
import com.recruit.airecruitsystem.model.ResumeAnalysisSnapshot;
import com.recruit.airecruitsystem.pojo.Delivery;
import com.recruit.airecruitsystem.pojo.Job;
import com.recruit.airecruitsystem.pojo.Resume;
import com.recruit.airecruitsystem.pojo.ResumeParseResult;
import com.recruit.airecruitsystem.pojo.Seeker;
import com.recruit.airecruitsystem.service.resume.ResumeDocumentParser;
import com.recruit.airecruitsystem.service.resume.ResumeService;
import com.recruit.airecruitsystem.utils.UserContext;
import com.recruit.airecruitsystem.vo.hr.HrResumeDetailVO;
import com.recruit.airecruitsystem.vo.resume.ResumeAiDetailVO;
import com.recruit.airecruitsystem.vo.resume.ResumePreviewVO;
import com.recruit.airecruitsystem.vo.resume.ResumeSummaryVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ResumeServiceImpl implements ResumeService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("doc", "docx");
    private static final long MAX_RESUME_SIZE = 20L * 1024 * 1024;

    @Autowired
    private ResumeMapper resumeMapper;

    @Autowired
    private ResumeParseResultMapper resumeParseResultMapper;

    @Autowired
    private SeekerMapper seekerMapper;

    @Autowired
    private DeliveryMapper deliveryMapper;

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResumeDocumentParser resumeDocumentParser;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.access-path}")
    private String accessPath;

    @Override
    public int uploadResume(Integer seekerId, MultipartFile file, ResumeSummaryVO vo) {
        if (!"seeker".equals(UserContext.getRole()) || seekerId == null) {
            return ResultCode.PARAM_ERROR;
        }
        Seeker seeker = seekerMapper.findById(seekerId);
        if (seeker == null) {
            return ResultCode.NOT_FOUND;
        }
        if (!isProfileComplete(seeker)) {
            return ResultCode.INFO_INCOMPLETE;
        }
        if (file == null || file.isEmpty()) {
            return ResultCode.PARAM_ERROR;
        }

        Resume currentResume = resumeMapper.selectCurrentBySeekerId(seekerId);
        boolean replacingResume = currentResume != null;
        if (replacingResume && deliveryMapper.countPendingBySeekerId(seekerId) > 0) {
            return ResultCode.RESUME_UPDATE_BLOCKED;
        }

        String extension = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResultCode.FILE_FORMAT_ERROR;
        }
        if (file.getSize() > MAX_RESUME_SIZE) {
            return ResultCode.FILE_TOO_LARGE;
        }

        try {
            StoredResumeFile storedFile = storeResumeFile(seekerId, file, extension);
            String fileName = resolveOriginalFileName(file, extension);
            return replacingResume
                    ? replaceResume(currentResume, storedFile, fileName, extension, vo)
                    : createResume(seekerId, storedFile, fileName, extension, vo);
        } catch (IOException e) {
            return ResultCode.PARAM_ERROR;
        }
    }

    @Override
    public List<ResumeSummaryVO> getMyResumeList(Integer seekerId) {
        List<ResumeSummaryVO> list = new ArrayList<>();
        if (seekerId == null) {
            return list;
        }
        Resume resume = resumeMapper.selectCurrentBySeekerId(seekerId);
        if (resume != null) {
            ResumeSummaryVO vo = new ResumeSummaryVO();
            copySummary(resume, vo);
            list.add(vo);
        }
        return list;
    }

    @Override
    public int getResumeAiDetail(Integer seekerId, Integer resumeId, ResumeAiDetailVO vo) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (!ownsResume(resume, seekerId)) {
            return ResultCode.NOT_FOUND;
        }
        if (resume.getIsParsed() != null && resume.getIsParsed() == 2) {
            return ResultCode.RESUME_PARSING;
        }
        if (resume.getIsParsed() != null && resume.getIsParsed() == 3) {
            return ResultCode.RESUME_PARSE_FAIL;
        }

        ResumeAnalysisSnapshot snapshot = buildSnapshot(resume);
        vo.setResumeId(resume.getId());
        vo.setResumeFileName(resume.getFileName());
        vo.setResumeFileUrl(resume.getFileUrl());
        vo.setIsParsed(resume.getIsParsed());
        vo.setPreviewText(buildPreviewText(snapshot));
        vo.setAnalysis(snapshot);
        vo.setCreateTime(resume.getCreateTime());
        vo.setUpdateTime(resume.getUpdateTime());
        return ResultCode.SUCCESS;
    }

    @Override
    public int previewResume(Integer seekerId, Integer resumeId, ResumePreviewVO vo) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (!ownsResume(resume, seekerId)) {
            return ResultCode.NOT_FOUND;
        }
        vo.setResumeId(resume.getId());
        vo.setResumeFileName(resume.getFileName());
        vo.setPreviewText(buildOriginalPreviewText(resume));
        return ResultCode.SUCCESS;
    }

    @Override
    public int deleteResume(Integer seekerId, Integer resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (!ownsResume(resume, seekerId)) {
            return ResultCode.NOT_FOUND;
        }
        if (resumeMapper.countDeliveriesUsingResume(resumeId) > 0) {
            return ResultCode.RESUME_HAS_DELIVERY;
        }
        deleteStoredFile(resume.getFileUrl());
        resumeParseResultMapper.deleteByResumeId(resumeId);
        resumeMapper.deleteById(resumeId);
        return ResultCode.SUCCESS;
    }

    @Override
    public int reparseResume(Integer seekerId, Integer resumeId, ResumeSummaryVO vo) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (!ownsResume(resume, seekerId)) {
            return ResultCode.NOT_FOUND;
        }
        if (resume.getIsParsed() != null && resume.getIsParsed() == 2) {
            return ResultCode.RESUME_PARSING;
        }

        try {
            resumeMapper.updateParseStatus(resumeId, 2, null);
            String extension = resolveExtension(resume.getFileName());
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                extension = resolveExtension(resume.getFileUrl());
            }
            parseAndPersist(resume, resolveStoredFilePath(resume.getFileUrl()), extension);
            resumeMapper.updateParseStatus(resumeId, 1, null);

            Resume latest = resumeMapper.selectById(resumeId);
            copySummary(latest, vo);
            return ResultCode.SUCCESS;
        } catch (IOException e) {
            resumeMapper.updateParseStatus(resumeId, 3, trimMessage(e.getMessage()));
            return ResultCode.PARAM_ERROR;
        }
    }

    @Override
    public HrResumeDetailVO getHrDeliveryDetail(Integer hrId, Integer deliveryId) {
        Delivery delivery = deliveryMapper.selectById(deliveryId);
        if (delivery == null) {
            return null;
        }

        Job job = jobMapper.selectById(delivery.getJobId());
        if (job == null || hrId == null || !hrId.equals(job.getHrId())) {
            return null;
        }

        Resume resume = resumeMapper.selectById(delivery.getResumeId());
        Seeker seeker = seekerMapper.findById(delivery.getSeekerId());
        if (resume == null || seeker == null) {
            return null;
        }

        ResumeAnalysisSnapshot snapshot = buildSnapshot(resume);

        HrResumeDetailVO.CandidateProfile seekerInfo = new HrResumeDetailVO.CandidateProfile();
        seekerInfo.setId(seeker.getId());
        seekerInfo.setRealName(seeker.getRealName());
        seekerInfo.setTargetTitle(seeker.getExPosition());
        seekerInfo.setAvatarUrl(seeker.getAvatarUrl());
        seekerInfo.setPhone(seeker.getPhone());
        seekerInfo.setEmail(seeker.getEmail());
        seekerInfo.setAge(seeker.getAge());
        seekerInfo.setEduBack(seeker.getEduBack());
        seekerInfo.setAlmaMater(seeker.getAlmaMater());
        seekerInfo.setCity(seeker.getExCity());

        HrResumeDetailVO.ResumeInfo resumeInfo = new HrResumeDetailVO.ResumeInfo();
        resumeInfo.setResumeId(resume.getId());
        resumeInfo.setResumeFileName(resume.getFileName());
        resumeInfo.setResumeFileUrl(resume.getFileUrl());
        resumeInfo.setWorkExperience(snapshot.getWorkExperience());
        resumeInfo.setSkills(snapshot.getSkills());
        resumeInfo.setWorkHistory(snapshot.getWorkHistory());
        resumeInfo.setPreviewText(buildOriginalPreviewText(resume));

        HrResumeDetailVO vo = new HrResumeDetailVO();
        vo.setDeliveryId(delivery.getId());
        vo.setJobId(job.getId());
        vo.setJobName(job.getJobName());
        vo.setSeekerInfo(seekerInfo);
        vo.setResumeInfo(resumeInfo);
        vo.setStatus(delivery.getStatus());
        vo.setDeliveryTime(delivery.getDeliveryTime());
        vo.setUpdateTime(delivery.getUpdateTime());
        return vo;
    }

    private void parseAndPersist(Resume resume, Path filePath, String extension) throws IOException {
        ResumeAnalysisSnapshot snapshot = resumeDocumentParser.parse(filePath, extension, resume.getFileName());
        persistSnapshot(resume.getId(), snapshot);
    }

    private int createResume(Integer seekerId,
                             StoredResumeFile storedFile,
                             String fileName,
                             String extension,
                             ResumeSummaryVO vo) {
        Resume resume = null;
        try {
            resume = Resume.builder()
                    .seekerId(seekerId)
                    .fileName(fileName)
                    .fileUrl(storedFile.fileUrl())
                    .isParsed(2)
                    .build();
            resumeMapper.insert(resume);
            parseAndPersist(resume, storedFile.path(), extension);
            resumeMapper.updateParseStatus(resume.getId(), 1, null);

            Resume latest = resumeMapper.selectById(resume.getId());
            copySummary(latest, vo);
            return ResultCode.SUCCESS;
        } catch (IOException e) {
            if (resume != null && resume.getId() != null) {
                resumeMapper.updateParseStatus(resume.getId(), 3, trimMessage(e.getMessage()));
            }
            return ResultCode.PARAM_ERROR;
        }
    }

    private int replaceResume(Resume currentResume,
                              StoredResumeFile storedFile,
                              String fileName,
                              String extension,
                              ResumeSummaryVO vo) {
        String oldFileName = currentResume.getFileName();
        String oldFileUrl = currentResume.getFileUrl();
        Integer oldIsParsed = currentResume.getIsParsed();
        String oldParseFailReason = currentResume.getParseFailReason();

        currentResume.setFileName(fileName);
        currentResume.setFileUrl(storedFile.fileUrl());
        currentResume.setIsParsed(2);
        currentResume.setParseFailReason(null);

        try {
            // Single-resume mode: overwrite the existing row instead of creating a versioned record.
            resumeMapper.updateFileInfo(currentResume);
            parseAndPersist(currentResume, storedFile.path(), extension);
            resumeMapper.updateParseStatus(currentResume.getId(), 1, null);

            deleteStoredFile(oldFileUrl);
            Resume latest = resumeMapper.selectById(currentResume.getId());
            copySummary(latest, vo);
            return ResultCode.SUCCESS;
        } catch (IOException e) {
            currentResume.setFileName(oldFileName);
            currentResume.setFileUrl(oldFileUrl);
            currentResume.setIsParsed(oldIsParsed);
            currentResume.setParseFailReason(oldParseFailReason);
            resumeMapper.updateFileInfo(currentResume);
            deleteStoredFile(storedFile.fileUrl());
            return ResultCode.PARAM_ERROR;
        }
    }

    private void persistSnapshot(Integer resumeId, ResumeAnalysisSnapshot snapshot) throws IOException {
        ResumeParseResult record = ResumeParseResult.builder()
                .resumeId(resumeId)
                .basicInfo(objectMapper.writeValueAsString(snapshot.getBasicInfo()))
                .workExperience(snapshot.getWorkExperience())
                .skills(objectMapper.writeValueAsString(nullToEmptyList(snapshot.getSkills())))
                .workHistory(objectMapper.writeValueAsString(nullToEmptyList(snapshot.getWorkHistory())))
                .build();
        if (resumeParseResultMapper.selectByResumeId(resumeId) == null) {
            resumeParseResultMapper.insert(record);
        } else {
            resumeParseResultMapper.updateByResumeId(record);
        }
    }

    private ResumeAnalysisSnapshot buildSnapshot(Resume resume) {
        ResumeParseResult result = resumeParseResultMapper.selectByResumeId(resume.getId());
        if (result == null) {
            return decorateSnapshot(new ResumeAnalysisSnapshot());
        }

        ResumeAnalysisSnapshot snapshot = ResumeAnalysisSnapshot.builder()
                .basicInfo(readJson(result.getBasicInfo(), ResumeAnalysisSnapshot.BasicInfo.class, null))
                .workExperience(result.getWorkExperience())
                .skills(readJson(result.getSkills(), new TypeReference<List<String>>() {}, Collections.emptyList()))
                .workHistory(readJson(result.getWorkHistory(), new TypeReference<List<ResumeAnalysisSnapshot.WorkHistoryItem>>() {}, Collections.emptyList()))
                .build();
        return decorateSnapshot(snapshot);
    }

    private ResumeAnalysisSnapshot decorateSnapshot(ResumeAnalysisSnapshot snapshot) {
        ResumeAnalysisSnapshot target = snapshot == null ? new ResumeAnalysisSnapshot() : snapshot;
        if (target.getBasicInfo() == null) {
            target.setBasicInfo(new ResumeAnalysisSnapshot.BasicInfo());
        }
        if (target.getSkills() == null) {
            target.setSkills(Collections.emptyList());
        }
        if (target.getWorkHistory() == null) {
            target.setWorkHistory(Collections.emptyList());
        }
        return target;
    }

    private <T> T readJson(String json, Class<T> type, T fallback) {
        if (!StringUtils.hasText(json)) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            return fallback;
        }
    }

    private <T> T readJson(String json, TypeReference<T> type, T fallback) {
        if (!StringUtils.hasText(json)) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (IOException e) {
            return fallback;
        }
    }

    private StoredResumeFile storeResumeFile(Integer seekerId, MultipartFile file, String extension) throws IOException {
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path targetDir = Paths.get(uploadPath, "resume", String.valueOf(seekerId), dateDir);
        Files.createDirectories(targetDir);

        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetFile = targetDir.resolve(storedName);
        file.transferTo(targetFile.toFile());

        String normalizedAccessPath = accessPath.endsWith("/") ? accessPath : accessPath + "/";
        String fileUrl = normalizedAccessPath + "resume/" + seekerId + "/" + dateDir.replace("\\", "/") + "/" + storedName;
        return new StoredResumeFile(targetFile, fileUrl);
    }

    private Path resolveStoredFilePath(String fileUrl) throws IOException {
        if (!StringUtils.hasText(fileUrl)) {
            throw new IOException("简历文件地址为空");
        }

        String normalizedAccessPath = accessPath.endsWith("/") ? accessPath : accessPath + "/";
        if (!fileUrl.startsWith(normalizedAccessPath)) {
            throw new IOException("简历文件不在本地上传目录");
        }

        String relative = fileUrl.substring(normalizedAccessPath.length());
        Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path fullPath = uploadRoot.resolve(relative.replace("/", java.io.File.separator)).normalize();
        if (!fullPath.startsWith(uploadRoot)) {
            throw new IOException("简历文件路径非法");
        }
        if (!Files.exists(fullPath)) {
            throw new IOException("简历文件不存在");
        }
        return fullPath;
    }

    private void deleteStoredFile(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }
        try {
            Path fullPath = resolveStoredFilePath(fileUrl);
            Files.deleteIfExists(fullPath);
        } catch (IOException ignored) {
        }
    }

    private boolean ownsResume(Resume resume, Integer seekerId) {
        return resume != null && seekerId != null && seekerId.equals(resume.getSeekerId());
    }

    private boolean isProfileComplete(Seeker seeker) {
        return StringUtils.hasText(seeker.getRealName())
                && StringUtils.hasText(seeker.getPhone())
                && StringUtils.hasText(seeker.getEmail());
    }

    private String resolveOriginalFileName(MultipartFile file, String extension) {
        return StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "resume." + extension;
    }

    private String resolveExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private void copySummary(Resume resume, ResumeSummaryVO vo) {
        vo.setResumeId(resume.getId());
        vo.setResumeFileName(resume.getFileName());
        vo.setResumeFileUrl(resume.getFileUrl());
        vo.setIsParsed(resume.getIsParsed());
        vo.setCreateTime(resume.getCreateTime());
        vo.setUpdateTime(resume.getUpdateTime());
    }

    private String buildOriginalPreviewText(Resume resume) {
        try {
            String extension = resolveExtension(resume.getFileName());
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                extension = resolveExtension(resume.getFileUrl());
            }
            Path filePath = resolveStoredFilePath(resume.getFileUrl());
            return resumeDocumentParser.extractPreviewText(filePath, extension);
        } catch (IOException e) {
            return buildPreviewText(buildSnapshot(resume));
        }
    }

    private String buildPreviewText(ResumeAnalysisSnapshot snapshot) {
        ResumeAnalysisSnapshot.BasicInfo info = snapshot.getBasicInfo();
        StringBuilder builder = new StringBuilder();
        builder.append("姓名：").append(nullToEmpty(info.getRealName())).append("\n");
        builder.append("电话：").append(nullToEmpty(info.getPhone())).append("    ");
        builder.append("邮箱：").append(nullToEmpty(info.getEmail())).append("\n");
        builder.append("年龄：").append(info.getAge() == null ? "" : info.getAge()).append("\n");
        builder.append("学历：").append(nullToEmpty(info.getEduBack())).append(" / ")
                .append(nullToEmpty(info.getAlmaMater())).append("\n");
        builder.append("工作年限：").append(nullToEmpty(snapshot.getWorkExperience())).append("\n\n");
        builder.append("技能：").append(String.join(" / ", snapshot.getSkills())).append("\n\n");
        builder.append("工作经历：\n");

        int index = 1;
        for (ResumeAnalysisSnapshot.WorkHistoryItem item : snapshot.getWorkHistory()) {
            builder.append(index++).append(". ")
                    .append(nullToEmpty(item.getCompany())).append(" / ")
                    .append(nullToEmpty(item.getPosition())).append(" / ")
                    .append(nullToEmpty(item.getStartTime())).append(" - ")
                    .append(nullToEmpty(item.getEndTime())).append("\n   ")
                    .append(nullToEmpty(item.getDescription())).append("\n");
        }
        return builder.toString();
    }

    private String trimMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "简历解析失败";
        }
        return message.length() > 255 ? message.substring(0, 255) : message;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private <T> List<T> nullToEmptyList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    private record StoredResumeFile(Path path, String fileUrl) {
    }
}
