package com.recruit.airecruitsystem.service.ai;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;
import com.recruit.airecruitsystem.mapper.JobMapper;
import com.recruit.airecruitsystem.pojo.Job;
import com.recruit.airecruitsystem.utils.StopWordsLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.*;

@Component
public class CorpusManager {
    @Autowired
    private JobMapper jobMapper;

    // 存储所有文档分词后的词语列表，每个文档是一个 List<String>
    private List<List<String>> documents = new ArrayList<>();

    // 存储每个词语的文档频率（DF）
    private Map<String, Integer> docFreq = new HashMap<>();

    // 总文档数
    private int totalDocs = 0;

    @PostConstruct
    public void init() {

        //从数据库加载所有岗位的描述文本
        List<Job> jobs = jobMapper.selectAllOnline();
        if (jobs != null && !jobs.isEmpty()) {
            for (Job job : jobs) {
                // 拼接岗位文本：岗位名称 + 岗位描述 + 任职要求 + 关键词
                String text = (job.getJobName() != null ? job.getJobName() : "")
                        + " " + (job.getJobDesc() != null ? job.getJobDesc() : "")
                        + " " + (job.getRequirement() != null ? job.getRequirement() : "")
                        + " " + (job.getKeywords() != null ? job.getKeywords() : "");
                if (!text.trim().isEmpty()) {
                    addDocument(text);
                }
            }
        }

        //如果没有从数据库读到任何岗位，则用示例文档（防止空语料库）
        if (documents.isEmpty()) {
            System.out.println("警告：数据库无岗位数据，使用示例文档初始化语料库");
            addDocument("Java开发 3年经验 SpringBoot MySQL Redis 后端");
            addDocument("前端开发 Vue React 2年经验 JavaScript CSS");
            addDocument("Python数据分析 机器学习 Pandas NumPy 3年经验");
        }

        // 根据文档集合计算每个词的文档频率（DF）
        for (List<String> docWords : documents) {
            //一篇文档中同一个词只计一次 DF
            Set<String> uniqueWords = new HashSet<>(docWords);
            for (String word : uniqueWords) {
                docFreq.put(word, docFreq.getOrDefault(word, 0) + 1);
            }
        }
        totalDocs = documents.size();
        System.out.println("语料库初始化完成，文档数：" + totalDocs + "，不同词数：" + docFreq.size());
        List<Job> jobss = jobMapper.selectAllOnline();
        System.out.println("数据库查询到岗位数量：" + (jobss == null ? 0 : jobss.size()));
        if (jobss != null && !jobss.isEmpty()) {
            for (Job job : jobss) {
                System.out.println("岗位ID：" + job.getId() + ", 名称：" + job.getJobName());
            }
        }
    }

    // 将一篇文本分词后加入语料库
    private void addDocument(String text) {
        List<String> words = segmentText(text);
        if (!words.isEmpty()) {
            documents.add(words);
        }
    }

    // 对文本进行分词并过滤停用词（保留长度>1的词）
    private List<String> segmentText(String text) {
        List<Term> termList = HanLP.segment(text);
        List<String> words = new ArrayList<>();
        for (Term term : termList) {
            String word = term.word;
            if (!StopWordsLoader.isStopWord(word) && word.length() > 1) {
                words.add(word);
            }
        }
        return words;
    }

    // 获取某个词的逆文档频率（IDF）
    public double getIdf(String word) {
        Integer df = docFreq.get(word);
        if (df == null || df == 0) {
            // 原逻辑：return Math.log((totalDocs + 1) / 1.0);
            // 优化：固定最小总文档数为20，避免totalDocs过小时IDF异常
            int minTotalDocs = Math.max(totalDocs, 20);
            return Math.log((minTotalDocs + 1) / 1.0);
        }
        // 优化：同样兜底总文档数
        int minTotalDocs = Math.max(totalDocs, 20);
        return Math.log((double) minTotalDocs / (1 + df));
    }
}