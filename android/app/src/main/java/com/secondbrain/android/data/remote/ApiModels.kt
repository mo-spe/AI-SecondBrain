package com.secondbrain.android.data.remote

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ApiResult<T>(val code: Int, val message: String, val data: T? = null)

@JsonClass(generateAdapter = true)
data class LoginRequest(val username: String, val password: String)

@JsonClass(generateAdapter = true)
data class LoginResponse(val token: String, val userInfo: UserInfo)

@JsonClass(generateAdapter = true)
data class UserInfo(val id: Long, val username: String, val bio: String? = null, val role: String? = null)

@JsonClass(generateAdapter = true)
data class Workspace(val id: Long, val name: String, val description: String? = null, val role: String? = null)

@JsonClass(generateAdapter = true)
data class KnowledgeNode(
    val id: Long, val title: String, val summary: String? = null, val contentMd: String? = null,
    val importance: Int? = null, val masteryLevel: Int? = null, val needReview: Int? = null
)

@JsonClass(generateAdapter = true)
data class KnowledgePage(val records: List<KnowledgeNode> = emptyList(), val total: Long = 0)

@JsonClass(generateAdapter = true)
data class CreateKnowledgeRequest(val title: String, val summary: String, val contentMd: String, val importance: Int = 3)

@JsonClass(generateAdapter = true)
data class WorkspaceSwitchResponse(val token: String, val workspaceId: Long? = null)

@JsonClass(generateAdapter = true)
data class ReviewCard(
    val id: Long,
    val nodeId: Long? = null,
    val nodeTitle: String? = null,
    val question: String,
    val answer: String? = null,
    val cardType: String? = null,
    val difficulty: Int? = null,
    val reviewCount: Int? = null,
    val masteryLevel: Int? = null,
    val nextReviewTime: String? = null,
    val status: Int? = null,
    val generationType: String? = null
)

@JsonClass(generateAdapter = true)
data class SubmitReviewRequest(val cardId: Long, val userAnswer: String, val duration: Int)

@JsonClass(generateAdapter = true)
data class ReviewResult(
    val isCorrect: Boolean,
    val correctAnswer: String? = null,
    val explanation: String? = null,
    val message: String? = null,
    val nextReviewTime: String? = null,
    val masteryLevel: Int? = null
)

@JsonClass(generateAdapter = true)
data class RagRequest(val question: String, val topK: Int = 3, val includeReferences: Boolean = true, val sessionId: Long? = null)

@JsonClass(generateAdapter = true)
data class ReviewPreference(val intervalDays: List<Int>, val reviewEmailEnabled: Boolean)

@JsonClass(generateAdapter = true)
data class ReviewReminder(val nodeId: Long, val scheduledAt: String, val status: String)

@JsonClass(generateAdapter = true)
data class ReminderRequest(val scheduledAt: String)

@JsonClass(generateAdapter = true)
data class SquarePost(val postId: Long, val nodeTitle: String? = null, val nodeSummary: String? = null, val recommendText: String? = null, val authorId: Long? = null, val authorName: String? = null, val likeCount: Int = 0, val commentCount: Int = 0, val bookmarkCount: Int = 0, val isLiked: Boolean? = false, val isBookmarked: Boolean? = false, val comments: List<SquareComment>? = null, val knowledgeNodes: List<SharedKnowledgeNode>? = null, val createdAt: String? = null)

@JsonClass(generateAdapter = true)
data class SharedKnowledgeNode(val nodeId: Long, val title: String? = null, val summary: String? = null, val contentMd: String? = null)

@JsonClass(generateAdapter = true)
data class SquareComment(val id: Long, val username: String? = null, val content: String, val createdAt: String? = null)

@JsonClass(generateAdapter = true)
data class CommunityQuestion(val id: Long, val title: String, val content: String? = null, val authorId: Long? = null, val authorName: String? = null, val answerCount: Int = 0, val tags: List<String>? = null, val answers: List<CommunityAnswer>? = null)

@JsonClass(generateAdapter = true)
data class CommunityAnswer(val id: Long, val authorName: String? = null, val content: String, val accepted: Boolean = false)

@JsonClass(generateAdapter = true)
data class CreateCommunityAnswerRequest(val content: String, val knowledgeNodeIds: List<Long> = emptyList())

@JsonClass(generateAdapter = true)
data class StudyTextRequest(val text: String)

@JsonClass(generateAdapter = true)
data class StudyAiSuggestion(
    val raw: String,
    val subject: String? = null,
    val chapter: String? = null,
    val knowledgePoints: List<String> = emptyList(),
    val errorType: String? = null,
    val confidence: Double? = null
)

