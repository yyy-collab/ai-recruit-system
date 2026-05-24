package com.recruit.airecruitsystem.service.ai;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.recruit.airecruitsystem.utils.StopWordsLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class KeywordExtractService {

    @Autowired
    private CorpusManager corpusManager;

    /**提取文本的核心关键词（简化版：使用 TF 词频，再乘以 IDF）
     * @param text 输入文本
     * @param limit 关键词数量（最多20）
     * @return 关键词列表（按重要性降序）
     */
    public List<String> extractKeywords(String text, int limit) {
        // 分词并过滤停用词
        List<Term> termList = HanLP.segment(text);
        List<String> words = new ArrayList<>();
        for (Term term : termList) {
            String word = term.word;
            if (!StopWordsLoader.isStopWord(word) && word.length() > 1) {
                words.add(word);
            }
        }

        // 统计词频（TF）
        Map<String, Integer> tfMap = new HashMap<>();
        for (String word : words) {
            int count = tfMap.getOrDefault(word, 0);
            tfMap.put(word, count + 1);
        }

        // 找出最大词频，用于归一化
        int maxTf = 0;
        for (int count : tfMap.values()) {
            if (count > maxTf) maxTf = count;
        }

        // 计算每个词的 TF-IDF 分数
        Map<String, Double> scoreMap = new HashMap<>();
        for (Map.Entry<String, Integer> entry : tfMap.entrySet()) {
            String word = entry.getKey();
            int tf = entry.getValue();
            double normalizedTf = (double) tf / maxTf;   //把出现次数缩放到 0~1 之间
            double idf = corpusManager.getIdf(word);
            double score = normalizedTf * idf; //最终分数 = 出现频率 × 稀有程度
            scoreMap.put(word, score);
        }

        // 按照分数排序，取前 limit 个
        List<Map.Entry<String, Double>> sortedList = new ArrayList<>(scoreMap.entrySet());
        for (int i = 0; i < sortedList.size() - 1; i++) {
            for (int j = i + 1; j < sortedList.size(); j++) {
                if (sortedList.get(i).getValue() < sortedList.get(j).getValue()) {
                    Map.Entry<String, Double> temp = sortedList.get(i);
                    sortedList.set(i, sortedList.get(j));
                    sortedList.set(j, temp);
                }
            }
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < sortedList.size() && i < limit; i++) {
            result.add(sortedList.get(i).getKey());
        }
        return result;
    }
}