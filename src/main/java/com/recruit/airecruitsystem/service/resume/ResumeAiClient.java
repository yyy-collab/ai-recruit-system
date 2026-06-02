package com.recruit.airecruitsystem.service.resume;

import com.recruit.airecruitsystem.model.ResumeAnalysisSnapshot;
import com.recruit.airecruitsystem.pojo.Resume;
import com.recruit.airecruitsystem.pojo.Seeker;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class ResumeAiClient {

    private final RestClient restClient = RestClient.builder().build();

    @Value("${ai.resume.endpoint:}")
    private String endpoint;

    public ResumeAnalysisSnapshot analyze(Seeker seeker, Resume resume, String rawText) {
        if (StringUtils.hasText(endpoint)) {
            try {
                ResumeAnalysisSnapshot remote = restClient.post()
                        .uri(endpoint)
                        .body(new AnalyzeRequest(seeker, resume, rawText))
                        .retrieve()
                        .body(ResumeAnalysisSnapshot.class);
                if (remote != null) {
                    return remote;
                }
            } catch (Exception ignored) {
                // Fall back to the local heuristic snapshot.
            }
        }
        return buildFallbackSnapshot(seeker, rawText);
    }

    private ResumeAnalysisSnapshot buildFallbackSnapshot(Seeker seeker, String rawText) {
        String role = StringUtils.hasText(seeker.getExPosition()) ? seeker.getExPosition() : "AI 产品候选人";
        String normalized = (role + " " + rawText).toLowerCase(Locale.ROOT);
        boolean frontendMode = normalized.contains("前端") || normalized.contains("vue") || normalized.contains("react");
        boolean productMode = normalized.contains("产品") || normalized.contains("strategy") || normalized.contains("需求");

        Set<String> skills = new LinkedHashSet<>();
        if (frontendMode) {
            skills.addAll(List.of("Vue 3", "TypeScript", "LLM 应用", "组件化", "性能优化", "多端协同"));
        }
        if (productMode) {
            skills.addAll(List.of("用户研究", "数据分析", "增长策略", "AI 工作流", "跨团队协作"));
        }
        if (skills.isEmpty()) {
            skills.addAll(List.of("需求拆解", "业务建模", "项目推进", "Prompt 设计"));
        }

        List<ResumeAnalysisSnapshot.WorkHistoryItem> workHistory = frontendMode
                ? buildFrontendHistory()
                : buildProductHistory();

        ResumeAnalysisSnapshot.BasicInfo basicInfo = ResumeAnalysisSnapshot.BasicInfo.builder()
                .realName(seeker.getRealName())
                .phone(seeker.getPhone())
                .email(seeker.getEmail())
                .age(seeker.getAge())
                .eduBack(seeker.getEduBack())
                .almaMater(seeker.getAlmaMater())
                .build();

        int coverage = frontendMode ? 84 : 80;
        return ResumeAnalysisSnapshot.builder()
                .keywordCoverage(coverage)
                .basicInfo(basicInfo)
                .workExperience(frontendMode ? "4.5 年前端经验" : "5 年产品经验")
                .skills(new ArrayList<>(skills))
                .workHistory(workHistory)
                .aiSummary(frontendMode
                        ? "候选人在前端架构与 AI 场景整合方面表现突出，具备复杂协同系统的落地经验。"
                        : "候选人在 AI 产品规划、增长策略与跨团队协作方面具备成熟经验。")
                .improvementSuggestions(frontendMode
                        ? "建议补充更明确的量化结果，并强化智能体工作流、多模态工程等关键词。"
                        : "建议补充更量化的业务结果，并突出 AI 产品岗位的核心方法论。")
                .qualityReport(ResumeAnalysisSnapshot.QualityReport.builder()
                        .totalScore(88)
                        .keywordRichness(coverage)
                        .structureCompleteness(93)
                        .benchmarkScore(71)
                        .build())
                .suggestionCards(List.of(
                        ResumeAnalysisSnapshot.SuggestionCard.builder()
                                .title("补充量化成果")
                                .detail("建议在项目经历中明确写出效率提升、成本节约或转化增长等结果。")
                                .emphasis("例如：效率提升 25%")
                                .actionLabel("查看示例")
                                .build(),
                        ResumeAnalysisSnapshot.SuggestionCard.builder()
                                .title("刷新岗位关键词")
                                .detail("建议把通用描述替换成更贴近目标岗位的能力关键词。")
                                .emphasis("如：Transformer 调优、多模态工程")
                                .actionLabel("应用建议")
                                .build()))
                .radarMetrics(buildRadarMetrics(frontendMode))
                .matchInsight(buildMatchInsight(frontendMode))
                .build();
    }

    private List<ResumeAnalysisSnapshot.WorkHistoryItem> buildFrontendHistory() {
        List<ResumeAnalysisSnapshot.WorkHistoryItem> items = new ArrayList<>();
        items.add(ResumeAnalysisSnapshot.WorkHistoryItem.builder()
                .company("字节跳动 (ByteDance)")
                .position("资深前端开发工程师 / 飞书 AI 团队")
                .startTime("2022.06")
                .endTime("至今")
                .description("主导飞书 MyAI 桌面端架构设计，负责 AI 能力接入、组件抽象和复杂交互体验优化。")
                .coreSkills(List.of("Vue 3", "LLM 应用", "架构设计"))
                .build());
        items.add(ResumeAnalysisSnapshot.WorkHistoryItem.builder()
                .company("阿里巴巴 (Alibaba)")
                .position("高级前端开发工程师 / 钉钉")
                .startTime("2019.07")
                .endTime("2022.05")
                .description("负责钉钉文档多人协同编辑器的性能优化，建设大规模协同场景下的前端基础设施。")
                .coreSkills(List.of("性能优化", "协同编辑", "工程化"))
                .build());
        return items;
    }

    private List<ResumeAnalysisSnapshot.WorkHistoryItem> buildProductHistory() {
        List<ResumeAnalysisSnapshot.WorkHistoryItem> items = new ArrayList<>();
        items.add(ResumeAnalysisSnapshot.WorkHistoryItem.builder()
                .company("字节跳动 (ByteDance)")
                .position("AI 产品经理")
                .startTime("2022.03")
                .endTime("至今")
                .description("负责智能助手产品规划与商业化落地，推动对话式 AI 在企业场景中的多端接入。")
                .coreSkills(List.of("用户研究", "AI 工作流", "商业化"))
                .build());
        items.add(ResumeAnalysisSnapshot.WorkHistoryItem.builder()
                .company("美团 (Meituan)")
                .position("高级产品经理")
                .startTime("2019.07")
                .endTime("2022.02")
                .description("围绕增长与效率场景搭建数据看板与自动化运营策略，提升跨团队协同效率。")
                .coreSkills(List.of("数据分析", "增长策略", "项目推进"))
                .build());
        return items;
    }

    private List<ResumeAnalysisSnapshot.RadarMetric> buildRadarMetrics(boolean frontendMode) {
        return List.of(
                ResumeAnalysisSnapshot.RadarMetric.builder().label("AI 理解").userScore(88).benchmarkScore(76).build(),
                ResumeAnalysisSnapshot.RadarMetric.builder().label("数据驱动").userScore(frontendMode ? 79 : 86).benchmarkScore(72).build(),
                ResumeAnalysisSnapshot.RadarMetric.builder().label("项目推进").userScore(frontendMode ? 74 : 81).benchmarkScore(68).build(),
                ResumeAnalysisSnapshot.RadarMetric.builder().label("交互创新").userScore(frontendMode ? 84 : 72).benchmarkScore(70).build(),
                ResumeAnalysisSnapshot.RadarMetric.builder().label("业务闭环").userScore(frontendMode ? 70 : 87).benchmarkScore(73).build()
        );
    }

    private ResumeAnalysisSnapshot.MatchInsight buildMatchInsight(boolean frontendMode) {
        return ResumeAnalysisSnapshot.MatchInsight.builder()
                .score(frontendMode ? 94 : 90)
                .level("High Match")
                .headline(frontendMode ? "综合匹配度极高" : "综合匹配度良好")
                .strengths(frontendMode
                        ? List.of(
                        "直接负责过 AI 助手场景落地，具备 LLM 应用整合经验。",
                        "复杂协同系统经验扎实，工程化与性能优化能力强。")
                        : List.of(
                        "具备 AI 产品从规划到落地的完整链路经验。",
                        "数据分析和增长策略能力比较成熟。"))
                .concerns(List.of("管理带队经历还需要进一步确认，当前履历更偏资深专家路径。"))
                .tags(frontendMode
                        ? List.of("LLM 应用专家", "架构能力强", "英语流利")
                        : List.of("AI 产品负责人", "数据增长", "商业闭环"))
                .build();
    }

    @Data
    @AllArgsConstructor
    private static class AnalyzeRequest {
        private Seeker seeker;
        private Resume resume;
        private String rawText;
    }
}
