package com.secondbrain.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.secondbrain.entity.KnowledgeNode;
import com.secondbrain.entity.ReviewCard;
import com.secondbrain.entity.User;
import com.secondbrain.mapper.ReviewCardMapper;
import com.secondbrain.mapper.UserMapper;
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
    private final UserMapper userMapper;

    @Autowired
    public QuestionGenerationServiceImpl(AiService aiService, ReviewCardMapper reviewCardMapper, EbbinghausService ebbinghausService, UserMapper userMapper) {
        this.aiService = aiService;
        this.reviewCardMapper = reviewCardMapper;
        this.ebbinghausService = ebbinghausService;
        this.userMapper = userMapper;
    }

    @Override
    public ReviewCard generateHighQualityQuestion(KnowledgeNode node, String cardType, Long userId) {
        ReviewCard card = new ReviewCard();
        card.setNodeId(node.getId());
        card.setUserId(node.getUserId());
        card.setCardType(cardType != null ? cardType : "choice");
        card.setReviewCount(0);
        card.setCorrectCount(0);
        card.setIncorrectCount(0);
        card.setMasteryLevel(0);
        card.setMemoryStrength(0.0);
        card.setLastReviewTime(LocalDateTime.now());
        card.setNextReviewTime(LocalDateTime.now());
        card.setStatus(0);
        card.setIsRestored(0);

        List<String> existingQuestions = getExistingQuestions(node.getId());

        int targetDifficulty = calculateTargetDifficulty(node);
        String question = generateChoiceQuestion(node, targetDifficulty, existingQuestions, userId);
        
        int maxRetries = 3;
        int retryCount = 0;
        
        while (question == null || question.isEmpty()) {
            retryCount++;
            if (retryCount >= maxRetries) {
                log.warn("生成题目失败，已重试{}次，使用备用方案", maxRetries);
                question = generateFallbackChoiceQuestion(node);
                break;
            }
            log.info("题目质量不达标，重新生成，重试次数：{}", retryCount);
            question = generateChoiceQuestion(node, targetDifficulty, existingQuestions, node.getUserId());
        }

        int difficulty = estimateQuestionDifficulty(question, node);

        double quality = evaluateQuestionQuality(question, node.getSummary());
        if (quality < 0.6) {
            log.warn("题目质量较低，quality：{}，重新生成", quality);
            question = generateChoiceQuestion(node, targetDifficulty, existingQuestions, node.getUserId());
            difficulty = estimateQuestionDifficulty(question, node);
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
    public String generateChoiceQuestion(KnowledgeNode node, Long userId) {
        try {
            int targetDifficulty = calculateTargetDifficulty(node);
            String prompt = buildChoiceQuestionPrompt(node, targetDifficulty);
            String aiResponse = aiService.generateQuestion(prompt, getUserApiKey(userId));

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

    private String generateChoiceQuestion(KnowledgeNode node, int targetDifficulty, List<String> existingQuestions, Long userId) {
        try {
            String prompt = buildChoiceQuestionPrompt(node, targetDifficulty, existingQuestions);
            String aiResponse = aiService.generateQuestion(prompt, getUserApiKey(userId));

            String question = parseChoiceQuestion(aiResponse);
            
            if (question == null || question.isEmpty()) {
                log.warn("AI生成单选题格式错误，使用备用方案");
                return generateFallbackChoiceQuestion(node);
            }

            log.info("AI生成单选题成功，nodeId：{}，targetDifficulty：{}", node.getId(), targetDifficulty);
            return question;
        } catch (RuntimeException e) {
            if (e.getMessage().contains("API Key")) {
                log.error("AI服务不可用，请配置API Key：{}", e.getMessage());
                throw new RuntimeException("AI服务不可用，请配置有效的API Key后重试。请前往个人设置添加API Key。");
            }
            log.error("AI生成单选题失败，nodeId：{}", node.getId(), e);
            return generateFallbackChoiceQuestion(node);
        } catch (Exception e) {
            log.error("AI生成单选题失败，nodeId：{}", node.getId(), e);
            return generateFallbackChoiceQuestion(node);
        }
    }

    private String getUserApiKey(Long userId) {
        try {
            if (userId == null) {
                return null;
            }
            
            User user = userMapper.selectById(userId);
            if (user != null) {
                return user.getApiKey();
            }
            return null;
        } catch (Exception e) {
            log.error("获取用户API Key失败，userId：{}", userId, e);
            return null;
        }
    }

    private int calculateTargetDifficulty(KnowledgeNode node) {
        int importance = node.getImportance() != null ? node.getImportance() : 3;
        int masteryLevel = node.getMasteryLevel() != null ? node.getMasteryLevel() : 0;
        int reviewCount = node.getReviewCount() != null ? node.getReviewCount() : 0;
        
        int baseDifficulty = 1;
        
        if (masteryLevel >= 4) {
            baseDifficulty = 4;
        } else if (masteryLevel >= 3) {
            baseDifficulty = 3;
        } else if (masteryLevel >= 2) {
            baseDifficulty = 2;
        } else if (masteryLevel >= 1) {
            baseDifficulty = 2;
        }
        
        int reviewBonus = Math.min(1, reviewCount / 5);
        int importanceBonus = Math.min(1, (importance - 3) / 2);
        
        int finalDifficulty = Math.min(5, Math.max(1, baseDifficulty + reviewBonus + importanceBonus));
        
        log.debug("计算目标难度，nodeId：{}，masteryLevel：{}，reviewCount：{}，importance：{}，baseDifficulty：{}，finalDifficulty：{}",
                node.getId(), masteryLevel, reviewCount, importance, baseDifficulty, finalDifficulty);
        
        return finalDifficulty;
    }

    private String buildChoiceQuestionPrompt(KnowledgeNode node, int targetDifficulty) {
        String content = node.getContentMd() != null ? node.getContentMd() : node.getSummary();
        if (content == null || content.isEmpty()) {
            content = node.getSummary();
        }
        
        String difficultyDescription = "";
        String difficultyInstruction = "";
        String depthRequirement = "";
        
        switch (targetDifficulty) {
            case 1:
                difficultyDescription = "简单（基础概念题）";
                difficultyInstruction = "题目应该直接考查知识点的最基本概念，答案应该显而易见，不需要深入思考。";
                depthRequirement = "考查知识点的定义、基本特征和简单应用。";
                break;
            case 2:
                difficultyDescription = "中等（应用理解题）";
                difficultyInstruction = "题目应该考查对知识点的理解和应用，需要一定的思考才能得出答案。";
                depthRequirement = "考查知识点的原理、机制和实际应用场景。";
                break;
            case 3:
                difficultyDescription = "困难（综合分析题）";
                difficultyInstruction = "题目应该考查对知识点的深入理解和综合分析能力，需要仔细思考才能得出答案。";
                depthRequirement = "考查知识点的深层原理、复杂应用和与其他知识点的关联。";
                break;
            case 4:
                difficultyDescription = "专家（深度探究题）";
                difficultyInstruction = "题目应该考查对知识点的深度理解和批判性思维，需要综合运用多个知识点。";
                depthRequirement = "考查知识点的本质、高级应用、边界条件和特殊场景。";
                break;
            case 5:
                difficultyDescription = "大师（创新挑战题）";
                difficultyInstruction = "题目应该考查对知识点的融会贯通和创新应用，需要跨领域思考和创造性解决问题。";
                depthRequirement = "考查知识点的理论深度、实践创新、前沿发展和极限情况。";
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
                        "2. 考查目标：%s\n" +
                        "3. 难度等级：%s\n" +
                        "4. 难度说明：%s\n" +
                        "5. 选项设计（重要）：\n" +
                        "   - 每个选项必须是完整、独立的句子，不能是短语或片段\n" +
                        "   - 每个选项必须表达完整的意思，不能是半句话\n" +
                        "   - 选项之间要有明显的区别，不能只是同一句话的不同部分\n" +
                        "   - 正确选项：明确、准确、无歧义，必须唯一\n" +
                        "   - 错误选项：具有迷惑性，但明显错误\n" +
                        "   - 选项要涵盖不同的错误类型（概念混淆、逻辑错误、细节遗漏、反向表述等）\n" +
                        "   - 选项长度要适中，一般15-60字\n" +
                        "   - 禁止使用\"该知识点涉及的核心概念\"这样的模糊描述\n" +
                        "   - 禁止把一句话拆分成四个选项\n" +
                        "   - 每个选项都要有明确的判断标准\n" +
                        "6. 题目表述：简洁、清晰、无歧义，避免使用过于复杂的句式\n" +
                        "7. 正确答案随机性：正确答案必须随机分布在A、B、C、D四个选项中，不能总是A\n" +
                        "8. 题目长度：控制在100-200字之间\n\n" +
                        "【输出格式】\n" +
                        "请严格按照以下JSON格式输出：\n" +
                        "{\n" +
                        "  \"question\": \"题目描述\",\n" +
                        "  \"options\": {\n" +
                        "    \"A\": \"选项A的完整句子内容\",\n" +
                        "    \"B\": \"选项B的完整句子内容\",\n" +
                        "    \"C\": \"选项C的完整句子内容\",\n" +
                        "    \"D\": \"选项D的完整句子内容\"\n" +
                        "  },\n" +
                        "  \"correctAnswer\": \"A或B或C或D，必须随机选择其中一个作为正确答案\",\n" +
                        "  \"explanation\": \"整体解析，解释题目考查的知识点\",\n" +
                        "  \"optionExplanations\": {\n" +
                        "    \"A\": \"选项A的详细解析，解释为什么正确或错误（必须提供）\",\n" +
                        "    \"B\": \"选项B的详细解析，解释为什么正确或错误（必须提供）\",\n" +
                        "    \"C\": \"选项C的详细解析，解释为什么正确或错误（必须提供）\",\n" +
                        "    \"D\": \"选项D的详细解析，解释为什么正确或错误（必须提供）\"\n" +
                        "  }\n" +
                        "}\n\n" +
                        "【重要提示】\n" +
                        "- 每个选项必须是完整的句子，不能是短语或片段\n" +
                        "- 每个选项都要有详细的解析，不能为空\n" +
                        "- 选项之间要有明显的区别，不能只是同一句话的不同部分\n" +
                        "- 正确答案必须随机分布\n\n" +
                        "请生成题目：",
                node.getTitle(),
                node.getSummary() != null ? node.getSummary() : "无摘要",
                content.length() > 2000 ? content.substring(0, 2000) : content,
                depthRequirement,
                difficultyDescription,
                difficultyInstruction
        );
    }

    private String buildChoiceQuestionPrompt(KnowledgeNode node, int targetDifficulty, List<String> existingQuestions) {
        String content = node.getContentMd() != null ? node.getContentMd() : node.getSummary();
        if (content == null || content.isEmpty()) {
            content = node.getSummary();
        }
        
        String difficultyDescription = "";
        String difficultyInstruction = "";
        String depthRequirement = "";
        
        switch (targetDifficulty) {
            case 1:
                difficultyDescription = "简单（基础概念题）";
                difficultyInstruction = "题目应该直接考查知识点的最基本概念，答案应该显而易见，不需要深入思考。";
                depthRequirement = "考查知识点的定义、基本特征和简单应用。";
                break;
            case 2:
                difficultyDescription = "中等（应用理解题）";
                difficultyInstruction = "题目应该考查对知识点的理解和应用，需要一定的思考才能得出答案。";
                depthRequirement = "考查知识点的原理、机制和实际应用场景。";
                break;
            case 3:
                difficultyDescription = "困难（综合分析题）";
                difficultyInstruction = "题目应该考查对知识点的深入理解和综合分析能力，需要仔细思考才能得出答案。";
                depthRequirement = "考查知识点的深层原理、复杂应用和与其他知识点的关联。";
                break;
            case 4:
                difficultyDescription = "专家（深度探究题）";
                difficultyInstruction = "题目应该考查对知识点的深度理解和批判性思维，需要综合运用多个知识点。";
                depthRequirement = "考查知识点的本质、高级应用、边界条件和特殊场景。";
                break;
            case 5:
                difficultyDescription = "大师（创新挑战题）";
                difficultyInstruction = "题目应该考查对知识点的融会贯通和创新应用，需要跨领域思考和创造性解决问题。";
                depthRequirement = "考查知识点的理论深度、实践创新、前沿发展和极限情况。";
                break;
        }

        String existingQuestionsText = "";
        if (existingQuestions != null && !existingQuestions.isEmpty()) {
            existingQuestionsText = "\n\n【已存在的题目（请避免重复）】\n";
            int count = Math.min(existingQuestions.size(), 5);
            for (int i = 0; i < count; i++) {
                existingQuestionsText += (i + 1) + ". " + existingQuestions.get(i) + "\n";
            }
            existingQuestionsText += "\n请确保生成的题目与以上题目完全不同，避免重复！";
        }
        
        return String.format(
                "你是一位专业的教育专家，请根据以下知识点生成一道高质量的单选题。\n\n" +
                        "【知识点信息】\n" +
                        "标题：%s\n" +
                        "摘要：%s\n" +
                        "内容：%s\n\n" +
                        "【题目要求】\n" +
                        "1. 题目类型：单选题（A、B、C、D四个选项）\n" +
                        "2. 考查目标：%s\n" +
                        "3. 难度等级：%s\n" +
                        "4. 难度说明：%s\n" +
                        "5. 选项设计：\n" +
                        "   - 正确选项：明确、准确、无歧义，必须唯一\n" +
                        "   - 错误选项：具有迷惑性，但明显错误\n" +
                        "   - 选项之间要有区分度\n" +
                        "   - 每个选项必须完整，不能是空值或占位符\n" +
                        "   - 选项内容要具体，不能是\"该知识点涉及的核心概念\"这样的描述\n" +
                        "   - 选项长度要适中，一般20-50字\n" +
                        "   - 选项要涵盖不同的错误类型（概念混淆、逻辑错误、细节遗漏等）\n" +
                        "6. 题目表述：简洁、清晰、无歧义，避免使用过于复杂的句式\n" +
                        "7. 正确答案随机性：正确答案必须随机分布在A、B、C、D四个选项中，不能总是A\n" +
                        "8. 题目长度：控制在100-200字之间\n" +
                        "9. 题目要新颖独特，避免与已存在的题目重复\n" +
                        "10. 从不同的角度和维度考察知识点\n\n" +
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
                        "  \"explanation\": \"整体解析，解释题目考查的知识点\",\n" +
                        "  \"optionExplanations\": {\n" +
                        "    \"A\": \"选项A的详细解析，解释为什么正确或错误\",\n" +
                        "    \"B\": \"选项B的详细解析，解释为什么正确或错误\",\n" +
                        "    \"C\": \"选项C的详细解析，解释为什么正确或错误\",\n" +
                        "    \"D\": \"选项D的详细解析，解释为什么正确或错误\"\n" +
                        "  }\n" +
                        "}\n\n" +
                        "%s\n\n" +
                        "请生成题目：",
                node.getTitle(),
                node.getSummary() != null ? node.getSummary() : "无摘要",
                content.length() > 2000 ? content.substring(0, 2000) : content,
                depthRequirement,
                difficultyDescription,
                difficultyInstruction,
                existingQuestionsText
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
            
            if (!isOptionValid(optionA) || !isOptionValid(optionB) || !isOptionValid(optionC) || !isOptionValid(optionD)) {
                log.warn("选项内容质量不合格，使用备用方案。A: {}, B: {}, C: {}, D: {}", 
                        optionA, optionB, optionC, optionD);
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
            
            if (root.has("optionExplanations")) {
                JsonNode optionExplanations = root.get("optionExplanations");
                question.append("\n\n选项解析：");
                if (optionExplanations.has("A")) {
                    question.append("\nA. ").append(optionExplanations.get("A").asText());
                }
                if (optionExplanations.has("B")) {
                    question.append("\nB. ").append(optionExplanations.get("B").asText());
                }
                if (optionExplanations.has("C")) {
                    question.append("\nC. ").append(optionExplanations.get("C").asText());
                }
                if (optionExplanations.has("D")) {
                    question.append("\nD. ").append(optionExplanations.get("D").asText());
                }
            }
            
            return question.toString();
        } catch (Exception e) {
            log.error("解析单选题JSON失败，原始响应：{}", aiResponse.substring(0, Math.min(500, aiResponse.length())), e);
            return parseChoiceQuestionFallback(aiResponse);
        }
    }

    private String parseChoiceQuestionFallback(String aiResponse) {
        try {
            String question = "";
            String[] options = new String[4];
            String correctAnswer = "";
            String explanation = "";

            String[] lines = aiResponse.split("\n");
            StringBuilder currentSection = new StringBuilder();
            String currentSectionName = "";

            for (String line : lines) {
                line = line.trim();
                
                if (line.startsWith("【题目】") || line.startsWith("题目：") || line.startsWith("Question：") || line.startsWith("question:")) {
                    question = currentSection.toString().trim();
                    currentSection = new StringBuilder();
                    currentSectionName = "question";
                } else if (line.startsWith("【正确答案】") || line.startsWith("正确答案：") || line.startsWith("Correct Answer：") || line.startsWith("correctAnswer:")) {
                    if (currentSectionName.equals("options")) {
                        String[] optionLines = currentSection.toString().split("\n");
                        for (String optionLine : optionLines) {
                            if (optionLine.matches("^[A-D][.、].*")) {
                                int index = optionLine.charAt(0) - 'A';
                                options[index] = optionLine.substring(2).trim();
                            }
                        }
                    }
                    correctAnswer = line.replaceAll("【正确答案】|正确答案：|【|】|Correct Answer：|correctAnswer:", "").trim();
                    currentSection = new StringBuilder();
                    currentSectionName = "answer";
                } else if (line.startsWith("【解析】") || line.startsWith("解析：") || line.startsWith("Explanation：") || line.startsWith("explanation:")) {
                    explanation = currentSection.toString().trim();
                    currentSection = new StringBuilder();
                    currentSectionName = "explanation";
                } else {
                    if (!line.isEmpty()) {
                        currentSection.append(line).append("\n");
                    }
                }
            }

            if (currentSectionName.equals("explanation")) {
                explanation = currentSection.toString().trim();
            }

            if (question.isEmpty()) {
                for (int i = 0; i < lines.length; i++) {
                    if (lines[i].matches("^[A-D][.、].*")) {
                        question = String.join("\n", java.util.Arrays.copyOfRange(lines, 0, i)).trim();
                        break;
                    }
                }
            }

            if (question.isEmpty()) {
                log.warn("无法解析题目内容");
                return null;
            }

            StringBuilder result = new StringBuilder();
            result.append(question).append("\n");
            for (int i = 0; i < 4; i++) {
                if (options[i] != null && !options[i].isEmpty()) {
                    result.append((char)('A' + i)).append(".").append(options[i]).append("\n");
                }
            }
            if (!correctAnswer.isEmpty()) {
                result.append("正确答案：").append(correctAnswer).append("\n");
            }
            if (!explanation.isEmpty()) {
                result.append("解析：").append(explanation);
            }

            log.info("备用解析成功，题目长度：{}，正确答案：{}", 
                    result.length(), correctAnswer);
            return result.toString();

        } catch (Exception e) {
            log.error("备用解析也失败", e);
            return null;
        }
    }

    private String generateFallbackChoiceQuestion(KnowledgeNode node) {
        String summary = node.getSummary() != null ? node.getSummary() : "";
        String title = node.getTitle();
        String content = node.getContentMd() != null ? node.getContentMd() : "";
        
        if (summary.isEmpty() && content.isEmpty()) {
            summary = title;
        }
        
        String fullContent = content.isEmpty() ? summary : content;
        String[] sentences = fullContent.split("[，。；；\n]");
        
        List<String> keyPoints = new ArrayList<>();
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > 10 && trimmed.length() < 100) {
                if (trimmed.contains("是") || trimmed.contains("的") || trimmed.contains("可以") || 
                    trimmed.contains("能够") || trimmed.contains("会") || trimmed.contains("不") ||
                    trimmed.contains("有") || trimmed.contains("为") || trimmed.contains("在")) {
                    keyPoints.add(trimmed);
                }
            }
            if (keyPoints.size() >= 10) {
                break;
            }
        }
        
        if (keyPoints.isEmpty()) {
            keyPoints.add(title + "是重要的技术概念");
            keyPoints.add("需要深入理解和掌握");
            keyPoints.add("在实际应用中很常见");
            keyPoints.add("是系统设计的关键要素");
        }
        
        String correctPoint = keyPoints.get(0);
        String wrongPoint1 = keyPoints.size() > 1 ? createWrongOption(keyPoints.get(1)) : "这是一个常见的误解";
        String wrongPoint2 = keyPoints.size() > 2 ? createWrongOption(keyPoints.get(2)) : "这个描述不够准确";
        String wrongPoint3 = keyPoints.size() > 3 ? createWrongOption(keyPoints.get(3)) : "这个说法存在偏差";
        
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
                        "解析：根据知识点内容，%s是正确的描述。\n\n" +
                        "选项解析：\n" +
                        "A. %s\n" +
                        "B. %s\n" +
                        "C. %s\n" +
                        "D. %s",
                title, shuffledOptions[0], shuffledOptions[1], shuffledOptions[2], shuffledOptions[3], 
                correctAnswer, correctPoint,
                generateOptionExplanation(shuffledOptions[0], correctAnswer, "A"),
                generateOptionExplanation(shuffledOptions[1], correctAnswer, "B"),
                generateOptionExplanation(shuffledOptions[2], correctAnswer, "C"),
                generateOptionExplanation(shuffledOptions[3], correctAnswer, "D")
        );
    }

    private String createWrongOption(String correctOption) {
        if (correctOption.contains("是")) {
            return correctOption.replace("是", "不是");
        } else if (correctOption.contains("可以")) {
            return correctOption.replace("可以", "不可以");
        } else if (correctOption.contains("能够")) {
            return correctOption.replace("能够", "不能够");
        } else if (correctOption.contains("会")) {
            return correctOption.replace("会", "不会");
        } else if (correctOption.contains("有")) {
            return correctOption.replace("有", "没有");
        } else if (correctOption.contains("在")) {
            return correctOption.replace("在", "不在");
        } else {
            return "这个描述存在偏差";
        }
    }

    private String generateOptionExplanation(String option, String correctAnswer, String optionLabel) {
        if (optionLabel.equals(correctAnswer)) {
            return "正确。" + option.substring(0, Math.min(20, option.length())) + "符合知识点描述。";
        } else {
            return "错误。" + option.substring(0, Math.min(20, option.length())) + "不符合知识点描述。";
        }
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
    public String generateFillQuestion(KnowledgeNode node, Long userId) {
        try {
            String prompt = buildFillQuestionPrompt(node);
            String aiResponse = aiService.generateQuestion(prompt, getUserApiKey(userId));

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
    public String generateSimpleQuestion(KnowledgeNode node, Long userId) {
        try {
            String prompt = buildSimpleQuestionPrompt(node);
            String aiResponse = aiService.generateQuestion(prompt, getUserApiKey(userId));

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

        quality += 0.2;

        if (question.length() >= 50 && question.length() <= 800) {
            quality += 0.15;
        } else if (question.length() >= 30 && question.length() <= 1000) {
            quality += 0.1;
        }

        if (question.contains("？") || question.contains("?") || question.contains("：") || question.contains(":")) {
            quality += 0.15;
        }

        if (question.contains("A.") && question.contains("B.") && question.contains("C.") && question.contains("D.")) {
            quality += 0.15;
        }

        int optionCount = 0;
        String[] lines = question.split("\n");
        String[] options = new String[4];
        for (String line : lines) {
            if (line.matches("^[A-D]\\..*")) {
                int index = line.charAt(0) - 'A';
                options[index] = line.substring(2).trim();
                optionCount++;
                String optionContent = line.substring(2).trim();
                if (optionContent.length() >= 10 && optionContent.length() <= 100) {
                    quality += 0.05;
                }
                if (!optionContent.isEmpty() && !optionContent.equals("...") && !optionContent.contains("相关")) {
                    quality += 0.05;
                }
            }
        }

        if (optionCount >= 4) {
            quality += 0.1;
        }

        if (question.contains("正确答案：") || question.contains("解析：")) {
            quality += 0.1;
        }

        if (question.contains("选项解析：")) {
            quality += 0.1;
        }

        if (options[0] != null && options[1] != null && options[2] != null && options[3] != null) {
            double similarity = calculateOptionSimilarity(options);
            if (similarity < 0.3) {
                quality += 0.15;
            } else if (similarity < 0.5) {
                quality += 0.1;
            } else {
                log.warn("选项相似度过高，可能是一句话拆分：{}", similarity);
                quality -= 0.2;
            }
        }

        String[] optionLetters = {"A.", "B.", "C.", "D."};
        String lowerQuestion = question.toLowerCase();
        int uniqueWords = 0;
        for (String optionLetter : optionLetters) {
            if (question.contains(optionLetter)) {
                String optionText = question.substring(question.indexOf(optionLetter) + 2);
                int endIndex = optionText.indexOf("\n");
                if (endIndex > 0) {
                    optionText = optionText.substring(0, endIndex).trim();
                    String[] words = optionText.split("\\s+");
                    for (String word : words) {
                        if (word.length() > 2 && !word.equals("相关") && !word.equals("概念") && !word.equals("选项")) {
                            uniqueWords++;
                        }
                    }
                }
            }
        }

        if (uniqueWords >= 8) {
            quality += 0.1;
        }

        return Math.min(quality, 1.0);
    }

    private double calculateOptionSimilarity(String[] options) {
        if (options == null || options.length != 4) {
            return 0.0;
        }

        double totalSimilarity = 0.0;
        int comparisons = 0;

        for (int i = 0; i < options.length; i++) {
            for (int j = i + 1; j < options.length; j++) {
                double similarity = calculateStringSimilarity(options[i], options[j]);
                totalSimilarity += similarity;
                comparisons++;
            }
        }

        return comparisons > 0 ? totalSimilarity / comparisons : 0.0;
    }

    private double calculateStringSimilarity(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return 0.0;
        }

        String[] words1 = s1.split("\\s+");
        String[] words2 = s2.split("\\s+");

        int commonWords = 0;
        for (String word1 : words1) {
            for (String word2 : words2) {
                if (word1.equals(word2) && word1.length() > 1) {
                    commonWords++;
                    break;
                }
            }
        }

        int totalWords = Math.max(words1.length, words2.length);
        return totalWords > 0 ? (double) commonWords / totalWords : 0.0;
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
                            .orderByDesc(com.secondbrain.entity.ReviewCard::getCreateTime)
                            .last("LIMIT 5")
            );

            List<String> questions = new ArrayList<>();
            for (com.secondbrain.entity.ReviewCard card : cards) {
                if (card.getQuestion() != null && !card.getQuestion().isEmpty()) {
                    String questionText = extractQuestionText(card.getQuestion());
                    if (questionText != null && !questionText.isEmpty()) {
                        questions.add(questionText);
                    }
                }
            }

            log.debug("获取已存在题目，nodeId：{}，数量：{}", nodeId, questions.size());
            return questions;
        } catch (Exception e) {
            log.error("获取已存在问题失败，nodeId：{}", nodeId, e);
            return new ArrayList<>();
        }
    }
    
    private boolean isOptionValid(String option) {
        if (option == null || option.trim().isEmpty()) {
            return false;
        }
        
        String trimmed = option.trim();
        
        if (trimmed.length() < 5 || trimmed.length() > 100) {
            return false;
        }
        
        if (trimmed.contains("选项") || trimmed.contains("选项A") || trimmed.contains("选项B") || 
            trimmed.contains("选项C") || trimmed.contains("选项D") || 
            trimmed.contains("相关概念") || trimmed.contains("占位符")) {
            return false;
        }
        
        if (trimmed.equals("A") || trimmed.equals("B") || trimmed.equals("C") || trimmed.equals("D")) {
            return false;
        }
        
        if (!trimmed.contains("是") && !trimmed.contains("的") && !trimmed.contains("可以") && 
            !trimmed.contains("能够") && !trimmed.contains("会") && !trimmed.contains("不") &&
            !trimmed.contains("有") && !trimmed.contains("为") && !trimmed.contains("在")) {
            log.warn("选项可能不是完整句子：{}", trimmed);
            return false;
        }
        
        return true;
    }

    private String extractQuestionText(String fullQuestion) {
        if (fullQuestion == null || fullQuestion.isEmpty()) {
            return null;
        }

        String[] lines = fullQuestion.split("\n");
        StringBuilder questionText = new StringBuilder();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            if (line.matches("^[A-D][.、].*")) {
                break;
            }

            if (line.startsWith("正确答案：") || line.startsWith("解析：")) {
                break;
            }

            if (questionText.length() > 0) {
                questionText.append(" ");
            }
            questionText.append(line);
        }

        return questionText.toString().trim();
    }
}
