package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.dto.VocabularyArticleDtos;
import com.secondbrain.entity.VocabularyArticle;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.VocabularyArticleMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.VocabularyArticleService;
import com.secondbrain.util.VisionImageValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** 通过用户视觉模型提取词表，并以程序校验文章真实覆盖。 */
@Service
public class VocabularyArticleServiceImpl implements VocabularyArticleService {
    private static final Logger log = LoggerFactory.getLogger(VocabularyArticleServiceImpl.class);
    private static final int MAX_IMAGES = 8;
    private static final int MAX_WORDS = 80;
    private static final int MAX_PARAGRAPH_REPAIRS = 2;
    private static final long MAX_IMAGE_BYTES = 4L * 1024 * 1024;
    private static final Pattern ENGLISH_WORD = Pattern.compile("[A-Za-z][A-Za-z'-]{0,49}");

    private final AiService aiService;
    private final VocabularyArticleMapper articleMapper;
    private final ObjectMapper objectMapper;

    /**
     * 复用现有用户模型配置与数据库访问，不另建模型密钥存储。
     *
     * @param aiService 用户配置下的 AI 服务
     * @param articleMapper 私有文章数据访问
     * @param objectMapper JSON 解析器
     */
    public VocabularyArticleServiceImpl(AiService aiService, VocabularyArticleMapper articleMapper,
                                        ObjectMapper objectMapper) {
        this.aiService = aiService;
        this.articleMapper = articleMapper;
        this.objectMapper = objectMapper;
    }

    /** {@inheritDoc} */
    @Override
    public VocabularyArticleDtos.VisionReadiness visionReady(Long userId) {
        try {
            aiService.resolveVisionConfig(userId, "vision");
            return new VocabularyArticleDtos.VisionReadiness(true, "配置检查通过，可以提取截图中的单词");
        } catch (BusinessException exception) {
            return new VocabularyArticleDtos.VisionReadiness(false, exception.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public VocabularyArticleDtos.Extraction extract(List<MultipartFile> files, Long userId) {
        if (files == null || files.isEmpty() || files.size() > MAX_IMAGES) {
            throw new BusinessException(400, "请选择 1 到 8 张单词截图");
        }
        List<byte[]> images = new ArrayList<>();
        List<String> mimeTypes = new ArrayList<>();
        long totalBytes = 0;
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty() || file.getSize() > MAX_IMAGE_BYTES) {
                throw new BusinessException(400, "每张截图不能超过 4 MB");
            }
            String mime = file.getContentType();
            if (!("image/jpeg".equals(mime) || "image/png".equals(mime))) {
                throw new BusinessException(400, "词表截图只支持 JPEG 或 PNG");
            }
            try {
                byte[] bytes = file.getBytes();
                VisionImageValidator.validate(bytes, mime);
                totalBytes += bytes.length;
                if (totalBytes > 24L * 1024 * 1024) {
                    throw new BusinessException(400, "截图总大小不能超过 24 MB");
                }
                images.add(bytes);
                mimeTypes.add(mime);
            } catch (IOException exception) {
                throw new BusinessException(400, "截图读取失败，请重新选择");
            }
        }
        String prompt = "图片按传入顺序编号，从 0 开始。只提取单词列表里可见的英文目标词，"
                + "不要提取按钮、中文释义、例句或猜测模糊字母。只返回 JSON："
                + "{\"words\":[{\"word\":\"example\",\"sourceImageIndex\":0,\"uncertain\":false}]}。"
                + "不确定的词标记 uncertain=true，空图返回空数组。";
        String response = aiService.analyzeImages(userId, "vision", prompt, images, mimeTypes);
        JsonNode root = parseObject(response, "word_extraction");
        JsonNode words = root.path("words");
        if (!words.isArray()) {
            throw new BusinessException(502, "模型未返回词表，请重试或更换截图");
        }
        Map<String, VocabularyArticleDtos.CandidateWord> unique = new LinkedHashMap<>();
        for (JsonNode item : words) {
            String word = item.path("word").asText("").trim();
            int source = item.path("sourceImageIndex").asInt(-1);
            if (!ENGLISH_WORD.matcher(word).matches() || source < 0 || source >= files.size()) {
                continue;
            }
            String key = word.toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, new VocabularyArticleDtos.CandidateWord(word, source,
                    item.path("uncertain").asBoolean(false)));
        }
        return new VocabularyArticleDtos.Extraction(List.copyOf(unique.values()));
    }