@JsonClass(generateAdapter = true)
data class VisualQuestionSuggestion(
    val raw: String,
    val questionText: String = "",
    val subject: String = "",
    val chapter: String = "",
    val knowledgePoints: List<String> = emptyList(),
    val errorType: String = "不确定",
    val needsConfirmation: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AiProviderOption(val id: Long, val code: String, val name: String,
                            val apiType: String = "openai_compatible")

@JsonClass(generateAdapter = true)
data class AiScenarioConfig(val scenarioCode: String, val providerId: Long, val modelName: String,
                            val apiKey: String? = null)

@JsonClass(generateAdapter = true)
data class VocabularyCandidate(val word: String, val sourceImageIndex: Int, val uncertain: Boolean)

@JsonClass(generateAdapter = true)
data class VocabularyExtraction(val words: List<VocabularyCandidate> = emptyList())

@JsonClass(generateAdapter = true)
data class VisionReadiness(val ready: Boolean, val message: String)

@JsonClass(generateAdapter = true)
data class VocabularyGenerateRequest(val words: List<String>, val topic: String, val difficulty: String)

@JsonClass(generateAdapter = true)
data class VocabularyArticle(val id: Long, val words: List<String>, val article: String,
                             val meanings: Map<String, String> = emptyMap(),
                             val missingWords: List<String> = emptyList(),
                             val topic: String, val difficulty: String, val createTime: String = "")

@JsonClass(generateAdapter = true)
data class VocabularyArticleSummary(val id: Long, val topic: String, val difficulty: String,
                                    val wordCount: Int, val coveredCount: Int, val createTime: String = "")

@JsonClass(generateAdapter = true)
data class CreateWrongQuestionRequest(
    val imagePath: String,
    val ocrText: String? = null,
    val userAnswer: String? = null,
    val correctAnswer: String? = null,
    val explanation: String? = null,
    val subject: String? = null,
    val sourceBook: String? = null,
    val sourcePage: String? = null,
    val chapter: String? = null,
    val knowledgePoints: List<String> = emptyList(),
    val errorType: String? = null,
    val userNote: String? = null,
    val aiSuggestionJson: String? = null,
    val aiConfidence: Double? = null
)

@JsonClass(generateAdapter = true)
data class WrongQuestionRecord(
    val id: Long,
    val imagePath: String,
    val ocrText: String? = null,
    val userAnswer: String? = null,
    val correctAnswer: String? = null,
    val explanation: String? = null,
    val subject: String? = null,
    val sourceBook: String? = null,
    val sourcePage: String? = null,
    val chapter: String? = null,
    val knowledgePoints: String? = null,
    val errorType: String? = null,
    val userNote: String? = null,
    val aiSuggestionJson: String? = null,
    val aiConfidence: Double? = null,
    val reviewStatus: String,
    val nextReviewTime: String? = null,
    val lastReviewTime: String? = null,
    val createTime: String? = null
)

@JsonClass(generateAdapter = true)
data class WrongQuestionReviewLog(val id: Long, val result: String, val note: String? = null, val createTime: String? = null)

@JsonClass(generateAdapter = true)
data class WrongQuestionDetail(val record: WrongQuestionRecord, val reviews: List<WrongQuestionReviewLog> = emptyList())

@JsonClass(generateAdapter = true)
data class CreateDoubtRequest(
    val content: String,
    val imagePath: String? = null,
    val sourceBook: String? = null,
    val sourcePage: String? = null,
    val chapter: String? = null,
    val doubtType: String? = null
)

@JsonClass(generateAdapter = true)
data class DoubtRecord(
    val id: Long,
    val imagePath: String? = null,
    val content: String,
    val sourceBook: String? = null,
    val sourcePage: String? = null,
    val chapter: String? = null,
    val doubtType: String? = null,
    val status: String,
    val aiExplanationFeedback: String? = null,
    val nextProcessTime: String? = null,
    val createTime: String? = null
)

@JsonClass(generateAdapter = true)
data class DoubtUnderstandingRevision(
    val id: Long,
    val content: String,
    val understandingStatus: String,
    val createTime: String? = null
)

@JsonClass(generateAdapter = true)
data class DoubtDetail(val record: DoubtRecord, val understandings: List<DoubtUnderstandingRevision> = emptyList())

@JsonClass(generateAdapter = true)
data class AddUnderstandingRequest(val content: String, val understandingStatus: String = "INITIAL")

@JsonClass(generateAdapter = true)
data class UpdateDoubtRequest(
    val status: String? = null,
    val content: String? = null,
    val sourceBook: String? = null,
    val sourcePage: String? = null,
    val chapter: String? = null,
    val doubtType: String? = null,
    val aiExplanationFeedback: String? = null
)

@JsonClass(generateAdapter = true)
data class StudyScheduleRequest(val scheduledAt: String?)

@JsonClass(generateAdapter = true)
data class WrongQuestionReviewRequest(val result: String, val note: String? = null)

@JsonClass(generateAdapter = true)
data class UpdateWrongQuestionRequest(
    val subject: String? = null,
    val ocrText: String? = null,
    val userAnswer: String? = null,
    val correctAnswer: String? = null,
    val explanation: String? = null,
    val sourceBook: String? = null,
    val sourcePage: String? = null,
    val chapter: String? = null,
    val knowledgePoints: List<String> = emptyList(),
    val errorType: String? = null,
    val userNote: String? = null
)

@JsonClass(generateAdapter = true)
data class TodayStudyArchives(val wrongQuestions: List<WrongQuestionRecord> = emptyList(), val doubts: List<DoubtRecord> = emptyList())

@JsonClass(generateAdapter = true)
data class PageResult<T>(val records: List<T> = emptyList(), val total: Long = 0)

fun <T> ApiResult<T>.requireData(): T {
    check(code == 200) { message.ifBlank { "请求失败，请稍后重试" } }
    return data ?: error(message.ifBlank { "服务未返回可用数据" })
}

/** 删除等操作成功时允许空载荷，仍必须校验业务状态码。 */
fun ApiResult<*>.requireSuccess() {
    check(code == 200) { message.ifBlank { "请求失败，请稍后重试" } }
}
