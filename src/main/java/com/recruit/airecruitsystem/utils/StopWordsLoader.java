package com.recruit.airecruitsystem.utils;

import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

@Component
public class StopWordsLoader {
    // 全局停用词集合
    public static Set<String> StopWords = new HashSet<>();

    // 项目启动自动加载停用词
    @PostConstruct
    public void loadStopWords() {
        try {
            StopWords = new HashSet<>(Files.readAllLines(Paths.get("src/main/resources/stopwords.txt")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //判断是否是停用词
    public static boolean isStopWord(String word) {
        return StopWords.contains(word);
    }
}