    /** {@inheritDoc} */
    @Override
    public VocabularyArticleDtos.ArticleView generate(VocabularyArticleDtos.GenerateRequest request, Long userId) {
        if (request == null || request.words() == null) {
            throw new BusinessException(400, "请先确认单词列表");
        }
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String raw : request.words()) {
            String word = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
            if (!ENGLISH_WORD.matcher(word).matches()) {
                throw new BusinessException(400, "词表中存在无效英文单词，请先修改");
            }
            normalized.add(word);
        }
        if (normalized.isEmpty() || normalized.size() > MAX_WORDS) {
            throw new BusinessException(400, "请确认 1 到 80 个不同的英文单词");
        }
        String topic = safePreference(request.topic(), "日常与学习");
        String difficulty = safePreference(request.difficulty(), "中级");
        List<String> words = List.copyOf(normalized);
        String wordCount = words.size() > 25 ? "1000–1400" : "700–1000";
        String prompt = "请先在内部按语义或场景规划目标词分别出现在哪些段落，再写成一篇约 "
                + wordCount + " 个英文单词、连贯自然的完整文章。规划只用于写作，不要输出规划说明或推理过程。"
                + "不要把单词机械堆在一起；每个目标词都必须以给定原形自然出现在某个正文段落中。题材："
                + topic + "；难度：" + difficulty
                + "。为每个目标词提供符合正文语境的简短中文释义。只返回 JSON："
                + "{\"article\":\"完整英文文章，段落之间用\\n\\n分隔\","
                + "\"meanings\":{\"word\":\"中文语境释义\"}}。目标词：" + String.join(", ", words);
        JsonNode result = parseObject(aiService.generateAnswer(userId, "vision", prompt), "article_draft");
        List<String> paragraphs = readParagraphs(result);
        String article = joinParagraphs(paragraphs);
        if (article.isBlank()) {
            throw new BusinessException(502, "模型没有生成文章，请重试");
        }
        Map<String, String> meanings = new LinkedHashMap<>();
        collectMeanings(result.path("meanings"), meanings, words);
        List<String> missing = missingWords(article, words);
        List<String> missingMeanings = missingMeanings(meanings, words);
        for (int attempt = 0; attempt < MAX_PARAGRAPH_REPAIRS
                && (!missing.isEmpty() || !missingMeanings.isEmpty()); attempt++) {
            String repair = paragraphRepairPrompt(topic, difficulty, paragraphs, missing, missingMeanings);
            JsonNode revision = parseObject(aiService.generateAnswer(userId, "vision", repair), "paragraph_repair");
            List<String> revisedParagraphs = missing.isEmpty()
                    ? paragraphs : applyParagraphRepairs(paragraphs, revision.path("replacements"));
            String revised = joinParagraphs(revisedParagraphs);
            List<String> revisedMissing = missingWords(revised, words);
            Map<String, String> revisedMeanings = new LinkedHashMap<>(meanings);
            collectMeanings(revision.path("meanings"), revisedMeanings, words);
            List<String> revisedMissingMeanings = missingMeanings(revisedMeanings, words);
            boolean textImproved = revisedMissing.size() < missing.size();
            boolean meaningsImproved = revisedMissingMeanings.size() < missingMeanings.size();
            if ((!revised.isBlank() && textImproved) || meaningsImproved) {
                if (textImproved) {
                    paragraphs = revisedParagraphs;
                    article = revised;
                }
                meanings = revisedMeanings;
                missing = revisedMissing;
                missingMeanings = revisedMissingMeanings;
            } else {
                break;
            }
        }
        if (!missing.isEmpty() || !missingMeanings.isEmpty()) {
            JsonNode supplement = parseObject(aiService.generateAnswer(userId, "vision",
                    supplementPrompt(topic, difficulty, missing, missingMeanings)), "supplement");
            String supplementText = supplement.path("article").asText("").trim();
            if (!supplementText.isBlank()) {
                article = article + "\n\nSupplementary reading\n\n" + supplementText;
                missing = missingWords(article, words);
            }
            collectMeanings(supplement.path("meanings"), meanings, words);
            missingMeanings = missingMeanings(meanings, words);
        }
        VocabularyArticle saved = new VocabularyArticle();
        saved.setUserId(userId);
        saved.setWordsJson(writeJson(words));
        saved.setArticle(article);
        saved.setMeaningsJson(writeJson(meanings));
        saved.setMissingWordsJson(writeJson(missing));
        saved.setTopic(topic);
        saved.setDifficulty(difficulty);
        saved.setCreateTime(LocalDateTime.now());
        articleMapper.insert(saved);
        return new VocabularyArticleDtos.ArticleView(saved.getId(), words, article, meanings, missing,
                topic, difficulty, saved.getCreateTime() == null ? "" : saved.getCreateTime().toString());
    }

    private List<String> readParagraphs(JsonNode result) {
        List<String> paragraphs = new ArrayList<>();
        JsonNode planned = result.path("paragraphs");
        if (planned.isArray()) {
            for (JsonNode paragraph : planned) {
                String text = paragraph.isTextual() ? paragraph.asText() : paragraph.path("text").asText("");
                if (!text.isBlank()) paragraphs.add(text.trim());
            }
        }
        if (!paragraphs.isEmpty()) return paragraphs;
        String article = result.path("article").asText("").trim();
        if (article.isBlank()) return List.of();
        return article.lines().map(String::trim).filter(line -> !line.isBlank()).toList();
    }

    private String joinParagraphs(List<String> paragraphs) {
        return String.join("\n\n", paragraphs).trim();
    }

    private String paragraphRepairPrompt(String topic, String difficulty, List<String> paragraphs,
                                         List<String> missing, List<String> missingMeanings) {
        List<Map<String, Object>> indexedParagraphs = new ArrayList<>();
        for (int index = 0; index < paragraphs.size(); index++) {
            indexedParagraphs.add(Map.of("paragraphIndex", index, "text", paragraphs.get(index)));
        }
        return "请修复这篇文章目前未覆盖的词汇，只替换确实需要加入缺失词的段落，不要重写全文。"
                + "保持原主题“" + topic + "”和“" + difficulty + "”难度。需要补入正文的目标词必须以给定原形自然出现。"
                + "正文缺词：" + String.join(", ", missing)
                + "。释义缺失词：" + String.join(", ", missingMeanings)
                + "。只返回 JSON：{\"replacements\":[{\"paragraphIndex\":0,\"text\":\"替换后的完整英文段落\"}],"
                + "\"meanings\":{\"word\":\"中文语境释义\"}}。释义应贴合正文所在句子，不要输出推理过程；"
                + "不需要修改的段落不要返回。现有段落："
                + writeJson(indexedParagraphs);
    }

    private List<String> applyParagraphRepairs(List<String> paragraphs, JsonNode replacements) {
        if (!replacements.isArray()) return paragraphs;
        List<String> updated = new ArrayList<>(paragraphs);
        LinkedHashSet<Integer> replaced = new LinkedHashSet<>();
        for (JsonNode replacement : replacements) {
            int index = replacement.path("paragraphIndex").asInt(-1);
            String text = replacement.path("text").asText("").trim();
            if (index >= 0 && index < updated.size() && !text.isBlank() && replaced.add(index)) {
                updated.set(index, text);
            }
        }
        return updated;
    }

    private String supplementPrompt(String topic, String difficulty, List<String> missing,
                                    List<String> missingMeanings) {
        String length = missing.size() > 15 ? "400–700" : "180–350";
        String textInstruction = missing.isEmpty()
                ? "文章词汇已齐，只需补齐中文释义，article 返回空字符串。"
                : "请写一段约 " + length + " 个英文单词的连贯补充阅读，作为同一主题“" + topic
                + "”的自然延伸，难度为“" + difficulty + "”。必须逐一使用所有缺失正文词的给定原形，不要输出单词清单或例句集合。";
        return "主文章已完成。" + textInstruction
                + "并为所有释义缺失词补充中文语境释义。只返回 JSON："
                + "{\"article\":\"英文补充阅读或空字符串\",\"meanings\":{\"word\":\"中文语境释义\"}}。"
                + "不要输出推理过程。缺失正文词：" + String.join(", ", missing)
                + "。释义缺失词：" + String.join(", ", missingMeanings);
    }

    private List<String> missingMeanings(Map<String, String> meanings, List<String> words) {
        return words.stream().filter(word -> meanings.get(word) == null || meanings.get(word).isBlank()).toList();
    }

    /** {@inheritDoc} */
    @Override
    public List<VocabularyArticleDtos.ArticleSummary> list(Long userId) {
        return articleMapper.selectList(new LambdaQueryWrapper<VocabularyArticle>()
                        .eq(VocabularyArticle::getUserId, userId)
                        .orderByDesc(VocabularyArticle::getCreateTime)
                        .last("LIMIT 50"))
                .stream().map(this::summary).toList();
    }

    /** {@inheritDoc} */
    @Override
    public VocabularyArticleDtos.ArticleView get(Long id, Long userId) {
        VocabularyArticle article = articleMapper.selectOne(new LambdaQueryWrapper<VocabularyArticle>()
                .eq(VocabularyArticle::getId, id).eq(VocabularyArticle::getUserId, userId));
        if (article == null) {
            throw new BusinessException(404, "文章不存在或无权访问");
        }
        return view(article);
    }

    private VocabularyArticleDtos.ArticleView view(VocabularyArticle article) {
        try {
            List<String> words = objectMapper.readValue(article.getWordsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            Map<String, String> meanings = objectMapper.readValue(article.getMeaningsJson(),
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
            List<String> missing = objectMapper.readValue(article.getMissingWordsJson(),
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            return new VocabularyArticleDtos.ArticleView(article.getId(), words, article.getArticle(),
                    meanings, missing, article.getTopic(), article.getDifficulty(),
                    article.getCreateTime() == null ? "" : article.getCreateTime().toString());
        } catch (IOException exception) {
            throw new BusinessException(500, "文章数据读取失败");
        }
    }

    private VocabularyArticleDtos.ArticleSummary summary(VocabularyArticle article) {
        try {
            JsonNode words = objectMapper.readTree(article.getWordsJson());
            JsonNode missing = objectMapper.readTree(article.getMissingWordsJson());
            int total = words.isArray() ? words.size() : 0;
            int remaining = missing.isArray() ? missing.size() : 0;
            return new VocabularyArticleDtos.ArticleSummary(article.getId(), article.getTopic(),
                    article.getDifficulty(), total, total - remaining,
                    article.getCreateTime() == null ? "" : article.getCreateTime().toString());
        } catch (IOException exception) {
            throw new BusinessException(500, "文章摘要读取失败");
        }
    }

    private JsonNode parseObject(String raw, String stage) {
        String text = raw == null ? "" : raw.trim();
        if (text.startsWith("```")) {
            int firstLine = text.indexOf('\n');
            text = firstLine < 0 ? "" : text.substring(firstLine + 1).replaceFirst("```\\s*$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end < start) {
            log.warn("vocabulary_ai_invalid_json stage={} responseLength={} reason=no_json_object",
                    stage, text.length());
            throw new BusinessException(502, "模型输出不完整或格式无效，请重试；词表和已填写内容已保留");
        }
        try {
            JsonNode root = objectMapper.readTree(text.substring(start, end + 1));
            if (!root.isObject()) {
                log.warn("vocabulary_ai_invalid_json stage={} responseLength={} reason=not_object",
                        stage, text.length());
                throw new BusinessException(502, "模型输出不完整或格式无效，请重试；词表和已填写内容已保留");
            }
            return root;
        } catch (IOException exception) {
            log.warn("vocabulary_ai_invalid_json stage={} responseLength={} parserError={}",
                    stage, text.length(), exception.getClass().getSimpleName());
            throw new BusinessException(502, "模型输出不完整或格式无效，请重试；词表和已填写内容已保留");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException exception) {
            throw new BusinessException(500, "文章数据保存失败");
        }
    }

    private String safePreference(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String normalized = value.trim();
        if (normalized.length() > 60 || !normalized.matches("[\\p{L}0-9 、，,. -]+")) {
            throw new BusinessException(400, "题材或难度格式不正确");
        }
        return normalized;
    }

    private List<String> missingWords(String article, List<String> words) {
        List<String> missing = new ArrayList<>();
        for (String word : words) {
            Pattern exact = Pattern.compile("(?i)(?<![A-Za-z'-])" + Pattern.quote(word) + "(?![A-Za-z'-])");
            if (!exact.matcher(article).find()) missing.add(word);
        }
        return missing;
    }

    private void collectMeanings(JsonNode node, Map<String, String> meanings, List<String> words) {
        if (!node.isObject()) return;
        node.fields().forEachRemaining(entry -> {
            String word = entry.getKey().toLowerCase(Locale.ROOT);
            String meaning = entry.getValue().asText("").trim();
            if (words.contains(word) && !meaning.isBlank()) {
                meanings.put(word, meaning.length() > 120 ? meaning.substring(0, 120) : meaning);
            }
        });
    }
}
