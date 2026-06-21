package com.recruit.airecruitsystem.service.ai;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.recruit.airecruitsystem.utils.SimpleCache;
import com.recruit.airecruitsystem.utils.StopWordsLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MatchCalculateService {

    @Autowired
    private CorpusManager corpusManager;

    @Autowired
    private KeywordExtractService keywordExtractService;

    // 缓存匹配结果，1小时过期
    private SimpleCache<String, Double> matchCache = new SimpleCache<>(3600);

    // 是否启用词性过滤（关闭，保留所有有效词）
    private static final boolean ENABLE_POS_FILTER = false;

    // ==================== 文本向量化 ====================
    private Map<String, Double> textToVector(String text) {
        List<Term> termList = HanLP.segment(text);
        List<String> words = new ArrayList<>();
        for (Term term : termList) {
            String word = term.word;
            // 过滤停用词和单字符
            if (StopWordsLoader.isStopWord(word) || word.length() <= 1) {
                continue;
            }
            // 可选词性过滤（默认关闭）
            if (ENABLE_POS_FILTER) {
                String nature = term.nature.toString();
                if (!(nature.startsWith("n") || nature.startsWith("v"))) {
                    continue;
                }
            }
            words.add(word.toLowerCase());
        }

        if (words.isEmpty()) {
            return Collections.emptyMap();
        }

        // 统计词频
        Map<String, Integer> tfMap = new HashMap<>();
        for (String w : words) {
            tfMap.put(w, tfMap.getOrDefault(w, 0) + 1);
        }
        int maxTf = tfMap.values().stream().max(Integer::compare).orElse(1);

        // 计算TF-IDF向量
        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : tfMap.entrySet()) {
            String word = entry.getKey();
            double tf = (double) entry.getValue() / maxTf;
            double idf = corpusManager.getIdf(word);
            vector.put(word, tf * idf);
        }
        return vector;
    }

    // ==================== 余弦相似度 ====================
    private double cosineSimilarity(Map<String, Double> vecA, Map<String, Double> vecB) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(vecA.keySet());
        allKeys.addAll(vecB.keySet());

        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (String key : allKeys) {
            double a = vecA.getOrDefault(key, 0.0);
            double b = vecB.getOrDefault(key, 0.0);
            dot += a * b;
            normA += a * a;
            normB += b * b;
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // ==================== 加分逻辑（含自动领域匹配） ====================
    private int getExtraScore(String jobText, String resumeText, String jobKeywordStr, double baseSimilar) {
        if (jobKeywordStr == null || jobKeywordStr.isBlank()) {
            return 0;
        }

        // 软技能集合
        Set<String> softSkills = new HashSet<>(Arrays.asList(
                "沟通", "协调", "责任心", "抗压", "团队协作", "学习能力"
        ));

        // 标准化岗位关键词（去除空格、转小写）
        Set<String> jobSkills = new HashSet<>();
        for (String s : jobKeywordStr.split("[,，;；]")) {
            String trim = s.trim().toLowerCase().replaceAll("\\s+", "");
            if (trim.length() >= 2) {
                jobSkills.add(trim);
            }
        }
        if (jobSkills.isEmpty()) {
            return 0;
        }

        // 标准化简历文本
        String resumeLower = resumeText.toLowerCase().replaceAll("\\s+", "");
        int extra = 0;

        // 技能匹配加分
        for (String skill : jobSkills) {
            if (resumeLower.contains(skill)) {
                if (softSkills.contains(skill)) {
                    extra += 1;
                } else {
                    extra += 4;
                }
            }
        }

        // 学历匹配加分
        if (containsAny(jobText, "本科", "大专", "硕士") && containsAny(resumeText, "本科", "大专", "硕士")) {
            extra += 5;
        }

        // 工作年限匹配加分
        if (containsAny(jobText, "3-5年", "3年以上") && containsAny(resumeText, "4.5年", "4年", "5年", "3年")) {
            extra += 5;
        }

        // ===== 自动领域匹配加分（基于向量相似度，无需预定义关键词） =====
        if (baseSimilar > 0.15) {
            extra += 6;
            System.out.println("自动领域匹配加分 +6（向量相似度：" + String.format("%.4f", baseSimilar) + "）");
        }

        return Math.min(extra, 35);
    }

    // ==================== 跨赛道折损（基于向量相似度智能判断） ====================
    private double applyCrossDomainPenalty(double total, String jobText, String resumeText, double baseSimilar) {
        // 如果向量相似度足够高，视为同领域，不折损
        if (baseSimilar > 0.15) {
            System.out.println("向量相似度充足，不触发跨赛道折损");
            return total;
        }

        // 否则执行原有折损逻辑（基于Top5关键词重合数）
        List<String> jobTopWords = keywordExtractService.extractKeywords(jobText, 5);
        List<String> resumeTopWords = keywordExtractService.extractKeywords(resumeText, 5);

        Set<String> jobSet = new HashSet<>();
        for (String w : jobTopWords) {
            jobSet.add(w.toLowerCase().replaceAll("\\s+", ""));
        }
        Set<String> resumeSet = new HashSet<>();
        for (String w : resumeTopWords) {
            resumeSet.add(w.toLowerCase().replaceAll("\\s+", ""));
        }

        // 排除通用词/软技能
        Set<String> filterSet = new HashSet<>(Arrays.asList(
                "沟通", "协调", "责任心", "抗压", "团队协作", "学习能力",
                "优化", "页面", "功能", "bug", "开发", "系统", "项目", "需求"
        ));

        int sameCoreWordCount = 0;
        for (String rWord : resumeSet) {
            if (!filterSet.contains(rWord) && jobSet.contains(rWord)) {
                sameCoreWordCount++;
            }
        }

        if (sameCoreWordCount < 2) {
            System.out.println("跨赛道折损：核心词匹配数 " + sameCoreWordCount + "，总分乘以 0.6");
            return total * 0.6;
        }
        return total;
    }

    // ==================== 辅助方法 ====================
    private boolean containsAny(String text, String... keywords) {
        String lower = text.toLowerCase();
        for (String kw : keywords) {
            if (lower.contains(kw)) return true;
        }
        return false;
    }

    // ==================== 主匹配方法 ====================
    public double calculateMatch(String jobText, String resumeText, String jobKeywordStr) {
        String cacheKey = "match_" + Objects.hash(jobText, resumeText, jobKeywordStr);
        Double cached = matchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 1. 向量化
        Map<String, Double> jobVec = textToVector(jobText);
        Map<String, Double> resumeVec = textToVector(resumeText);

        // 2. 余弦相似度
        double baseSimilar = cosineSimilarity(jobVec, resumeVec);
        double baseScore = baseSimilar * 75; // 基础分 0~75

        // 3. 加分（含自动领域匹配）
        int extra = getExtraScore(jobText, resumeText, jobKeywordStr, baseSimilar);
        double total = baseScore + extra;

        // 4. 跨赛道折损（基于相似度智能判断）
        total = applyCrossDomainPenalty(total, jobText, resumeText, baseSimilar);

        // 5. 基础分过低时的兜底（放宽至 20 分以下才封顶 30）
        if (baseScore < 20) {
            total = Math.min(total, 30);
        }

        // 6. 全局封顶 95
        total = Math.min(total, 95);

        // 7. 存入缓存
        matchCache.put(cacheKey, total);
        System.out.println("最终匹配分数：" + total);
        return total;
    }

    // ==================== 匹配等级判定 ====================
    public String getMatchLevel(double score) {
        if (score >= 70) return "高潜力";
        if (score >= 40) return "中潜力";
        return "低潜力";
    }

    // ==================== 清除缓存 ====================
    public void clearCache(String jobText, String resumeText, String jobKeywordStr) {
        String cacheKey = "match_" + Objects.hash(jobText, resumeText, jobKeywordStr);
        matchCache.remove(cacheKey);
    }
}