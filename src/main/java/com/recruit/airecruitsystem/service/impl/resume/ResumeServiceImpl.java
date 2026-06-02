package com.recruit.airecruitsystem.service.impl.resume;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.airecruitsystem.constant.ResultCode;
import com.recruit.airecruitsystem.mapper.*;
import com.recruit.airecruitsystem.model.ResumeAnalysisSnapshot;
import com.recruit.airecruitsystem.pojo.*;
import com.recruit.airecruitsystem.service.resume.ResumeAiClient;
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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ResumeServiceImpl implements ResumeService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "doc", "docx");
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
    private ResumeAiClient resumeAiClient;

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
        if (resumeMapper.selectBySeekerId(seekerId) != null) {
            return ResultCode.RESUME_EXIST;
        }

        String extension = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResultCode.FILE_FORMAT_ERROR;
        }
        if (file.getSize() > MAX_RESUME_SIZE) {
            return ResultCode.FILE_TOO_LARGE;
        }

        Resume resume = null;
        try {
            StoredResumeFile storedFile = storeResumeFile(seekerId, file, extension);
            resume = Resume.builder()
                    .seekerId(seekerId)
                    .fileName(resolveOriginalFileName(file, extension))
                    .fileUrl(storedFile.fileUrl())
                    .isParsed(2)
                    .build();
            resumeMapper.insert(resume);
            analyzeAndPersist(resume, seeker, file);
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

    @Override
    public List<ResumeSummaryVO> getMyResumeList(Integer seekerId) {
        List<ResumeSummaryVO> list = new ArrayList<>();
        if (seekerId == null) {
            return list;
        }
        Resume resume = resumeMapper.selectBySeekerId(seekerId);
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

        Seeker seeker = seekerMapper.findById(seekerId);
        ResumeAnalysisSnapshot snapshot = buildSnapshot(resume, seeker);
        vo.setResumeId(resume.getId());
        vo.setResumeFileName(resume.getFileName());
        vo.setResumeFileUrl(resume.getFileUrl());
        vo.setIsParsed(resume.getIsParsed());
        vo.setPreviewText(buildPreviewText(seeker, snapshot));
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

        Seeker seeker = seekerMapper.findById(seekerId);
        ResumeAnalysisSnapshot snapshot = buildSnapshot(resume, seeker);
        vo.setResumeId(resume.getId());
        vo.setResumeFileName(resume.getFileName());
        vo.setPreviewText(buildPreviewText(seeker, snapshot));
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

        Seeker seeker = seekerMapper.findById(seekerId);
        try {
            resumeMapper.updateParseStatus(resumeId, 2, null);
            analyzeAndPersist(resume, seeker, null);
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

        ResumeAnalysisSnapshot snapshot = buildSnapshot(resume, seeker);

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
        resumeInfo.setKeywordCoverage(snapshot.getKeywordCoverage());
        resumeInfo.setWorkExperience(snapshot.getWorkExperience());
        resumeInfo.setSkills(snapshot.getSkills());
        resumeInfo.setWorkHistory(snapshot.getWorkHistory());
        resumeInfo.setPreviewText(buildPreviewText(seeker, snapshot));

        HrResumeDetailVO.MatchInfo matchInfo = new HrResumeDetailVO.MatchInfo();
        ResumeAnalysisSnapshot.MatchInsight insight = snapshot.getMatchInsight();
        matchInfo.setMatchScore(insight.getScore());
        matchInfo.setMatchLevel(insight.getLevel());
        matchInfo.setAiComment(insight.getHeadline());
        matchInfo.setCoreAdvantages(insight.getStrengths());
        matchInfo.setPotentialRisks(insight.getConcerns());
        matchInfo.setSkillTags(insight.getTags());

        HrResumeDetailVO vo = new HrResumeDetailVO();
        vo.setDeliveryId(delivery.getId());
        vo.setJobId(job.getId());
        vo.setJobName(job.getJobName());
        vo.setSeekerInfo(seekerInfo);
        vo.setResumeInfo(resumeInfo);
        vo.setMatchInfo(matchInfo);
        vo.setStatus(delivery.getStatus());
        vo.setDeliveryTime(delivery.getDeliveryTime());
        vo.setUpdateTime(delivery.getUpdateTime());
        return vo;
    }

    private void analyzeAndPersist(Resume resume, Seeker seeker, MultipartFile file) throws IOException {
        String rawText = buildRawContext(seeker, resume.getFileName());
        if (file != null && file.getContentType() != null && file.getContentType().startsWith("text/")) {
            rawText = rawText + "\n" + new String(file.getBytes(), StandardCharsets.UTF_8);
        }
        ResumeAnalysisSnapshot snapshot = resumeAiClient.analyze(seeker, resume, rawText);
        persistSnapshot(resume.getId(), snapshot);
    }

    private void persistSnapshot(Integer resumeId, ResumeAnalysisSnapshot snapshot) throws IOException {
        ResumeParseResult record = ResumeParseResult.builder()
                .resumeId(resumeId)
                .keywordCoverage(snapshot.getKeywordCoverage())
                .basicInfo(objectMapper.writeValueAsString(snapshot.getBasicInfo()))
                .workExperience(snapshot.getWorkExperience())
                .skills(objectMapper.writeValueAsString(snapshot.getSkills()))
                .workHistory(objectMapper.writeValueAsString(snapshot.getWorkHistory()))
                .aiSummary(snapshot.getAiSummary())
                .improvementSuggestions(snapshot.getImprovementSuggestions())
                .build();
        if (resumeParseResultMapper.selectByResumeId(resumeId) == null) {
            resumeParseResultMapper.insert(record);
        } else {
            resumeParseResultMapper.updateByResumeId(record);
        }
    }

    private ResumeAnalysisSnapshot buildSnapshot(Resume resume, Seeker seeker) {
        ResumeParseResult result = resumeParseResultMapper.selectByResumeId(resume.getId());
        if (result == null) {
            return decorateSnapshot(resumeAiClient.analyze(seeker, resume, buildRawContext(seeker, resume.getFileName())), seeker);
        }
        try {
            ResumeAnalysisSnapshot snapshot = ResumeAnalysisSnapshot.builder()
                    .keywordCoverage(result.getKeywordCoverage())
                    .basicInfo(objectMapper.readValue(result.getBasicInfo(), ResumeAnalysisSnapshot.BasicInfo.class))
                    .workExperience(result.getWorkExperience())
                    .skills(objectMapper.readValue(result.getSkills(), new TypeReference<List<String>>() {}))
                    .workHistory(objectMapper.readValue(result.getWorkHistory(), new TypeReference<List<ResumeAnalysisSnapshot.WorkHistoryItem>>() {}))
                    .aiSummary(result.getAiSummary())
                    .improvementSuggestions(result.getImprovementSuggestions())
                    .build();
            return decorateSnapshot(snapshot, seeker);
        } catch (Exception e) {
            return decorateSnapshot(resumeAiClient.analyze(seeker, resume, buildRawContext(seeker, resume.getFileName())), seeker);
        }
    }

    private ResumeAnalysisSnapshot decorateSnapshot(ResumeAnalysisSnapshot snapshot, Seeker seeker) {
        ResumeAnalysisSnapshot target = snapshot == null ? new ResumeAnalysisSnapshot() : snapshot;

        if (target.getBasicInfo() == null) {
            target.setBasicInfo(ResumeAnalysisSnapshot.BasicInfo.builder()
                    .realName(seeker.getRealName())
                    .phone(seeker.getPhone())
                    .email(seeker.getEmail())
                    .age(seeker.getAge())
                    .eduBack(seeker.getEduBack())
                    .almaMater(seeker.getAlmaMater())
                    .build());
        }
        if (target.getSkills() == null) {
            target.setSkills(Collections.emptyList());
        }
        if (target.getWorkHistory() == null) {
            target.setWorkHistory(Collections.emptyList());
        }
        if (target.getKeywordCoverage() == null) {
            target.setKeywordCoverage(80);
        }
        if (target.getQualityReport() == null) {
            target.setQualityReport(buildQualityReport(target.getKeywordCoverage()));
        }
        if (target.getSuggestionCards() == null || target.getSuggestionCards().isEmpty()) {
            target.setSuggestionCards(buildSuggestionCards());
        }
        if (target.getRadarMetrics() == null || target.getRadarMetrics().isEmpty()) {
            target.setRadarMetrics(buildRadarMetrics(seeker));
        }
        if (target.getMatchInsight() == null) {
            target.setMatchInsight(buildMatchInsight(seeker));
        }
        if (!StringUtils.hasText(target.getAiSummary())) {
            target.setAiSummary("简历整体信息完整，和目标岗位存在较高相关度。");
        }
        if (!StringUtils.hasText(target.getImprovementSuggestions())) {
            target.setImprovementSuggestions("建议补充更多量化成果，并优化和目标岗位更相关的关键词。");
        }
        if (!StringUtils.hasText(target.getWorkExperience())) {
            target.setWorkExperience("3 年以上相关经验");
        }
        return target;
    }

    private ResumeAnalysisSnapshot.QualityReport buildQualityReport(Integer keywordCoverage) {
        int coverage = keywordCoverage == null ? 80 : keywordCoverage;
        int total = Math.max(74, Math.min(95, coverage + 4));
        return ResumeAnalysisSnapshot.QualityReport.builder()
                .totalScore(total)
                .keywordRichness(coverage)
                .structureCompleteness(93)
                .benchmarkScore(71)
                .build();
    }

    private List<ResumeAnalysisSnapshot.SuggestionCard> buildSuggestionCards() {
        return List.of(
                ResumeAnalysisSnapshot.SuggestionCard.builder()
                        .title("补充量化成果")
                        .detail("建议在项目经历中加入效率提升、成本下降或转化增长等结果。")
                        .emphasis("例如：效率提升 25%")
                        .actionLabel("查看示例")
                        .build(),
                ResumeAnalysisSnapshot.SuggestionCard.builder()
                        .title("更新岗位关键词")
                        .detail("建议把通用描述替换成更贴近目标岗位的能力关键词。")
                        .emphasis("如：Transformer 调优、多模态工程")
                        .actionLabel("应用建议")
                        .build()
        );
    }

    private List<ResumeAnalysisSnapshot.RadarMetric> buildRadarMetrics(Seeker seeker) {
        boolean frontendMode = StringUtils.hasText(seeker.getExPosition())
                && seeker.getExPosition().toLowerCase(Locale.ROOT).contains("前端");
        return List.of(
                ResumeAnalysisSnapshot.RadarMetric.builder().label("AI 理解").userScore(88).benchmarkScore(76).build(),
                ResumeAnalysisSnapshot.RadarMetric.builder().label("数据驱动").userScore(frontendMode ? 79 : 86).benchmarkScore(72).build(),
                ResumeAnalysisSnapshot.RadarMetric.builder().label("项目推进").userScore(frontendMode ? 74 : 81).benchmarkScore(68).build(),
                ResumeAnalysisSnapshot.RadarMetric.builder().label("交互创新").userScore(frontendMode ? 84 : 72).benchmarkScore(70).build(),
                ResumeAnalysisSnapshot.RadarMetric.builder().label("业务闭环").userScore(frontendMode ? 70 : 87).benchmarkScore(73).build()
        );
    }

    private ResumeAnalysisSnapshot.MatchInsight buildMatchInsight(Seeker seeker) {
        boolean frontendMode = StringUtils.hasText(seeker.getExPosition())
                && seeker.getExPosition().toLowerCase(Locale.ROOT).contains("前端");
        return ResumeAnalysisSnapshot.MatchInsight.builder()
                .score(frontendMode ? 94 : 90)
                .level("High Match")
                .headline(frontendMode ? "综合匹配度极高" : "综合匹配度良好")
                .strengths(frontendMode
                        ? List.of(
                        "做过 AI 助手场景落地，具备 LLM 应用整合经验。",
                        "复杂协同系统经验扎实，工程化和性能优化能力强。")
                        : List.of(
                        "具备 AI 产品从规划到落地的完整链路经验。",
                        "数据分析和增长策略能力比较成熟。"))
                .concerns(List.of("管理带队经历仍需进一步确认，当前履历更偏技术专家路径。"))
                .tags(frontendMode
                        ? List.of("LLM 应用专家", "架构能力强", "英语流利")
                        : List.of("AI 产品负责人", "数据增长", "商业闭环"))
                .build();
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

    private void deleteStoredFile(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }
        try {
            String normalizedAccessPath = accessPath.endsWith("/") ? accessPath : accessPath + "/";
            if (!fileUrl.startsWith(normalizedAccessPath)) {
                return;
            }
            String relative = fileUrl.substring(normalizedAccessPath.length());
            Path fullPath = Paths.get(uploadPath).resolve(relative.replace("/", java.io.File.separator));
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
    }

    private String buildPreviewText(Seeker seeker, ResumeAnalysisSnapshot snapshot) {
        ResumeAnalysisSnapshot.BasicInfo info = snapshot.getBasicInfo();
        StringBuilder builder = new StringBuilder();
        builder.append("姓名：").append(nullToEmpty(info.getRealName())).append("\n");
        builder.append("目标岗位：").append(nullToEmpty(seeker.getExPosition())).append("\n");
        builder.append("电话：").append(nullToEmpty(info.getPhone())).append("    ");
        builder.append("邮箱：").append(nullToEmpty(info.getEmail())).append("\n");
        builder.append("学历：").append(nullToEmpty(info.getEduBack())).append(" / ")
                .append(nullToEmpty(info.getAlmaMater())).append("\n\n");
        builder.append("技能：").append(String.join(" / ", snapshot.getSkills())).append("\n\n");
        builder.append("工作经历：\n");

        int index = 1;
        for (ResumeAnalysisSnapshot.WorkHistoryItem item : snapshot.getWorkHistory()) {
            builder.append(index++).append(". ")
                    .append(item.getCompany()).append(" / ")
                    .append(item.getPosition()).append(" / ")
                    .append(item.getStartTime()).append(" - ").append(item.getEndTime()).append("\n   ")
                    .append(item.getDescription()).append("\n");
        }
        builder.append("\nAI 总结：").append(snapshot.getAiSummary());
        return builder.toString();
    }

    private String buildRawContext(Seeker seeker, String fileName) {
        return String.format(
                Locale.ROOT,
                "文件名:%s 候选人:%s 目标岗位:%s 城市:%s 学历:%s 状态:%s",
                fileName,
                nullToEmpty(seeker.getRealName()),
                nullToEmpty(seeker.getExPosition()),
                nullToEmpty(seeker.getExCity()),
                nullToEmpty(seeker.getEduBack()),
                nullToEmpty(seeker.getState())
        );
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

    private record StoredResumeFile(Path path, String fileUrl) {
    }
}
