package com.recruit.airecruitsystem.service.ai;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.recruit.airecruitsystem.utils.StopWordsLoader;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class HanLPService {
    public List<String> segment(String text, boolean removeStopWords) {
        List<Term> termList = HanLP.segment(text);
        // 存放最终的词语
        List<String> resultList = new ArrayList<>();
        // 循环遍历每一个分词结果
        for (int i = 0; i < termList.size(); i++) {
            Term term = termList.get(i);
            String word = term.word;
            // 判断是否需要过滤停用词
            if (removeStopWords) {
                // 不是停用词加入结果
                if (!StopWordsLoader.StopWords.contains(word)) {
                    resultList.add(word);
                }
            } else {
                // 不过滤，直接加入
                resultList.add(word);
            }
        }
        return resultList;
    }
}