package com.secondbrain.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ReviewCard;
import com.secondbrain.mapper.ReviewCardMapper;
import com.secondbrain.service.AiService;
import com.secondbrain.service.EbbinghausService;
import com.secondbrain.service.QuestionGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class QuestionGenerationServiceImpl implements QuestionGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionGenerationServiceImpl.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final AiService aiService;
    private final ReviewCardMapper reviewCardMapper;
    private final EbbinghausService ebbinghausService;

    @Autowired
    public QuestionGenerationServiceImpl(AiService aiService, ReviewCardMapper reviewCardMapper, EbbinghausService ebbinghausService) {
        this.aiService = aiService;
        this.reviewCardMapper = reviewCardMapper;
        this.ebbinghausService = ebbinghausService;
    }

    @Override
    public ReviewCard generateHighQualityQuestion(KnowledgeNode node, String cardType) {
        ReviewCard card = new ReviewCard();
        card.setNodeId(node.getId());
        card.setUserId(node.getUserId());
        card.setCardType("choice");
        card.setReviewCount(0);
        card.setCorrectCount(0);
        card.setIncorrectCount(0);
        card.setAverageAccuracy(0.0);
        card.setMasteryLevel(0);
        card.setMemoryStrength(0.0);
        card.setLastReviewTime(LocalDateTime.now());
        card.setNextReviewTime(LocalDateTime.now());
        card.setStatus(0);

        List<String> existingQuestions = getExistingQuestions(node.getId());

        int targetDifficulty = calculateTargetDifficulty(node);
        String question = generateChoiceQuestion(node, targetDifficulty);
        int difficulty = estimateQuestionDifficulty(question, node);

        double quality = evaluateQuestionQuality(question, node.getSummary());
        if (quality < 0.6) {
            log.warn("题目质量较低，重新生成，quality：{}", quality);
            question = generateFallbackChoiceQuestion(node);
            difficulty = 1;
        }

        card.setQuestion(question);
        card.setDifficulty(difficulty);
        card.setAiGenerated("true");
        
        String correctAnswer = extractCorrectAnswer(question);
        if (correctAnswer != null) {
            card.setAnswer(correctAnswer);
        }

        log.info("生成高质量复习卡片成功，nodeId：{}，cardType：{}，difficulty：{}，quality：{}，correctAnswer：{}",
                node.getId(), "choice", difficulty, quality, correctAnswer);

        return card;
    }

    @Override
    public String generateChoiceQuestion(KnowledgeNode node) {
        try {
            int targetDifficulty = calculateTargetDifficulty(node);
            String prompt = buildChoiceQuestionPrompt(node, targetDifficulty);
            String aiResponse = aiService.generateQuestion(prompt);

            String question = parseChoiceQuestion(aiResponse);
            
            if (question == null || question.isEmpty()) {
                log.warn("AI生成单选题格式错误，使用备用方案");
                return generateFallbackChoiceQuestion(node);
            }

            log.info("AI生成单选题成功，nodeId：{}", node.getId());
            return question;
        } catch (Exception e) {
            log.error("AI生成单选题失败，nodeId：{}", node.getId(), e);
            return generateFallbackChoiceQuestion(node);
        }
    }

    private String generateChoiceQuestion(KnowledgeNode node, int targetDifficulty) {
        try {
            String prompt = buildChoiceQuestionPrompt(node, targetDifficulty);
            String aiResponse = aiService.generateQuestion(prompt);

            String question = parseChoiceQuestion(aiResponse);
            
            if (question == null || question.isEmpty()) {
                log.warn("AI生成单选题格式错误，使用备用方案");
                return generateFallbackChoiceQuestion(node);
            }

            log.info("AI生成单选题成功，nodeId：{}，targetDifficulty：{}", node.getId(), targetDifficulty);
            return question;
        } catch (Exception e) {
            log.error("AI生成单选题失败，nodeId：{}", node.getId(), e);
            return generateFallbackChoiceQuestion(node);
        }
    }

    private int calculateTargetDifficulty(KnowledgeNode node) {
        int importance = node.getImportance() != null ? node.getImportance() : 3;
        int masteryLevel = node.getMasteryLevel() != null ? node.getMasteryLevel() : 0;
        
        if (masteryLevel >= 4) {
            return 3;
        } else if (masteryLevel >= 2) {
            return 2;
        } else {
            return 1;
        }
    }

    private String buildChoiceQuestionPrompt(KnowledgeNode node, int targetDifficulty) {
        String content = node.getContentMd() != null ? node.getContentMd() : node.getSummary();
        if (content == null || content.isEmpty()) {
            content = node.getSummary();
        }
        
        String difficultyDescription = "";
        String difficultyInstruction = "";
        
        switch (targetDifficulty) {
            case 1:
                difficultyDescription = "简单（基础概念题）";
                difficultyInstruction = "题目应该直接考查知识点的最基本概念，答案应该显而易见，不需要深入思考。";
                break;
            case 2:
                difficultyDescription = "中等（应用理解题）";
                difficultyInstruction = "题目应该考查对知识点的理解和应用，需要一定的思考才能得出答案。";
                break;
            case 3:
                difficultyDescription = "困难（综合分析题）";
                difficultyInstruction = "题目应该考查对知识点的深入理解和综合分析能力，需要仔细思考才能得出答案。";
                break;
        }
        
        return String.format(
                "你是一位专业的教育专家，请根据以下知识点生成一道高质量的单选题。\n\n" +
                        "【知识点信息】\n" +
                        "标题：%s\n" +
                        "摘要：%s\n" +
                        "内容：%s\n\n" +
                        "【题目要求】\n" +
                        "1. 题目类型：单选题（A、B、C、D四个选项）\n" +
                        "2. 考查目标：考查知识点的核心概念、关键原理或重要应用\n" +
                        "3. 难度等级：%s\n" +
                        "4. 难度说明：%s\n" +
                        "5. 选项设计：\n" +
                        "   - 正确选项：明确、准确、无歧义，必须唯一\n" +
                        "   - 错误选项：具有迷惑性，但明显错误\n" +
                        "   - 选项之间要有区分度\n" +
                        "   - 每个选项必须完整，不能是空值或占位符\n" +
                        "   - 选项内容要具体，不能是\"该知识点涉及的核心概念\"这样的描述\n" +
                        "6. 题目表述：简洁、清晰、无歧义\n" +
                        "7. 正确答案随机性：正确答案必须随机分布在A、B、C、D四个选项中，不能总是A\n\n" +
                        "【输出格式】\n" +
                        "请严格按照以下JSON格式输出：\n" +
                        "{\n" +
                        "  \"question\": \"题目描述\",\n" +
                        "  \"options\": {\n" +
                        "    \"A\": \"选项A的具体内容，必须完整\",\n" +
                        "    \"B\": \"选项B的具体内容，必须完整\",\n" +
                        "    \"C\": \"选项C的具体内容，必须完整\",\n" +
                        "    \"D\": \"选项D的具体内容，必须完整\"\n" +
                        "  },\n" +
                        "  \"correctAnswer\": \"A或B或C或D，必须随机选择其中一个作为正确答案\",\n" +
                        "  \"explanation\": \"答案解析，解释为什么正确答案是正确的，其他选项为什么是错误的\"\n" +
                        "}\n\n" +
                        "请生成题目：",
                node.getTitle(),
                node.getSummary() != null ? node.getSummary() : "无摘要",
                content.length() > 2000 ? content.substring(0, 2000) : content,
                difficultyDescription,
                difficultyInstruction
        );
    }

    private String parseChoiceQuestion(String aiResponse) {
        try {
            String cleanedResponse = aiResponse.trim();
            
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            } else if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            
            cleanedResponse = cleanedResponse.trim();
            
            log.info("清理后的AI响应长度：{}", cleanedResponse.length());
            
            JsonNode root = objectMapper.readTree(cleanedResponse);
            
            StringBuilder question = new StringBuilder();
            question.append(root.get("question").asText()).append("\n\n");
            
            JsonNode options = root.get("options");
            String optionA = options.get("A").asText();
            String optionB = options.get("B").asText();
            String optionC = options.get("C").asText();
            String optionD = options.get("D").asText();
            
            if (optionA.isEmpty() || optionB.isEmpty() || optionC.isEmpty() || optionD.isEmpty()) {
                log.warn("选项内容为空，使用备用方案");
                return null;
            }
            
            question.append("A. ").append(optionA).append("\n");
            question.append("B. ").append(optionB).append("\n");
            question.append("C. ").append(optionC).append("\n");
            question.append("D. ").append(optionD).append("\n");
            
            String aiCorrectAnswer = root.get("correctAnswer").asText();
            String correctAnswer;
            
            if (aiCorrectAnswer.matches("[ABCD]")) {
                correctAnswer = aiCorrectAnswer;
            } else {
                String[] optionLabels = {"A", "B", "C", "D"};
                int randomIndex = (int) (Math.random() * 4);
                correctAnswer = optionLabels[randomIndex];
                log.warn("AI返回的正确答案格式错误，随机选择：{}", correctAnswer);
            }
            
            question.append("\n正确答案：").append(correctAnswer);
            
            if (root.has("explanation")) {
                question.append("\n解析：").append(root.get("explanation").asText());
            }
            
            return question.toString();
        } catch (Exception e) {
            log.error("解析单选题JSON失败，原始响应：{}", aiResponse.substring(0, Math.min(500, aiResponse.length())), e);
            return null;
        }
    }

    private String generateFallbackChoiceQuestion(KnowledgeNode node) {
        String summary = node.getSummary() != null ? node.getSummary() : "";
        String title = node.getTitle();
        
        String[] keyPoints = summary.split("[，。；；]");
        String correctPoint = keyPoints.length > 0 ? keyPoints[0] : title;
        String wrongPoint1 = keyPoints.length > 1 ? keyPoints[1] : "相关概念A";
        String wrongPoint2 = keyPoints.length > 2 ? keyPoints[2] : "相关概念B";
        String wrongPoint3 = keyPoints.length > 3 ? keyPoints[3] : "相关概念C";
        
        String[] options = {correctPoint, wrongPoint1, wrongPoint2, wrongPoint3};
        String[] optionLabels = {"A", "B", "C", "D"};
        
        int correctIndex = (int) (Math.random() * 4);
        String correctAnswer = optionLabels[correctIndex];
        
        String[] shuffledOptions = new String[4];
        shuffledOptions[0] = options[correctIndex];
        shuffledOptions[1] = options[(correctIndex + 1) % 4];
        shuffledOptions[2] = options[(correctIndex + 2) % 4];
        shuffledOptions[3] = options[(correctIndex + 3) % 4];
        
        return String.format(
                "关于\"%s\"，以下哪项描述是正确的？\n\n" +
                        "A. %s\n" +
                        "B. %s\n" +
                        "C. %s\n" +
                        "D. %s\n\n" +
                        "正确答案：%s\n" +
                        "解析：根据知识点内容，%s是正确的描述。",
                title, shuffledOptions[0], shuffledOptions[1], shuffledOptions[2], shuffledOptions[3], 
                correctAnswer, correctPoint
        );
    }

    private String extractCorrectAnswer(String question) {
        if (question == null || question.isEmpty()) {
            return null;
        }
        
        String[] lines = question.split("\n");
        for (String line : lines) {
            if (line.startsWith("正确答案：")) {
                String answer = line.substring("正确答案：".length()).trim();
                if (answer.matches("[ABCD]")) {
                    return answer;
                }
            }
        }
        
        return null;
    }

    @Override
    public String generateFillQuestion(KnowledgeNode node) {
        try {
            String prompt = buildFillQuestionPrompt(node);
            String aiResponse = aiService.generateQuestion(prompt);

            String question = parseFillQuestion(aiResponse);
            
            if (question == null || question.isEmpty()) {
                log.warn("AI生成填空题格式错误，使用备用方案");
                return generateFallbackFillQuestion(node);
            }

            log.info("AI生成填空题成功，nodeId：{}", node.getId());
            return question;
        } catch (Exception e) {
            log.error("AI生成填空题失败，nodeId：{}", node.getId(), e);
            return generateFallbackFillQuestion(node);
        }
    }

    private String buildFillQuestionPrompt(KnowledgeNode node) {
        return String.format(
                "你是一位专业的教育专家，请根据以下知识点生成一道高质量的填空题。\n\n" +
                        "【知识点信息】\n" +
                        "标题：%s\n" +
                        "摘要：%s\n" +
                        "内容：%s\n\n" +
                        "【题目要求】\n" +
                        "1. 题目类型：填空题\n" +
                        "2. 考查目标：考查知识点的核心概念、关键术语或重要数据\n" +
                        "3. 填空设计：\n" +
                        "   - 填空处要明确，用\"____\"表示\n" +
                        "   - 答案要简洁、准确、唯一\n" +
                        "   - 避免过于宽泛或模糊的填空\n" +
                        "4. 题目表述：上下文要充分，确保能理解填空要求\n" +
                        "5. 难度适中：既不能太简单（直接抄原文），也不能太复杂（需要推理）\n\n" +
                        "【输出格式】\n" +
                        "请严格按照以下JSON格式输出：\n" +
                        "{\n" +
                        "  \"question\": \"题目描述，包含填空处____\",\n" +
                        "  \"answer\": \"填空答案\",\n" +
                        "  \"explanation\": \"答案解析\"\n" +
                        "}\n\n" +
                        "请生成题目：",
                node.getTitle(),
                node.getSummary() != null ? node.getSummary() : "无摘要",
                node.getContentMd() != null ? node.getContentMd().substring(0, Math.min(800, node.getContentMd().length())) : "无内容"
        );
    }

    private String parseFillQuestion(String aiResponse) {
        try {
            String cleanedResponse = aiResponse.trim();
            
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            } else if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            
            cleanedResponse = cleanedResponse.trim();
            
            log.info("清理后的AI响应长度：{}", cleanedResponse.length());
            
            JsonNode root = objectMapper.readTree(cleanedResponse);
            
            StringBuilder question = new StringBuilder();
            question.append(root.get("question").asText()).append("\n\n");
            question.append("答案：").append(root.get("answer").asText());
            
            if (root.has("explanation")) {
                question.append("\n解析：").append(root.get("explanation").asText());
            }
            
            return question.toString();
        } catch (Exception e) {
            log.error("解析填空题JSON失败，原始响应：{}", aiResponse.substring(0, Math.min(500, aiResponse.length())), e);
            return null;
        }
    }

    private String generateFallbackFillQuestion(KnowledgeNode node) {
        String summary = node.getSummary() != null ? node.getSummary() : node.getTitle();
        String[] words = summary.split("\\s+");
        String keyWord = words.length > 0 ? words[0] : "核心概念";
        
        return String.format(
                "在\"%s\"中，____是最重要的概念之一。\n\n" +
                        "答案：%s\n" +
                        "解析：这是该知识点的核心概念。",
                node.getTitle(), keyWord
        );
    }

    @Override
    public String generateSimpleQuestion(KnowledgeNode node) {
        try {
            String prompt = buildSimpleQuestionPrompt(node);
            String aiResponse = aiService.generateQuestion(prompt);

            String question = parseSimpleQuestion(aiResponse);
            
            if (question == null || question.isEmpty()) {
                log.warn("AI生成简单问题格式错误，使用备用方案");
                return generateFallbackSimpleQuestion(node);
            }

            log.info("AI生成简单问题成功，nodeId：{}", node.getId());
            return question;
        } catch (Exception e) {
            log.error("AI生成简单问题失败，nodeId：{}", node.getId(), e);
            return generateFallbackSimpleQuestion(node);
        }
    }

    private String buildSimpleQuestionPrompt(KnowledgeNode node) {
        return String.format(
                "你是一位专业的教育专家，请根据以下知识点生成一道高质量的简答题。\n\n" +
                        "【知识点信息】\n" +
                        "标题：%s\n" +
                        "摘要：%s\n" +
                        "内容：%s\n\n" +
                        "【题目要求】\n" +
                        "1. 题目类型：简答题\n" +
                        "2. 考查目标：考查知识点的核心概念、关键原理或重要应用\n" +
                        "3. 题目设计：\n" +
                        "   - 问题要具体、明确\n" +
                        "   - 答案要简洁、准确\n" +
                        "   - 避免过于宽泛的问题\n" +
                        "4. 题目表述：简洁、清晰、无歧义\n" +
                        "5. 难度适中：能够用2-3句话回答清楚\n\n" +
                        "【输出格式】\n" +
                        "请严格按照以下JSON格式输出：\n" +
                        "{\n" +
                        "  \"question\": \"题目描述\",\n" +
                        "  \"answer\": \"参考答案\",\n" +
                        "  \"keyPoints\": [\"要点1\", \"要点2\", \"要点3\"]\n" +
                        "}\n\n" +
                        "请生成题目：",
                node.getTitle(),
                node.getSummary() != null ? node.getSummary() : "无摘要",
                node.getContentMd() != null ? node.getContentMd().substring(0, Math.min(800, node.getContentMd().length())) : "无内容"
        );
    }

    private String parseSimpleQuestion(String aiResponse) {
        try {
            String cleanedResponse = aiResponse.trim();
            
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            } else if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            
            cleanedResponse = cleanedResponse.trim();
            
            log.info("清理后的AI响应长度：{}", cleanedResponse.length());
            
            JsonNode root = objectMapper.readTree(cleanedResponse);
            
            StringBuilder question = new StringBuilder();
            question.append(root.get("question").asText()).append("\n\n");
            question.append("参考答案：").append(root.get("answer").asText());
            
            if (root.has("keyPoints")) {
                question.append("\n\n要点：");
                JsonNode keyPoints = root.get("keyPoints");
                for (int i = 0; i < keyPoints.size(); i++) {
                    question.append("\n").append(i + 1).append(". ").append(keyPoints.get(i).asText());
                }
            }
            
            return question.toString();
        } catch (Exception e) {
            log.error("解析简答题JSON失败，原始响应：{}", aiResponse.substring(0, Math.min(500, aiResponse.length())), e);
            return null;
        }
    }

    private String generateFallbackSimpleQuestion(KnowledgeNode node) {
        String summary = node.getSummary() != null ? node.getSummary() : "请描述该知识点的主要内容。";
        return String.format(
                "请简要说明\"%s\"的核心概念。\n\n" +
                        "参考答案：%s",
                node.getTitle(), summary
        );
    }

    @Override
    public double evaluateQuestionQuality(String question, String expectedAnswer) {
        double quality = 0.0;

        if (question == null || question.isEmpty()) {
            return 0.0;
        }

        quality += 0.3;

        if (question.length() >= 20 && question.length() <= 500) {
            quality += 0.2;
        }

        if (question.contains("？") || question.contains("?") || question.contains("：") || question.contains(":")) {
            quality += 0.2;
        }

        if (question.contains("A.") || question.contains("B.") || question.contains("C.") || question.contains("D.") ||
            question.contains("A、") || question.contains("B、") || question.contains("C、") || question.contains("D、")) {
            quality += 0.1;
        }

        if (question.contains("答案") || question.contains("解析") || question.contains("要点")) {
            quality += 0.2;
        }

        return Math.min(quality, 1.0);
    }

    @Override
    public int estimateQuestionDifficulty(String question, KnowledgeNode node) {
        if (question == null || question.isEmpty()) {
            return 1;
        }

        int difficulty = 1;
        int importance = node.getImportance() != null ? node.getImportance() : 3;
        int masteryLevel = node.getMasteryLevel() != null ? node.getMasteryLevel() : 0;
        String content = node.getContentMd() != null ? node.getContentMd() : "";

        int questionLength = question.length();
        int contentLength = content.length();

        int complexityScore = 0;

        if (question.contains("A.") && question.contains("B.") && question.contains("C.") && question.contains("D.")) {
            complexityScore += 2;
        } else if (question.contains("____") || question.contains("_____")) {
            complexityScore += 1;
        }

        if (questionLength > 300) {
            complexityScore += 2;
        } else if (questionLength > 150) {
            complexityScore += 1;
        }

        if (contentLength > 2000) {
            complexityScore += 2;
        } else if (contentLength > 1000) {
            complexityScore += 1;
        }

        if (importance >= 4) {
            complexityScore += 1;
        }

        if (masteryLevel >= 3) {
            complexityScore += 1;
        }

        if (complexityScore >= 5) {
            difficulty = 3;
        } else if (complexityScore >= 3) {
            difficulty = 2;
        } else {
            difficulty = 1;
        }

        return difficulty;
    }

    @Override
    public boolean isQuestionDiverse(String newQuestion, List<String> existingQuestions) {
        if (existingQuestions == null || existingQuestions.isEmpty()) {
            return true;
        }

        String newQuestionLower = newQuestion.toLowerCase();
        int similarCount = 0;

        for (String existingQuestion : existingQuestions) {
            String existingLower = existingQuestion.toLowerCase();
            
            double similarity = calculateSimilarity(newQuestionLower, existingLower);
            
            if (similarity > 0.7) {
                similarCount++;
            }
        }

        return similarCount == 0;
    }

    private double calculateSimilarity(String str1, String str2) {
        if (str1 == null || str2 == null) {
            return 0.0;
        }

        int maxLength = Math.max(str1.length(), str2.length());
        if (maxLength == 0) {
            return 1.0;
        }

        int distance = levenshteinDistance(str1, str2);
        return 1.0 - (double) distance / maxLength;
    }

    private int levenshteinDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= str2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j], dp[i][j - 1]), dp[i - 1][j - 1]) + 1;
                }
            }
        }

        return dp[str1.length()][str2.length()];
    }

    private List<String> getExistingQuestions(Long nodeId) {
        try {
            List<com.secondbrain.entity.ReviewCard> cards = reviewCardMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.secondbrain.entity.ReviewCard>()
                            .eq(com.secondbrain.entity.ReviewCard::getNodeId, nodeId)
                            .eq(com.secondbrain.entity.ReviewCard::getDeleted, 0)
            );

            List<String> questions = new ArrayList<>();
            for (com.secondbrain.entity.ReviewCard card : cards) {
                if (card.getQuestion() != null) {
                    questions.add(card.getQuestion());
                }
            }

            return questions;
        } catch (Exception e) {
            log.error("获取已存在问题失败，nodeId：{}", nodeId, e);
            return new ArrayList<>();
        }
    }
}
