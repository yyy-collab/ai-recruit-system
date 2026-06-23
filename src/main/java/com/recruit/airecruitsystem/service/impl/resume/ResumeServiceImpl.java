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

/**
 * 简历业务服务实现类
 * 负责求职者简历上传、文件存储、简历解析、预览、删除、HR查看简历详情等全流程业务
 */
@Service
public class ResumeServiceImpl implements ResumeService {

    private static final List<String> ALLOWED_EXTENSIONS = List.of("doc", "docx");
    //简历最大上传大小：20MB
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

    //JSON序列化工具，用于解析存储的简历结构化数据
    @Autowired
    private ObjectMapper objectMapper;

    // Word文档解析工具，提取简历文本、HTML预览、结构化信息
    @Autowired
    private ResumeDocumentParser resumeDocumentParser;

    @Value("${file.upload.path}")
    private String uploadPath;

    //文件访问前缀URL，用于前端访问资源
    @Value("${file.upload.access-path}")
    private String accessPath;

    /**
     * 求职者上传/更新简历
     * @param seekerId 求职者ID
     * @param file 上传的Word文件
     * @param vo 简历返回摘要VO
     * @return 业务结果码，参考ResultCode
     */
    @Override
    public int uploadResume(Integer seekerId, MultipartFile file, ResumeSummaryVO vo) {
        // 校验登录身份必须为求职者
        if (!"seeker".equals(UserContext.getRole()) || seekerId == null) {
            return ResultCode.PARAM_ERROR;
        }
        // 校验求职者账号存在
        Seeker seeker = seekerMapper.findById(seekerId);
        if (seeker == null) {
            return ResultCode.NOT_FOUND;
        }
        // 校验求职者基础信息完整（姓名、手机、邮箱）
        if (!isProfileComplete(seeker)) {
            return ResultCode.INFO_INCOMPLETE;
        }
        // 文件非空校验
        if (file == null || file.isEmpty()) {
            return ResultCode.PARAM_ERROR;
        }

        // 获取文件后缀并校验格式
        String extension = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return ResultCode.FILE_FORMAT_ERROR;
        }
        // 校验文件大小不超过20MB
        if (file.getSize() > MAX_RESUME_SIZE) {
            return ResultCode.FILE_TOO_LARGE;
        }

