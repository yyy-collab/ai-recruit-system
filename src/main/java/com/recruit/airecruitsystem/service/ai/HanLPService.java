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
        List<String> result = new ArrayList<>();
        for (Term term : termList) {
            String word = term.word;
            // 如果需要过滤停用词，并且当前词是停用词，则跳过
            if (removeStopWords && StopWordsLoader.isStopWord(word)) {
                continue;
            }
            result.add(word);
        }
        return result;
    }
}