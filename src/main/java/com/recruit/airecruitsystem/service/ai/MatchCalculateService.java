package com.recruit.airecruitsystem.service.ai;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.recruit.airecruitsystem.utils.SimpleCache;
import com.recruit.airecruitsystem.utils.StopWordsLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class MatchCalculateService {

    @Autowired
    private CorpusManager corpusManager;

    // 缓存匹配结果，1小时过期
    private SimpleCache<String, Double> matchCache = new SimpleCache<>(3600);

    // 将文本转为 TF-IDF 向量
    private Map<String, Double> textToVector(String text) {
        List<Term> termList = HanLP.segment(text);
        List<String> words = new ArrayList<>();
        for (Term term : termList) {
            String word = term.word;
            if (!StopWordsLoader.isStopWord(word) && word.length() > 1) {
                words.add(word);
            }
        }

        Map<String, Integer> tfMap = new HashMap<>();
        for (String w : words) {
            tfMap.put(w, tfMap.getOrDefault(w, 0) + 1);
        }

        int maxTf = 0;
        for (int cnt : tfMap.values()) {
            if (cnt > maxTf) maxTf = cnt;
        }

        Map<String, Double> vector = new HashMap<>();
        for (Map.Entry<String, Integer> entry : tfMap.entrySet()) {
            String word = entry.getKey();
            double tf = (double) entry.getValue() / maxTf;
            double idf = corpusManager.getIdf(word);
            vector.put(word, tf * idf);
        }
        return vector;
    }

    // 余弦相似度（岗位和简历）
    private double cosineSimilarity(Map<String, Double> vecA, Map<String, Double> vecB) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(vecA.keySet());
        allKeys.addAll(vecB.keySet());

        double dot = 0.0;// 点积（两个向量“方向一致”的程度）
        double normA = 0.0;// 岗位向量的长度
        double normB = 0.0;// 简历向量的长度
        for (String key : allKeys) {
            double a = vecA.getOrDefault(key, 0.0);
            double b = vecB.getOrDefault(key, 0.0);
            dot += a * b;// 点积：越相似越大
            normA += a * a;
            normB += b * b;
        }
        if (normA == 0 || normB == 0) return 0;
        return (dot / (Math.sqrt(normA) * Math.sqrt(normB))) * 100;
    }

    // 微调加分
    private int getExtraScore(String jobText, String resumeText) {
        int extra = 0;

        // 1. 核心技能强加权（每个词加10分）
        List<String> coreSkills = Arrays.asList("vue 3", "typescript", "llm 应用", "性能优化", "协同编辑", "前端架构");
        for (String skill : coreSkills) {
            if (jobText.contains(skill) && resumeText.contains(skill)) {
                extra += 10;
            }
        }

        // 2. 学历匹配加分
        if (jobText.contains("本科") && resumeText.contains("本科")) {
            extra += 8;
        }

        // 3. 工作年限匹配加分（3-5年 与 4.5年 视为匹配）
        if ((jobText.contains("3-5年") || jobText.contains("3年以上"))
                && resumeText.contains("4.5年")) {
            extra += 8;
        }

        // 加分上限提高到 30 分
        return Math.min(extra, 30);
    }

    /**
     * 计算匹配度（带缓存）
     */
    public double calculateMatch(String jobText, String resumeText) {
        String cacheKey = jobText.hashCode() + "_" + resumeText.hashCode();

        Double cached = matchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Map<String, Double> jobVec = textToVector(jobText);
        Map<String, Double> resumeVec = textToVector(resumeText);
        double baseScore = cosineSimilarity(jobVec, resumeVec);

        // 基础分 0~70
        double baseScore100 = baseScore * 70;

        // 加分 0~20（封顶！！！）
        int extra = getExtraScore(jobText, resumeText);
        extra = Math.min(extra, 20);

        // 最终分 0~90
        double finalScore = baseScore100 + extra;

        // 最高 95，避免 100 分泛滥
        finalScore = Math.min(finalScore, 95);

        matchCache.put(cacheKey, finalScore);
        return finalScore;
    }

    // 根据分数返回等级
    public String getMatchLevel(double score) {
        if (score >= 70) {
            return "高潜力";
        } else if (score >= 40) {
            return "中潜力";
        } else {
            return "低潜力";
        }
    }

    //删除缓存
    public void clearCache(String jobText, String resumeText) {
        String cacheKey = jobText.hashCode() + "_" + resumeText.hashCode();
        matchCache.remove(cacheKey);
    }
}