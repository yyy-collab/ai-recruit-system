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

    // 余弦相似度
    private double cosineSimilarity(Map<String, Double> vecA, Map<String, Double> vecB) {
        Set<String> allKeys = new HashSet<>();
        allKeys.addAll(vecA.keySet());
        allKeys.addAll(vecB.keySet());

        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (String key : allKeys) {
            double a = vecA.getOrDefault(key, 0.0);
            double b = vecB.getOrDefault(key, 0.0);
            dot += a * b;
            normA += a * a;
            normB += b * b;
        }
        if (normA == 0 || normB == 0) return 0;
        return (dot / (Math.sqrt(normA) * Math.sqrt(normB))) * 100;
    }

    // 微调加分
    private int getExtraScore(String jobText, String resumeText) {
        int extra = 0;
        if (jobText.contains("本科") && resumeText.contains("本科")) extra += 3;
        if (jobText.contains("硕士") && resumeText.contains("硕士")) extra += 3;
        if (jobText.contains("3年") && resumeText.contains("3年")) extra += 2;
        return Math.min(extra, 10);
    }

    /**
     * 计算匹配度（带缓存）
     */
    public double calculateMatch(String jobText, String resumeText) {
        // 生成缓存 key
        String cacheKey = jobText.hashCode() + "_" + resumeText.hashCode();

        // 1. 查缓存
        Double cached = matchCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // 2. 计算匹配度
        Map<String, Double> jobVec = textToVector(jobText);
        Map<String, Double> resumeVec = textToVector(resumeText);
        double baseScore = cosineSimilarity(jobVec, resumeVec);
        int extra = getExtraScore(jobText, resumeText);
        double finalScore = baseScore + extra;
        if (finalScore > 100) finalScore = 100;

        // 3. 存入缓存
        matchCache.put(cacheKey, finalScore);

        return finalScore;
    }

    // 根据分数返回等级
    public String getMatchLevel(double score) {
        if (score >= 90) return "极高潜力";
        if (score >= 75) return "高潜力";
        if (score >= 60) return "中等潜力";
        return "低潜力";
    }
}