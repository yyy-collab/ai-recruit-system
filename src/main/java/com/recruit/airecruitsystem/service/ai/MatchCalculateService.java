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

    // 缓存（使用项目已有的 SimpleCache，设置1小时过期，最大1000条）
    private static final int CACHE_MAX_SIZE = 1000;
    private SimpleCache<String, Double> matchCache = new SimpleCache<>(3600);

    // 是否启用词性过滤（名词/动词），开启可提升关键词质量
    private static final boolean ENABLE_POS_FILTER = true;

    // 文本转 TF-IDF 向量（统一停用词，可选词性过滤）
    private Map<String, Double> textToVector(String text) {
        List<Term> termList = HanLP.segment(text);
        List<String> words = new ArrayList<>();
        for (Term term : termList) {
            String word = term.word;
            String nature = term.nature.toString();
            // 过滤停用词和单字符
            if (StopWordsLoader.isStopWord(word) || word.length() <= 1) {
                continue;
            }
            // 可选词性过滤
            if (ENABLE_POS_FILTER) {
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

        // 计算 TF-IDF 向量
        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : tfMap.entrySet()) {
            String word = entry.getKey();
            double tf = (double) entry.getValue() / maxTf;
            double idf = corpusManager.getIdf(word);
            vector.put(word, tf * idf);
        }
        return vector;
    }

    // 余弦相似度（返回 0~1）
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

    /**
     * 加分逻辑（借鉴优质代码，区分软硬技能，不限制匹配数量）
     */
    private int getExtraScore(String jobText, String resumeText, String jobKeywordStr) {
        if (jobKeywordStr == null || jobKeywordStr.isBlank()) {
            return 0;
        }

        // 软技能集合（可根据业务扩展）
        Set<String> softSkills = new HashSet<>(Arrays.asList(
                "沟通", "协调", "责任心", "抗压", "团队协作", "学习能力"
        ));

        // 解析岗位关键词（逗号、分号分隔）
        Set<String> jobSkills = new HashSet<>();
        for (String s : jobKeywordStr.split("[,，;；]")) {
            String trim = s.trim().toLowerCase();
            if (trim.length() >= 2) {
                jobSkills.add(trim);
            }
        }
        if (jobSkills.isEmpty()) {
            return 0;
        }

        String resumeLower = resumeText.toLowerCase();
        int extra = 0;
        int matchCount = 0;

        // 遍历所有岗位技能，匹配加分（软技能+1，硬技能+4）
        for (String skill : jobSkills) {
            if (resumeLower.contains(skill)) {
                matchCount++;
                if (softSkills.contains(skill)) {
                    extra += 1;
                } else {
                    extra += 4;
                }
            }
        }

        // 学历加分（各+3）
        boolean jobNeedEdu = jobText.contains("本科") || jobText.contains("大专") || jobText.contains("硕士");
        boolean resumeHasEdu = resumeText.contains("本科") || resumeText.contains("大专") || resumeText.contains("硕士");
        if (jobNeedEdu && resumeHasEdu) {
            extra += 3;
        }

        // 工作年限加分（3-5年区间匹配）
        boolean jobNeed3To5 = jobText.contains("3-5年") || jobText.contains("3年以上");
        boolean resumeHas4Exp = resumeText.contains("4.5年") || resumeText.contains("4年") || resumeText.contains("5年");
        if (jobNeed3To5 && resumeHas4Exp) {
            extra += 3;
        }

        // 加分上限 20（与优质代码一致）
        return Math.min(extra, 20);
    }

    /**
     * 跨赛道折损：基于 Top5 核心词，排除软技能和通用词，重合数 < 2 时降权
     */
    private double applyCrossDomainPenalty(double total, String jobText, String resumeText) {
        List<String> jobTopWords = keywordExtractService.extractKeywords(jobText, 5);
        List<String> resumeTopWords = keywordExtractService.extractKeywords(resumeText, 5);
        Set<String> jobWordSet = new HashSet<>(jobTopWords);

        // 需排除的通用词/软技能
        Set<String> filterSet = new HashSet<>(Arrays.asList(
                "沟通", "协调", "责任心", "抗压", "团队协作", "学习能力",
                "优化", "页面", "功能", "bug", "开发", "系统", "项目", "需求"
        ));

        int sameCoreWordCount = 0;
        for (String rWord : resumeTopWords) {
            if (!filterSet.contains(rWord) && jobWordSet.contains(rWord)) {
                sameCoreWordCount++;
            }
        }

        if (sameCoreWordCount < 2) {
            System.out.println("跨赛道折损：核心词匹配数 " + sameCoreWordCount + "，总分乘以 0.6");
            return total * 0.6;
        }
        return total;
    }

    /**
     * 主匹配方法（三参数）
     */
    public double calculateMatch(String jobText, String resumeText, String jobKeywordStr) {
        String cacheKey = "match_" + Objects.hash(jobText, resumeText, jobKeywordStr);
        Double cached = matchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 向量化与相似度
        Map<String, Double> jobVec = textToVector(jobText);
        Map<String, Double> resumeVec = textToVector(resumeText);
        double baseSimilar = cosineSimilarity(jobVec, resumeVec);
        double baseScore = baseSimilar * 70; // 基础分 0~70

        // 加分
        int extra = getExtraScore(jobText, resumeText, jobKeywordStr);
        double total = baseScore + extra;

        // 跨赛道折损
        total = applyCrossDomainPenalty(total, jobText, resumeText);

        // 基础分极低时的兜底（≤30 分）
        if (baseScore < 10) {
            total = Math.min(total, 30);
        }

        // 全局封顶 95
        total = Math.min(total, 95);

        // 存入缓存
        matchCache.put(cacheKey, total);
        return total;
    }

    /**
     * 匹配等级判定
     */
    public String getMatchLevel(double score) {
        if (score >= 70) return "高潜力";
        if (score >= 40) return "中潜力";
        return "低潜力";
    }

    /**
     * 清除缓存（重新计算时调用）
     */
    public void clearCache(String jobText, String resumeText, String jobKeywordStr) {
        String cacheKey = "match_" + Objects.hash(jobText, resumeText, jobKeywordStr);
        matchCache.remove(cacheKey);
    }
}