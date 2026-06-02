package com.recruit.airecruitsystem.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.recruit.airecruitsystem.mapper.*;
import com.recruit.airecruitsystem.model.ResumeAnalysisSnapshot;
import com.recruit.airecruitsystem.pojo.*;
import com.recruit.airecruitsystem.service.resume.ResumeAiClient;
import com.recruit.airecruitsystem.utils.PasswordEncoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class DemoResumeDataInitializer implements ApplicationRunner {

    private static final String DEMO_SEEKER_USERNAME = "resume_demo_sk";
    private static final String DEMO_HR_USERNAME = "resume_demo_hr";

    @Autowired
    private SeekerMapper seekerMapper;

    @Autowired
    private HrMapper hrMapper;

    @Autowired
    private ResumeMapper resumeMapper;

    @Autowired
    private ResumeParseResultMapper resumeParseResultMapper;

    @Autowired
    private JobMapper jobMapper;

    @Autowired
    private DeliveryMapper deliveryMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ResumeAiClient resumeAiClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${file.upload.path}")
    private String uploadPath;

    @Value("${file.upload.access-path}")
    private String accessPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Seeker seeker = ensureDemoSeeker();
        Hr hr = ensureDemoHr();
        Resume resume = ensureDemoResume(seeker);
        Job job = ensureDemoJob(hr);
        ensureDemoDelivery(job, seeker, resume);
    }

    private Seeker ensureDemoSeeker() {
        Seeker seeker = seekerMapper.findByUsername(DEMO_SEEKER_USERNAME);
        if (seeker != null) {
            return seeker;
        }

        Seeker seed = new Seeker();
        seed.setUsername(DEMO_SEEKER_USERNAME);
        seed.setPassword(passwordEncoder.encode("Demo1234"));
        seekerMapper.insert(seed);

        Seeker profile = new Seeker();
        profile.setId(seed.getId());
        profile.setRealName("陈志远");
        profile.setPhone("13888888888");
        profile.setEmail("chen.zy@example.com");
        profile.setAge(29);
        profile.setAddress("浙江杭州");
        profile.setEduBack("本科");
        profile.setAlmaMater("浙江大学");
        profile.setState("在职");
        profile.setExPosition("高级前端工程师 / AI 产品架构");
        profile.setExCity("杭州");
        profile.setExSalaryMin(25);
        profile.setExSalaryMax(40);
        seekerMapper.updateSeeker(profile);
        return seekerMapper.findById(seed.getId());
    }

    private Hr ensureDemoHr() {
        Hr hr = hrMapper.selectByUsername(DEMO_HR_USERNAME);
        if (hr != null) {
            return hr;
        }

        Hr seed = Hr.builder()
                .username(DEMO_HR_USERNAME)
                .password(passwordEncoder.encode("Demo1234"))
                .build();
        hrMapper.insert(seed);

        Hr profile = Hr.builder()
                .id(seed.getId())
                .realName("林雅宁")
                .companyName("智启科技")
                .email("hr-demo@example.com")
                .phone("13666666666")
                .build();
        hrMapper.updateHr(profile);
        return hrMapper.selectById(seed.getId());
    }

    private Resume ensureDemoResume(Seeker seeker) throws Exception {
        Resume resume = resumeMapper.selectBySeekerId(seeker.getId());
        if (resume == null) {
            Path demoDir = Paths.get(uploadPath, "resume", "demo");
            Files.createDirectories(demoDir);
            Path demoFile = demoDir.resolve("chen-zhiyuan-demo-resume.txt");
            Files.writeString(demoFile,
                    """
                    陈志远
                    高级前端工程师 / AI 产品架构
                    字节跳动 飞书 AI 团队
                    阿里巴巴 钉钉
                    Vue 3 TypeScript LLM 应用 组件化 性能优化
                    """,
                    StandardCharsets.UTF_8);

            String normalizedAccessPath = accessPath.endsWith("/") ? accessPath : accessPath + "/";
            resume = Resume.builder()
                    .seekerId(seeker.getId())
                    .fileName("陈志远-高级前端工程师-AI产品架构.pdf")
                    .fileUrl(normalizedAccessPath + "resume/demo/chen-zhiyuan-demo-resume.txt")
                    .isParsed(1)
                    .build();
            resumeMapper.insert(resume);
        }

        if (resumeParseResultMapper.selectByResumeId(resume.getId()) == null) {
            ResumeAnalysisSnapshot snapshot = resumeAiClient.analyze(
                    seeker,
                    resume,
                    "陈志远 高级前端工程师 AI 产品架构 飞书 AI 团队 LLM 应用 Vue3 TypeScript"
            );
            ResumeParseResult result = ResumeParseResult.builder()
                    .resumeId(resume.getId())
                    .keywordCoverage(snapshot.getKeywordCoverage())
                    .basicInfo(objectMapper.writeValueAsString(snapshot.getBasicInfo()))
                    .workExperience(snapshot.getWorkExperience())
                    .skills(objectMapper.writeValueAsString(snapshot.getSkills()))
                    .workHistory(objectMapper.writeValueAsString(snapshot.getWorkHistory()))
                    .aiSummary(snapshot.getAiSummary())
                    .improvementSuggestions(snapshot.getImprovementSuggestions())
                    .build();
            resumeParseResultMapper.insert(result);
        }

        return resumeMapper.selectBySeekerId(seeker.getId());
    }

    private Job ensureDemoJob(Hr hr) {
        Job job = jobMapper.selectFirstByHrId(hr.getId());
        if (job != null) {
            return job;
        }

        Job seed = Job.builder()
                .hrId(hr.getId())
                .jobName("高级前端开发工程师")
                .jobDesc("负责 AI 协同办公产品的前端架构设计与核心模块交付。")
                .requirement("具备大规模前端工程化经验，熟悉 AI 应用场景。")
                .keywords("Vue 3,TypeScript,LLM 应用,性能优化,协同编辑")
                .salary("25K-40K/月")
                .workAddress("杭州")
                .workExperience("3-5年")
                .status(1)
                .deliveryCount(1)
                .build();
        jobMapper.insert(seed);
        return seed;
    }

    private void ensureDemoDelivery(Job job, Seeker seeker, Resume resume) {
        if (deliveryMapper.existsByJobAndSeeker(job.getId(), seeker.getId())) {
            return;
        }
        Delivery delivery = Delivery.builder()
                .jobId(job.getId())
                .seekerId(seeker.getId())
                .resumeId(resume.getId())
                .status(0)
                .build();
        deliveryMapper.insert(delivery);
    }
}
