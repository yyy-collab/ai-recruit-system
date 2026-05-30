package com.recruit.airecruitsystem.utils;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

@Component
public class StopWordsLoader {
    private static final Set<String> STOP_WORDS = new HashSet<>();

    // 应用启动完成后自动加载停用词，无需 @PostConstruct
    @EventListener(ApplicationReadyEvent.class)
    public void loadStopWords() {
        try {
            ClassPathResource resource = new ClassPathResource("stopwords.txt");
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        STOP_WORDS.add(line);
                    }
                }
            }
            System.out.println("加载停用词数量：" + STOP_WORDS.size());
        } catch (Exception e) {
            System.err.println("加载停用词文件失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    public static boolean isStopWord(String word) {
        return STOP_WORDS.contains(word);
    }
}