        try {
            // 保存文件到本地磁盘
            StoredResumeFile storedFile = storeResumeFile(seekerId, file, extension);
            // 获取原始文件名
            String fileName = resolveOriginalFileName(file, extension);
            // 写入数据库记录并执行解析任务
            return createResume(seekerId, storedFile, fileName, extension, vo);
        } catch (IOException e) {
            return ResultCode.PARAM_ERROR;
        }
    }

    /**
     * 查询当前求职者所有简历列表（按创建时间倒序）
     * @param seekerId 求职者ID
     * @return 简历摘要VO集合
     */
    @Override
    public List<ResumeSummaryVO> getMyResumeList(Integer seekerId) {
        List<ResumeSummaryVO> list = new ArrayList<>();
        if (seekerId == null) {
            return list;
        }
        // 根据求职者ID查询全部简历
        List<Resume> resumes = resumeMapper.selectBySeekerIdOrderByIdDesc(seekerId);
        // 实体转VO封装
        for (Resume resume : resumes) {
            ResumeSummaryVO vo = new ResumeSummaryVO();
            copySummary(resume, vo);
            list.add(vo);
        }
        return list;
    }

    /**
     * 获取简历AI解析详情（前端评分、雷达图数据源接口）
     * @param seekerId 求职者ID
     * @param resumeId 简历ID
     * @param vo 简历AI详情返回VO
     * @return 业务结果码
     */
    @Override
    public int getResumeAiDetail(Integer seekerId, Integer resumeId, ResumeAiDetailVO vo) {
        // 查询简历记录
        Resume resume = resumeMapper.selectById(resumeId);
        // 校验简历归属权，防止越权访问
        if (!ownsResume(resume, seekerId)) {
            return ResultCode.NOT_FOUND;
        }
        // 校验简历解析状态：解析中/解析失败直接返回对应错误码
        if (resume.getIsParsed() != null && resume.getIsParsed() == 2) {
            return ResultCode.RESUME_PARSING;
        }
        if (resume.getIsParsed() != null && resume.getIsParsed() == 3) {
            return ResultCode.RESUME_PARSE_FAIL;
        }

        // 封装简历结构化解析快照数据
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

    /**
     * 原简历文件预览（返回HTML预览内容）
     * @param seekerId 求职者ID
     * @param resumeId 简历ID
     * @param vo 预览返回VO
     * @return 业务结果码
     */
    @Override
    public int previewResume(Integer seekerId, Integer resumeId, ResumePreviewVO vo) {
        Resume resume = resumeMapper.selectById(resumeId);
        // 校验简历归属
        if (!ownsResume(resume, seekerId)) {
            return ResultCode.NOT_FOUND;
        }
        vo.setResumeId(resume.getId());
        vo.setResumeFileName(resume.getFileName());
        // 纯文本预览内容
        vo.setPreviewText(buildOriginalPreviewText(resume));
        // HTML格式化预览页面
        vo.setPreviewHtml(buildOriginalPreviewHtml(resume));
        return ResultCode.SUCCESS;
    }

    /**
     * 删除简历
     * @param seekerId 求职者ID
     * @param resumeId 简历ID
     * @return 业务结果码
     */
    @Override
    public int deleteResume(Integer seekerId, Integer resumeId) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (!ownsResume(resume, seekerId)) {
            return ResultCode.NOT_FOUND;
        }
        // 如果该简历已投递岗位，禁止删除
        if (resumeMapper.countDeliveriesUsingResume(resumeId) > 0) {
            return ResultCode.RESUME_HAS_DELIVERY;
        }
        // 删除本地存储文件
        deleteStoredFile(resume.getFileUrl());
        // 删除简历解析记录
        resumeParseResultMapper.deleteByResumeId(resumeId);
        // 删除简历主记录
        resumeMapper.deleteById(resumeId);
        return ResultCode.SUCCESS;
    }

    /**
     * 重新解析已上传简历
     * @param seekerId 求职者ID
     * @param resumeId 简历ID
     * @param vo 简历摘要返回VO
     * @return 业务结果码
     */
    @Override
    public int reparseResume(Integer seekerId, Integer resumeId, ResumeSummaryVO vo) {
        Resume resume = resumeMapper.selectById(resumeId);
        if (!ownsResume(resume, seekerId)) {
            return ResultCode.NOT_FOUND;
        }
        // 解析中不可重复发起解析
        if (resume.getIsParsed() != null && resume.getIsParsed() == 2) {
            return ResultCode.RESUME_PARSING;
        }

        try {
            // 更新状态为解析中
            resumeMapper.updateParseStatus(resumeId, 2, null);
            // 兼容文件名/URL两种方式读取后缀
            String extension = resolveExtension(resume.getFileName());
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                extension = resolveExtension(resume.getFileUrl());
            }
            // 执行解析并持久化结构化数据
            parseAndPersist(resume, resolveStoredFilePath(resume.getFileUrl()), extension);
            // 更新状态为解析成功
            resumeMapper.updateParseStatus(resumeId, 1, null);

            // 封装最新简历信息返回
            Resume latest = resumeMapper.selectById(resumeId);
            copySummary(latest, vo);
            return ResultCode.SUCCESS;
        } catch (IOException e) {
            // 解析失败，更新状态并记录错误信息
            resumeMapper.updateParseStatus(resumeId, 3, trimMessage(e.getMessage()));
            return ResultCode.PARAM_ERROR;
        }
    }

    /**
     * HR查看投递简历完整详情（含求职者信息、简历解析数据、投递记录）
     * @param hrId HR账号ID
     * @param deliveryId 投递记录ID
     * @return HR简历详情VO，无权限/无数据返回null
     */
    @Override
    public HrResumeDetailVO getHrDeliveryDetail(Integer hrId, Integer deliveryId) {
        Delivery delivery = deliveryMapper.selectById(deliveryId);
        if (delivery == null) {
            return null;
        }

        // 校验岗位归属，仅创建该岗位的HR可查看
        Job job = jobMapper.selectById(delivery.getJobId());
        if (job == null || hrId == null || !hrId.equals(job.getHrId())) {
            return null;
        }

        Resume resume = resumeMapper.selectById(delivery.getResumeId());
        Seeker seeker = seekerMapper.findById(delivery.getSeekerId());
        if (resume == null || seeker == null) {
            return null;
        }

        // 加载简历结构化解析数据
        ResumeAnalysisSnapshot snapshot = buildSnapshot(resume);

        // 封装求职者基础信息
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

        // 封装简历解析信息
        HrResumeDetailVO.ResumeInfo resumeInfo = new HrResumeDetailVO.ResumeInfo();
        resumeInfo.setResumeId(resume.getId());
        resumeInfo.setResumeFileName(resume.getFileName());
        resumeInfo.setResumeFileUrl(resume.getFileUrl());
        resumeInfo.setWorkExperience(snapshot.getWorkExperience());
        resumeInfo.setSkills(snapshot.getSkills());
        resumeInfo.setWorkHistory(snapshot.getWorkHistory());
        resumeInfo.setPreviewText(buildOriginalPreviewText(resume));

        // 组装完整返回VO
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

    /**
     * 执行简历解析并持久化结构化解析结果
     * @param resume 简历实体
     * @param filePath 本地文件路径
     * @param extension 文件后缀
     * @throws IOException 文件读取/解析异常
     */
    private void parseAndPersist(Resume resume, Path filePath, String extension) throws IOException {
        // 调用文档解析器生成结构化快照
        ResumeAnalysisSnapshot snapshot = resumeDocumentParser.parse(filePath, extension, resume.getFileName());
        // 将解析结果存入数据库
        persistSnapshot(resume.getId(), snapshot);
    }

    /**
     * 创建简历主记录，保存文件、写入数据库、自动解析简历
     * @param seekerId 求职者ID
     * @param storedFile 本地存储文件对象（路径+访问URL）
     * @param fileName 用户原始上传文件名
     * @param extension 文件后缀
     * @param vo 简历摘要返回VO
     * @return 业务结果码
     */
    private int createResume(Integer seekerId,
                             StoredResumeFile storedFile,
                             String fileName,
                             String extension,
                             ResumeSummaryVO vo) {
        Resume resume = null;
        try {
            // 构建简历数据库实体，初始状态为解析中
            resume = Resume.builder()
                    .seekerId(seekerId)
                    .fileName(fileName)
                    .fileUrl(storedFile.fileUrl())
                    .isParsed(2)
                    .build();
            // 插入简历主表
            resumeMapper.insert(resume);
            // 执行解析并保存结构化数据
            parseAndPersist(resume, storedFile.path(), extension);
            // 更新解析状态为成功
            resumeMapper.updateParseStatus(resume.getId(), 1, null);

            // 查询最新简历数据，封装返回VO
            Resume latest = resumeMapper.selectById(resume.getId());
            copySummary(latest, vo);
            return ResultCode.SUCCESS;
        } catch (IOException e) {
            // 解析异常，更新状态为失败并记录错误信息
            if (resume != null && resume.getId() != null) {
                resumeMapper.updateParseStatus(resume.getId(), 3, trimMessage(e.getMessage()));
            }
            return ResultCode.PARAM_ERROR;
        }
    }

    /**
     * 将简历结构化快照存入数据库（存在则更新，不存在则新增）
     * @param resumeId 简历ID
     * @param snapshot 简历解析结构化数据
     * @throws IOException JSON序列化异常
     */
    private void persistSnapshot(Integer resumeId, ResumeAnalysisSnapshot snapshot) throws IOException {
        ResumeParseResult record = ResumeParseResult.builder()
                .resumeId(resumeId)
                // 基础信息序列化为JSON字符串
                .basicInfo(objectMapper.writeValueAsString(snapshot.getBasicInfo()))
                .workExperience(snapshot.getWorkExperience())
                // 技能列表转JSON，空列表兜底
                .skills(objectMapper.writeValueAsString(nullToEmptyList(snapshot.getSkills())))
                // 工作经历数组转JSON
                .workHistory(objectMapper.writeValueAsString(nullToEmptyList(snapshot.getWorkHistory())))
                .build();
        // 判断是否已有解析记录，新增或更新
        if (resumeParseResultMapper.selectByResumeId(resumeId) == null) {
            resumeParseResultMapper.insert(record);
        } else {
            resumeParseResultMapper.updateByResumeId(record);
        }
    }

    /**
     * 根据简历ID加载解析后的结构化快照数据
     * @param resume 简历实体
     * @return 封装好的简历结构化数据快照
     */
    private ResumeAnalysisSnapshot buildSnapshot(Resume resume) {
        // 查询简历解析JSON记录
        ResumeParseResult result = resumeParseResultMapper.selectByResumeId(resume.getId());
        // 无解析记录返回空快照
        if (result == null) {
            return decorateSnapshot(new ResumeAnalysisSnapshot());
        }

        // 反序列化JSON字符串为对象
        ResumeAnalysisSnapshot snapshot = ResumeAnalysisSnapshot.builder()
                .basicInfo(readJson(result.getBasicInfo(), ResumeAnalysisSnapshot.BasicInfo.class, null))
                .workExperience(result.getWorkExperience())
                .skills(readJson(result.getSkills(), new TypeReference<List<String>>() {}, Collections.emptyList()))
                .workHistory(readJson(result.getWorkHistory(), new TypeReference<List<ResumeAnalysisSnapshot.WorkHistoryItem>>() {}, Collections.emptyList()))
                .build();
        // 统一空值兜底，避免页面空指针
        return decorateSnapshot(snapshot);
    }

    /**
     * 对快照进行空值填充，防止空集合/空对象报错
     * @param snapshot 原始快照
     * @return 补齐空对象/空集合的快照
     */
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

    /**
     * JSON字符串反序列化为普通对象，失败返回兜底值
     * @param json JSON字符串
     * @param type 目标实体Class
     * @param fallback 解析失败兜底值
     * @return 转换后的对象
     */
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

    /**
     * JSON字符串反序列化为泛型集合，失败返回兜底值
     * @param json JSON字符串
     * @param type 泛型类型引用
     * @param fallback 解析失败兜底集合
     * @return 转换后的集合
     */
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

    /**
     * 存储上传简历文件至本地磁盘，生成唯一访问URL
     * @param seekerId 求职者ID
     * @param file 上传文件
     * @param extension 文件后缀
     * @return 存储文件对象（本地路径+前端访问URL）
     * @throws IOException 文件写入异常
     */
    private StoredResumeFile storeResumeFile(Integer seekerId, MultipartFile file, String extension) throws IOException {
        // 按日期分目录存储 yyyy/MM/dd
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        Path targetDir = Paths.get(uploadPath, "resume", String.valueOf(seekerId), dateDir);
        // 创建多级目录
        Files.createDirectories(targetDir);

        // UUID生成唯一文件名，避免重名覆盖
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetFile = targetDir.resolve(storedName);
        // 写入本地文件
        file.transferTo(targetFile.toFile());

        // 拼接前端资源访问URL
        String normalizedAccessPath = accessPath.endsWith("/") ? accessPath : accessPath + "/";
        String fileUrl = normalizedAccessPath + "resume/" + seekerId + "/" + dateDir.replace("\\", "/") + "/" + storedName;
        return new StoredResumeFile(targetFile, fileUrl);
    }

    /**
     * 根据文件访问URL反向解析本地磁盘真实路径，做路径安全校验
     * @param fileUrl 前端文件访问地址
     * @return 本地磁盘Path对象
     * @throws IOException 路径非法/文件不存在/地址为空抛出异常
     */
    private Path resolveStoredFilePath(String fileUrl) throws IOException {
        if (!StringUtils.hasText(fileUrl)) {
            throw new IOException("简历文件地址为空");
        }

        String normalizedAccessPath = accessPath.endsWith("/") ? accessPath : accessPath + "/";
        // 校验文件URL属于本系统资源前缀，防止路径穿越攻击
        if (!fileUrl.startsWith(normalizedAccessPath)) {
            throw new IOException("简历文件不在本地上传目录");
        }

        // 截取相对路径，拼接本地根目录
        String relative = fileUrl.substring(normalizedAccessPath.length());
        Path uploadRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path fullPath = uploadRoot.resolve(relative.replace("/", java.io.File.separator)).normalize();
        // 二次校验路径未跳出上传根目录，防止目录穿越
        if (!fullPath.startsWith(uploadRoot)) {
            throw new IOException("简历文件路径非法");
        }
        // 校验文件物理存在
        if (!Files.exists(fullPath)) {
            throw new IOException("简历文件不存在");
        }
        return fullPath;
    }

    /**
     * 根据文件URL删除本地磁盘简历文件
     * @param fileUrl 文件访问URL
     */
    private void deleteStoredFile(String fileUrl) {
        if (!StringUtils.hasText(fileUrl)) {
            return;
        }
        try {
            Path fullPath = resolveStoredFilePath(fileUrl);
            Files.deleteIfExists(fullPath);
        } catch (IOException ignored) {
            // 删除失败不抛出异常，仅忽略
        }
    }

    /**
     * 校验简历归属权：判断当前简历是否属于操作的求职者
     * @param resume 简历实体
     * @param seekerId 当前操作求职者ID
     * @return true=归属正确 false=越权
     */
    private boolean ownsResume(Resume resume, Integer seekerId) {
        return resume != null && seekerId != null && seekerId.equals(resume.getSeekerId());
    }

    /**
     * 校验求职者基础资料是否完整（必填：姓名、手机、邮箱）
     * @param seeker 求职者实体
     * @return true=资料完整 false=缺失必填信息
     */
    private boolean isProfileComplete(Seeker seeker) {
        return StringUtils.hasText(seeker.getRealName())
                && StringUtils.hasText(seeker.getPhone())
                && StringUtils.hasText(seeker.getEmail());
    }

    /**
     * 获取原始上传文件名，无文件名称则生成默认文件名
     * @param file 上传文件
     * @param extension 文件后缀
     * @return 原始文件名
     */
    private String resolveOriginalFileName(MultipartFile file, String extension) {
        return StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "resume." + extension;
    }

    /**
     * 截取文件后缀并统一转为小写
     * @param originalFilename 文件全名
     * @return 文件后缀，无后缀返回空字符串
     */
    private String resolveExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Resume实体转简历摘要VO，用于列表展示
     * @param resume 数据库简历实体
     * @param vo 输出摘要VO
     */
    private void copySummary(Resume resume, ResumeSummaryVO vo) {
        vo.setResumeId(resume.getId());
        vo.setResumeFileName(resume.getFileName());
        vo.setResumeFileUrl(resume.getFileUrl());
        vo.setIsParsed(resume.getIsParsed());
        vo.setCreateTime(resume.getCreateTime());
        vo.setUpdateTime(resume.getUpdateTime());
    }

    /**
     * 读取原始Word文档纯文本预览内容，读取失败则返回解析快照拼接文本兜底
     * @param resume 简历实体
     * @return 简历纯文本内容
     */
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

    /**
     * 读取原始Word文档转HTML预览页面，读取失败返回兜底静态HTML
     * @param resume 简历实体
     * @return HTML预览字符串
     */
    private String buildOriginalPreviewHtml(Resume resume) {
        try {
            String extension = resolveExtension(resume.getFileName());
            if (!ALLOWED_EXTENSIONS.contains(extension)) {
                extension = resolveExtension(resume.getFileUrl());
            }

            Path filePath = resolveStoredFilePath(resume.getFileUrl());
            return resumeDocumentParser.extractPreviewHtml(filePath, extension, resume.getFileName());
        } catch (IOException e) {
            return wrapFallbackPreviewHtml(resume.getFileName());
        }
    }

    /**
     * 生成预览失败兜底HTML页面
     * @param fileName 简历文件名
     * @return 静态HTML字符串
     */
    private String wrapFallbackPreviewHtml(String fileName) {
        String safeTitle = escapeHtml(StringUtils.hasText(fileName) ? fileName : "简历预览");
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>%s</title>
                  <style>
                    body {
                      margin: 0;
                      background: #eef2f7;
                      color: #172033;
                      font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
                    }
                    .page {
                      box-sizing: border-box;
                      max-width: 880px;
                      margin: 24px auto;
                      padding: 48px 56px;
                      background: #ffffff;
                      border-radius: 12px;
                      box-shadow: 0 18px 46px rgba(23, 32, 51, 0.12);
                    }
                    .title {
                      margin: 0 0 24px;
                      padding-bottom: 16px;
                      border-bottom: 1px solid #e5eaf1;
                      font-size: 18px;
                      font-weight: 700;
                    }
                    .content {
                      line-height: 1.7;
                      text-align: center;
                      word-break: break-word;
                    }
                  </style>
                </head>
                <body>
                  <main class="page">
                    <h1 class="title">%s</h1>
                    <div class="content">当前原简历无法在线预览，请下载后查看原文件。</div>
                  </main>
                </body>
                </html>
                """.formatted(safeTitle, safeTitle);
    }

    /**
     * HTML特殊字符转义，防止XSS注入
     * @param value 原始文本
     * @return 转义后安全文本
     */
    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * 根据解析快照拼接结构化纯文本，用于「解析后浏览」弹窗
     * @param snapshot 简历结构化解析数据
     * @return 格式化纯文本字符串
     */
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

    /**
     * 截断异常信息长度，限制数据库存储长度255字符
     * @param message 原始异常信息
     * @return 截断后错误文案
     */
    private String trimMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return "简历解析失败";
        }
        return message.length() > 255 ? message.substring(0, 255) : message;
    }

    /**
     * 字符串空值兜底，null转为空字符串
     * @param value 原始字符串
     * @return 非空字符串
     */
    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 集合空值兜底，null转为空不可变集合
     * @param values 原始集合
     * @return 非空集合
     */
    private <T> List<T> nullToEmptyList(List<T> values) {
        return values == null ? Collections.emptyList() : values;
    }

    /**
     * 本地存储文件记录内部记录类
     * @param path 本地磁盘完整路径
     * @param fileUrl 前端资源访问URL
     */
    private record StoredResumeFile(Path path, String fileUrl) {
    }
}