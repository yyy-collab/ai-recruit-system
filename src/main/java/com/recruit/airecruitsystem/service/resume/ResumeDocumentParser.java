package com.recruit.airecruitsystem.service.resume;

import com.recruit.airecruitsystem.model.ResumeAnalysisSnapshot;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ResumeDocumentParser {

    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)(?:\\+?86[-\\s]?)?1[3-9]\\d(?:[-\\s]?\\d{4}){2}(?!\\d)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern AGE_PATTERN = Pattern.compile("(?i)(?:年龄|age)\\s*[:：]?\\s*(1[6-9]|[2-5]\\d|6[0-5])\\s*(?:岁|周岁)?");
    private static final Pattern BIRTH_PATTERN = Pattern.compile("(?:出生(?:年月|日期)?|生日)\\s*[:：]?\\s*((?:19|20)\\d{2})(?:[年./-]\\s*(\\d{1,2}))?");
    private static final Pattern WORK_RANGE_PATTERN = Pattern.compile(
            "(?i)((?:19|20)\\d{2}(?:[.\\-/年]\\s*\\d{1,2})?\\s*(?:年|月)?\\s*(?:-|~|至|到|—|–)\\s*(?:(?:19|20)\\d{2}(?:[.\\-/年]\\s*\\d{1,2})?\\s*(?:年|月)?|至今|现在|present|current))"
    );

    private static final List<String> SECTION_HEADERS = List.of(
            "基本信息", "个人信息", "求职意向", "教育经历", "教育背景", "专业技能", "技能专长",
            "核心技能", "工作经历", "工作经验", "实习经历", "项目经历", "项目经验", "自我评价",
            "个人评价", "荣誉奖项", "证书"
    );

    private static final List<String> SKILL_DICTIONARY = List.of(
            "Java", "Spring Boot", "SpringBoot", "Spring Cloud", "MyBatis", "MySQL", "Redis",
            "Kafka", "RabbitMQ", "Elasticsearch", "Docker", "Kubernetes", "K8s", "Linux",
            "Git", "Maven", "Gradle", "Nginx", "Dubbo", "Netty", "JPA", "Hibernate",
            "Oracle", "PostgreSQL", "MongoDB", "SQL", "Python", "Go", "Golang", "C++",
            "C#", "JavaScript", "TypeScript", "Vue", "Vue 3", "React", "Angular", "Node.js",
            "HTML", "CSS", "Sass", "Less", "Webpack", "Vite", "UniApp", "Flutter",
            "Android", "iOS", "TensorFlow", "PyTorch", "机器学习", "深度学习", "数据分析",
            "数据挖掘", "大数据", "Hadoop", "Spark", "Flink", "Hive", "微服务", "分布式",
            "高并发", "性能优化", "RESTful", "GraphQL", "DevOps", "Jenkins", "CI/CD",
            "产品设计", "需求分析", "用户研究", "项目管理", "Axure", "Figma", "墨刀"
    );

    private static final String PREVIEW_FONT_STACK = "'Microsoft YaHei','SimSun','SimHei','FangSong','KaiTi','Arial'";

    public ResumeAnalysisSnapshot parse(Path file, String extension, String originalFileName) throws IOException {
        String text = normalizeText(extractText(file, extension));
        if (!StringUtils.hasText(text)) {
            throw new IOException("Apache POI未提取到简历文本");
        }

        List<String> skills = extractSkills(text);
        return ResumeAnalysisSnapshot.builder()
                .basicInfo(extractBasicInfo(text, originalFileName))
                .workExperience(extractWorkExperience(text))
                .skills(skills)
                .workHistory(extractWorkHistory(text, skills))
                .build();
    }

    public String extractPreviewText(Path file, String extension) throws IOException {
        String text = normalizeText(extractText(file, extension));
        if (!StringUtils.hasText(text)) {
            throw new IOException("Apache POI未提取到简历文本");
        }
        return text;
    }

    public String extractPreviewHtml(Path file, String extension, String fileName) throws IOException {
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        return switch (normalizedExtension) {
            case "docx" -> renderDocxHtml(file, fileName);
            case "doc" -> renderDocHtml(file, fileName);
            default -> wrapHtmlDocument(fileName, "<p>暂不支持该格式的在线预览</p>");
        };
    }

    private String extractText(Path file, String extension) throws IOException {
        String normalizedExtension = extension == null ? "" : extension.toLowerCase(Locale.ROOT);
        return switch (normalizedExtension) {
            case "docx" -> extractDocxText(file);
            case "doc" -> extractDocText(file);
            case "txt" -> Files.readString(file, StandardCharsets.UTF_8);
            default -> throw new IOException("仅支持DOC/DOCX格式简历解析");
        };
    }

    private String extractDocxText(Path file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file);
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDocText(Path file) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file);
             HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String renderDocxHtml(Path file, String fileName) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file);
             XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder body = new StringBuilder();
            for (IBodyElement element : document.getBodyElements()) {
                if (element.getElementType() == BodyElementType.PARAGRAPH) {
                    appendDocxParagraphHtml(body, (XWPFParagraph) element);
                    continue;
                }
                if (element.getElementType() == BodyElementType.TABLE) {
                    appendDocxTableHtml(body, (XWPFTable) element);
                }
            }
            if (body.isEmpty()) {
                body.append("<p>暂无可预览内容</p>");
            }
            return wrapHtmlDocument(fileName, body.toString());
        }
    }

    private String renderDocHtml(Path file, String fileName) throws IOException {
        try (InputStream inputStream = Files.newInputStream(file);
             HWPFDocument document = new HWPFDocument(inputStream)) {
            StringBuilder body = new StringBuilder();
            Range range = document.getRange();
            for (int i = 0; i < range.numParagraphs(); i++) {
                appendDocParagraphHtml(body, range.getParagraph(i));
            }
            if (body.isEmpty()) {
                body.append("<p>暂无可预览内容</p>");
            }
            return wrapHtmlDocument(fileName, body.toString());
        }
    }

    private void appendDocxParagraphHtml(StringBuilder body, XWPFParagraph paragraph) {
        StringBuilder text = new StringBuilder();
        for (XWPFRun run : paragraph.getRuns()) {
            String runText = run.text();
            if (!StringUtils.hasText(runText)) {
                continue;
            }
            text.append("<span style=\"")
                    .append(buildDocxRunStyle(run))
                    .append("\">")
                    .append(escapeHtml(runText))
                    .append("</span>");
        }
        if (text.isEmpty()) {
            String fallbackText = paragraph.getText();
            if (!StringUtils.hasText(fallbackText)) {
                return;
            }
            text.append(escapeHtml(fallbackText));
        }

        body.append("<p style=\"")
                .append(buildDocxParagraphStyle(paragraph))
                .append("\">")
                .append(text)
                .append("</p>");
    }

    private void appendDocxTableHtml(StringBuilder body, XWPFTable table) {
        body.append("<table style=\"")
                .append(buildDocxTableStyle())
                .append("\">");
        for (XWPFTableRow row : table.getRows()) {
            body.append("<tr>");
            for (XWPFTableCell cell : row.getTableCells()) {
                body.append("<td");
                int colspan = resolveDocxGridSpan(cell);
                if (colspan > 1) {
                    body.append(" colspan=\"").append(colspan).append("\"");
                }
                body.append(" style=\"")
                        .append(buildDocxTableCellStyle(cell))
                        .append("\">");

                StringBuilder cellContent = new StringBuilder();
                for (XWPFParagraph paragraph : cell.getParagraphs()) {
                    appendDocxParagraphHtml(cellContent, paragraph);
                }
                if (cellContent.isEmpty()) {
                    cellContent.append("&nbsp;");
                }
                body.append(cellContent).append("</td>");
            }
            body.append("</tr>");
        }
        body.append("</table>");
    }

    private void appendDocParagraphHtml(StringBuilder body, Paragraph paragraph) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < paragraph.numCharacterRuns(); i++) {
            CharacterRun run = paragraph.getCharacterRun(i);
            String runText = cleanDocRunText(run.text());
            if (!StringUtils.hasText(runText)) {
                continue;
            }
            text.append("<span style=\"")
                    .append(buildDocRunStyle(run))
                    .append("\">")
                    .append(escapeHtml(runText))
                    .append("</span>");
        }
        if (text.isEmpty()) {
            String fallbackText = cleanDocRunText(paragraph.text());
            if (!StringUtils.hasText(fallbackText)) {
                return;
            }
            text.append(escapeHtml(fallbackText));
        }

        body.append("<p style=\"")
                .append(buildParagraphStyle(mapDocAlignment(paragraph.getJustification()), 1.5))
                .append("\">")
                .append(text)
                .append("</p>");
    }

    private String buildDocxRunStyle(XWPFRun run) {
        StringBuilder style = new StringBuilder();
        if (run.isBold()) {
            style.append("font-weight:700;");
        }
        if (run.isItalic()) {
            style.append("font-style:italic;");
        }
        if (StringUtils.hasText(run.getColor())) {
            style.append("color:#").append(run.getColor()).append(';');
        }
        appendPreviewFontFamily(style, run.getFontFamily());
        if (run.getFontSize() > 0) {
            style.append("font-size:").append(run.getFontSize()).append("pt;");
        }
        return style.toString();
    }

    private String buildDocRunStyle(CharacterRun run) {
        StringBuilder style = new StringBuilder();
        if (run.isBold()) {
            style.append("font-weight:700;");
        }
        if (run.isItalic()) {
            style.append("font-style:italic;");
        }
        appendPreviewFontFamily(style, run.getFontName());
        if (run.getFontSize() > 0) {
            style.append("font-size:").append(run.getFontSize() / 2).append("pt;");
        }
        return style.toString();
    }

    private String buildDocxParagraphStyle(XWPFParagraph paragraph) {
        StringBuilder style = new StringBuilder(buildParagraphStyle(
                paragraph.getAlignment() == null ? null : paragraph.getAlignment().name(),
                paragraph.getSpacingBetween() > 0 ? paragraph.getSpacingBetween() : 1.5
        ));
        appendTwipCss(style, "margin-left", paragraph.getIndentationLeft());
        appendTwipCss(style, "margin-right", paragraph.getIndentationRight());
        if (paragraph.getIndentationFirstLine() > 0) {
            style.append("text-indent:")
                    .append(toPointValue(paragraph.getIndentationFirstLine()))
                    .append("pt;");
        }
        return style.toString();
    }

    private String buildDocxTableStyle() {
        return "width:100%;"
                + "margin:0 0 16px;"
                + "border-collapse:collapse;"
                + "table-layout:fixed;";
    }

    private String buildDocxTableCellStyle(XWPFTableCell cell) {
        StringBuilder style = new StringBuilder("border:1px solid #dce3ef;padding:10px 12px;vertical-align:top;");
        if (StringUtils.hasText(cell.getColor())) {
            style.append("background:#").append(cell.getColor()).append(';');
        }
        return style.toString();
    }

    private int resolveDocxGridSpan(XWPFTableCell cell) {
        var tcPr = cell.getCTTc().getTcPr();
        if (tcPr == null || !tcPr.isSetGridSpan() || tcPr.getGridSpan() == null || tcPr.getGridSpan().getVal() == null) {
            return 1;
        }
        return Math.max(1, tcPr.getGridSpan().getVal().intValue());
    }

    private void appendTwipCss(StringBuilder style, String property, int twips) {
        if (twips <= 0) {
            return;
        }
        style.append(property)
                .append(':')
                .append(toPointValue(twips))
                .append("pt;");
    }

    private double toPointValue(int twips) {
        return twips / 20.0;
    }

    private String buildParagraphStyle(String alignment, double lineHeight) {
        String safeAlignment = switch (alignment == null ? "" : alignment.toUpperCase(Locale.ROOT)) {
            case "CENTER" -> "center";
            case "RIGHT" -> "right";
            case "BOTH", "DISTRIBUTE" -> "justify";
            default -> "left";
        };
        return "margin:0 0 12px;"
                + "line-height:" + lineHeight + ";"
                + "text-align:" + safeAlignment + ";"
                + "white-space:pre-wrap;"
                + "word-break:break-word;";
    }

    private String mapDocAlignment(int justification) {
        return switch (justification) {
            case 1 -> "CENTER";
            case 2 -> "RIGHT";
            case 3, 4, 5 -> "BOTH";
            default -> "LEFT";
        };
    }

    private String cleanDocRunText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\u0007", "")
                .replace("\r", "")
                .replace("\u0000", "")
                .trim();
    }

    private String wrapHtmlDocument(String fileName, String body) {
        String safeTitle = escapeHtml(StringUtils.hasText(fileName) ? fileName : "简历预览");
        return """
                <!DOCTYPE html>
                <html lang="zh-CN">
                <head>
                  <meta charset="UTF-8" />
                  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                  <title>%s</title>
                  <style>
                    body {
                      margin: 0;
                      background: #eef2f7;
                      color: #172033;
                      font-family: "Microsoft YaHei", "SimSun", "SimHei", "FangSong", "KaiTi", "Arial", sans-serif;
                    }
                    .page {
                      box-sizing: border-box;
                      max-width: 880px;
                      margin: 24px auto;
                      padding: 48px 56px;
                      background: #ffffff;
                      border-radius: 12px;
                      box-shadow: 0 18px 46px rgba(23, 32, 51, 0.12);
                    }
                    .title {
                      margin: 0 0 24px;
                      padding-bottom: 16px;
                      border-bottom: 1px solid #e5eaf1;
                      font-size: 18px;
                      font-weight: 700;
                    }
                    table {
                      width: 100%%;
                    }
                    td p:last-child {
                      margin-bottom: 0;
                    }
                  </style>
                </head>
                <body>
                  <main class="page">
                    <h1 class="title">%s</h1>
                    %s
                  </main>
                </body>
                </html>
                """.formatted(safeTitle, safeTitle, body);
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("\n", "<br/>");
    }

    private void appendPreviewFontFamily(StringBuilder style, String originalFontFamily) {
        style.append("font-family:").append(buildPreviewFontFamily(originalFontFamily)).append(';');
    }

    private String buildPreviewFontFamily(String originalFontFamily) {
        if (!StringUtils.hasText(originalFontFamily)) {
            return PREVIEW_FONT_STACK;
        }

        String escapedFont = "'" + escapeCssFontFamily(originalFontFamily.trim()) + "'";
        String normalized = originalFontFamily.trim().toLowerCase(Locale.ROOT);
        if (isLatinOnlyFont(normalized)) {
            return PREVIEW_FONT_STACK + "," + escapedFont;
        }
        return escapedFont + "," + PREVIEW_FONT_STACK;
    }

    private boolean isLatinOnlyFont(String normalizedFontFamily) {
        return normalizedFontFamily.equals("arial")
                || normalizedFontFamily.equals("calibri")
                || normalizedFontFamily.equals("cambria")
                || normalizedFontFamily.equals("times new roman")
                || normalizedFontFamily.equals("helvetica")
                || normalizedFontFamily.equals("verdana")
                || normalizedFontFamily.equals("tahoma");
    }

    private String escapeCssFontFamily(String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    private ResumeAnalysisSnapshot.BasicInfo extractBasicInfo(String text, String originalFileName) {
        return ResumeAnalysisSnapshot.BasicInfo.builder()
                .realName(firstText(
                        findLabeledValue(text, "姓名", "Name"),
                        extractNameFromFileName(originalFileName),
                        extractNameFromFirstLines(text)
                ))
                .phone(extractPhone(text))
                .email(extractEmail(text))
                .age(extractAge(text))
                .eduBack(extractEducation(text))
                .almaMater(extractSchool(text))
                .build();
    }

    private String extractPhone(String text) {
        Matcher matcher = PHONE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        String digits = matcher.group().replaceAll("\\D", "");
        return digits.startsWith("86") && digits.length() == 13 ? digits.substring(2) : digits;
    }

    private String extractEmail(String text) {
        Matcher matcher = EMAIL_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }

    private Integer extractAge(String text) {
        Matcher ageMatcher = AGE_PATTERN.matcher(text);
        if (ageMatcher.find()) {
            return Integer.parseInt(ageMatcher.group(1));
        }

        Matcher birthMatcher = BIRTH_PATTERN.matcher(text);
        if (!birthMatcher.find()) {
            return null;
        }

        int year = Integer.parseInt(birthMatcher.group(1));
        int month = birthMatcher.group(2) == null ? 1 : Integer.parseInt(birthMatcher.group(2));
        LocalDate now = LocalDate.now();
        int age = now.getYear() - year;
        if (month > now.getMonthValue()) {
            age--;
        }
        return age >= 16 && age <= 65 ? age : null;
    }

    private String extractEducation(String text) {
        String labeled = findLabeledValue(text, "学历", "最高学历", "教育程度", "Education");
        String normalized = normalizeEducation(labeled);
        if (normalized != null) {
            return normalized;
        }

        String educationSection = findSection(text, "教育经历|教育背景");
        normalized = normalizeEducation(educationSection);
        return normalized != null ? normalized : normalizeEducation(text);
    }

    private String normalizeEducation(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        if (value.contains("博士")) {
            return "博士";
        }
        if (value.contains("硕士") || value.contains("研究生") || value.toUpperCase(Locale.ROOT).contains("MBA")) {
            return "硕士";
        }
        if (value.contains("本科") || value.contains("学士")) {
            return "本科";
        }
        if (value.contains("大专") || value.contains("专科")) {
            return "专科";
        }
        return null;
    }

    private String extractSchool(String text) {
        String labeled = findLabeledValue(text, "毕业院校", "毕业学校", "学校", "院校", "School");
        String school = findSchoolName(labeled);
        if (school != null) {
            return school;
        }

        String educationSection = findSection(text, "教育经历|教育背景");
        school = findSchoolName(educationSection);
        return school != null ? school : findSchoolName(text);
    }

    private String findSchoolName(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Matcher matcher = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9（）()·\\s]{2,40}(?:大学|学院|学校|研究院|University|College|Institute))").matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return cleanValue(matcher.group(1));
    }

    private String extractWorkExperience(String text) {
        Matcher labeledMatcher = Pattern.compile(
                "(?:工作年限|工作经验|从业年限|经验年限)\\s*[:：]?\\s*([^\\n，,；;]{1,24})"
        ).matcher(text);
        if (labeledMatcher.find()) {
            String value = normalizeWorkExperience(labeledMatcher.group(1));
            if (value != null) {
                return value;
            }
        }

        Matcher experienceMatcher = Pattern.compile("([0-9]{1,2}(?:\\.[0-9])?\\s*(?:\\+\\s*)?年(?:以上|左右)?)(?:\\s*(?:工作)?经验)?").matcher(text);
        if (experienceMatcher.find()) {
            return normalizeWorkExperience(experienceMatcher.group(1));
        }

        Integer calculatedYears = calculateYearsFromDateRanges(text);
        return calculatedYears == null ? null : calculatedYears + "年";
    }

    private String normalizeWorkExperience(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        Matcher matcher = Pattern.compile("([0-9]{1,2}(?:\\.[0-9])?\\s*(?:\\+\\s*)?年(?:以上|左右)?)").matcher(value);
        return matcher.find() ? matcher.group(1).replaceAll("\\s+", "") : null;
    }

    private List<String> extractSkills(String text) {
        LinkedHashSet<String> skills = new LinkedHashSet<>();
        addDictionarySkills(skills, text);

        String skillSection = findSection(text, "专业技能|技能专长|核心技能|技能|技术栈|IT技能");
        if (StringUtils.hasText(skillSection)) {
            Arrays.stream(skillSection.split("[,，、/|；;\\n\\r]+"))
                    .map(this::cleanSkillToken)
                    .filter(this::looksLikeSkill)
                    .forEach(skills::add);
            addDictionarySkills(skills, skillSection);
        }

        return limitList(new ArrayList<>(skills), 40);
    }

    private void addDictionarySkills(Set<String> skills, String text) {
        String lowerText = text.toLowerCase(Locale.ROOT);
        for (String skill : SKILL_DICTIONARY) {
            if (lowerText.contains(skill.toLowerCase(Locale.ROOT))) {
                skills.add(skill);
            }
        }
    }

    private String cleanSkillToken(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return cleanValue(value)
                .replaceAll("^(熟悉|熟练掌握|掌握|精通|了解|具备|使用)", "")
                .replaceAll("(等技术|等)$", "")
                .trim();
    }

    private boolean looksLikeSkill(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        if (value.length() < 2 || value.length() > 30) {
            return false;
        }
        return !value.matches(".*(负责|项目|经历|经验|公司|岗位|职位|描述|以上|以下).*");
    }

    private List<ResumeAnalysisSnapshot.WorkHistoryItem> extractWorkHistory(String text, List<String> resumeSkills) {
        String workSection = findSection(text, "工作经历|工作经验|实习经历");
        String source = StringUtils.hasText(workSection) ? workSection : text;

        List<ResumeAnalysisSnapshot.WorkHistoryItem> items = new ArrayList<>();
        List<MatcherMatch> matches = findDateRangeMatches(source);
        for (int i = 0; i < matches.size(); i++) {
            MatcherMatch current = matches.get(i);
            int blockStart = Math.max(0, source.lastIndexOf('\n', current.start()) + 1);
            int blockEnd = i + 1 < matches.size()
                    ? Math.max(current.end(), source.lastIndexOf('\n', matches.get(i + 1).start()) + 1)
                    : source.length();
            String block = source.substring(blockStart, blockEnd).trim();
            ResumeAnalysisSnapshot.WorkHistoryItem item = parseWorkHistoryBlock(block, current.value(), resumeSkills);
            if (item != null) {
                items.add(item);
            }
        }
        return limitList(items, 10);
    }

    private ResumeAnalysisSnapshot.WorkHistoryItem parseWorkHistoryBlock(String block, String dateRange, List<String> resumeSkills) {
        if (!StringUtils.hasText(block)) {
            return null;
        }

        String[] rangeParts = dateRange.split("\\s*(?:-|~|至|到|—|–)\\s*", 2);
        String startTime = rangeParts.length > 0 ? cleanDate(rangeParts[0]) : null;
        String endTime = rangeParts.length > 1 ? cleanDate(rangeParts[1]) : null;
        String detail = cleanValue(block.replace(dateRange, " "));
        String company = firstText(
                findLabeledValue(detail, "公司", "单位", "企业"),
                findCompanyName(detail)
        );
        String position = firstText(
                findLabeledValue(detail, "职位", "岗位", "职务"),
                findPositionName(detail)
        );
        String description = buildDescription(detail, company, position);

        if (!StringUtils.hasText(company) && !StringUtils.hasText(position) && !StringUtils.hasText(description)) {
            return null;
        }

        return ResumeAnalysisSnapshot.WorkHistoryItem.builder()
                .company(company)
                .position(position)
                .startTime(startTime)
                .endTime(endTime)
                .description(description)
                .coreSkills(extractCoreSkills(block, resumeSkills))
                .build();
    }

    private List<String> extractCoreSkills(String block, List<String> resumeSkills) {
        LinkedHashSet<String> coreSkills = new LinkedHashSet<>();
        String lowerBlock = block.toLowerCase(Locale.ROOT);
        for (String skill : resumeSkills) {
            if (lowerBlock.contains(skill.toLowerCase(Locale.ROOT))) {
                coreSkills.add(skill);
            }
        }
        addDictionarySkills(coreSkills, block);
        return limitList(new ArrayList<>(coreSkills), 8);
    }

    private List<MatcherMatch> findDateRangeMatches(String text) {
        List<MatcherMatch> matches = new ArrayList<>();
        Matcher matcher = WORK_RANGE_PATTERN.matcher(text);
        while (matcher.find()) {
            matches.add(new MatcherMatch(matcher.start(), matcher.end(), matcher.group(1)));
        }
        return matches;
    }

    private Integer calculateYearsFromDateRanges(String text) {
        Matcher matcher = WORK_RANGE_PATTERN.matcher(text);
        Integer earliestYear = null;
        while (matcher.find()) {
            Matcher yearMatcher = Pattern.compile("(?:19|20)\\d{2}").matcher(matcher.group(1));
            if (yearMatcher.find()) {
                int year = Integer.parseInt(yearMatcher.group());
                earliestYear = earliestYear == null ? year : Math.min(earliestYear, year);
            }
        }
        if (earliestYear == null) {
            return null;
        }
        return Math.max(0, LocalDate.now().getYear() - earliestYear);
    }

    private String findCompanyName(String detail) {
        Matcher matcher = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z0-9（）()·.\\s]{2,50}(?:公司|集团|科技|银行|中心|研究院|工作室|Ltd|Inc|LLC))").matcher(detail);
        return matcher.find() ? cleanValue(matcher.group(1)) : null;
    }

    private String findPositionName(String detail) {
        Matcher matcher = Pattern.compile("(Java开发工程师|后端开发工程师|前端开发工程师|全栈开发工程师|测试工程师|算法工程师|数据分析师|产品经理|项目经理|运营经理|设计师|工程师|经理|主管|实习生)").matcher(detail);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String buildDescription(String detail, String company, String position) {
        String description = detail;
        if (StringUtils.hasText(company)) {
            description = description.replace(company, " ");
        }
        if (StringUtils.hasText(position)) {
            description = description.replace(position, " ");
        }
        description = cleanValue(description)
                .replaceAll("^(公司|单位|企业|职位|岗位|职务)\\s*[:：]", "")
                .trim();
        if (!StringUtils.hasText(description)) {
            return null;
        }
        return description.length() > 500 ? description.substring(0, 500) : description;
    }

    private String findSection(String text, String headerRegex) {
        Matcher startMatcher = Pattern.compile("(?im)^\\s*(?:" + headerRegex + ")\\s*[:：]?\\s*$").matcher(text);
        if (!startMatcher.find()) {
            return null;
        }

        int start = startMatcher.end();
        int end = text.length();
        Matcher nextMatcher = Pattern.compile("(?im)^\\s*(?:" + String.join("|", SECTION_HEADERS) + ")\\s*[:：]?\\s*$").matcher(text);
        while (nextMatcher.find(start)) {
            if (nextMatcher.start() > start) {
                end = nextMatcher.start();
                break;
            }
            start = nextMatcher.end();
        }
        return text.substring(start, end).trim();
    }

    private String findLabeledValue(String text, String... labels) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String labelRegex = String.join("|", labels);
        Matcher matcher = Pattern.compile("(?im)(?:^|\\n)\\s*(?:" + labelRegex + ")\\s*[:：]?\\s*([^\\n；;，,|/]{1,80})").matcher(text);
        return matcher.find() ? cleanValue(matcher.group(1)) : null;
    }

    private String extractNameFromFileName(String originalFileName) {
        if (!StringUtils.hasText(originalFileName)) {
            return null;
        }
        String name = originalFileName.replaceFirst("\\.[^.]+$", "");
        Matcher matcher = Pattern.compile("([\\u4e00-\\u9fa5·]{2,4})").matcher(name);
        while (matcher.find()) {
            String candidate = matcher.group(1);
            if (looksLikeChineseName(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String extractNameFromFirstLines(String text) {
        String[] lines = text.split("\\R");
        for (int i = 0; i < Math.min(lines.length, 8); i++) {
            String line = cleanValue(lines[i]);
            if (looksLikeChineseName(line)) {
                return line;
            }
        }
        return null;
    }

    private boolean looksLikeChineseName(String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        String compact = value.replaceAll("\\s+", "");
        if (!compact.matches("[\\u4e00-\\u9fa5·]{2,4}")) {
            return false;
        }
        return !compact.matches(".*(简历|个人|姓名|求职|应聘|电话|邮箱|学校|大学|学院|工作|项目|技能).*");
    }

    private String cleanDate(String value) {
        return cleanValue(value)
                .replace("年", ".")
                .replace("月", "")
                .replaceAll("\\s+", "")
                .replaceAll("\\.$", "");
    }

    private String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        return value.replace('\u00A0', ' ')
                .replaceAll("[\\t ]+", " ")
                .replaceAll("^[：:，,；;|/\\-\\s]+", "")
                .replaceAll("[：:，,；;|/\\-\\s]+$", "")
                .trim();
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u00A0', ' ')
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t ]+", " ")
                .replaceAll("(?m)^\\s+|\\s+$", "")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    @SafeVarargs
    private final <T> T firstText(T... values) {
        for (T value : values) {
            if (value instanceof String textValue) {
                if (StringUtils.hasText(textValue)) {
                    return value;
                }
            } else if (value != null) {
                return value;
            }
        }
        return null;
    }

    private <T> List<T> limitList(List<T> values, int limit) {
        if (values.size() <= limit) {
            return values;
        }
        return new ArrayList<>(values.subList(0, limit));
    }

    private record MatcherMatch(int start, int end, String value) {
    }
}
