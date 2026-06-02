package com.recruit.airecruitsystem.utils;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class SimilarityUtil {

    // 余弦相似度算法
    public static double cosineSimilarity(List<String> keywords1, List<String> keywords2) {
        Map<String, Integer> freq1 = getTermFrequency(keywords1);
        Map<String, Integer> freq2 = getTermFrequency(keywords2);

        double dotProduct = 0.0;
        for (String key : freq1.keySet()) {
            if (freq2.containsKey(key)) {
                dotProduct += freq1.get(key) * freq2.get(key);
            }
        }

        double mag1 = Math.sqrt(freq1.values().stream().mapToInt(x -> x * x).sum());
        double mag2 = Math.sqrt(freq2.values().stream().mapToInt(x -> x * x).sum());

        if (mag1 == 0 || mag2 == 0) return 0.0;
        return dotProduct / (mag1 * mag2);
    }

    private static Map<String, Integer> getTermFrequency(List<String> list) {
        Map<String, Integer> freq = new HashMap<>();
        for (String s : list) {
            if (s == null || s.isBlank()) continue;
            freq.put(s.trim().toLowerCase(), freq.getOrDefault(s.trim().toLowerCase(), 0) + 1);
        }
        return freq;
    }
}