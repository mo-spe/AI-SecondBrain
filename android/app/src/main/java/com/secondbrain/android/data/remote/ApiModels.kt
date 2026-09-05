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
data class ReviewCard(val id: Long, val nodeId: Long? = null, val nodeTitle: String? = null, val question: String, val answer: String? = null, val cardType: String? = null)

@JsonClass(generateAdapter = true)
data class SubmitReviewRequest(val cardId: Long, val userAnswer: String, val duration: Int)

@JsonClass(generateAdapter = true)
data class ReviewResult(val isCorrect: Boolean, val correctAnswer: String? = null, val explanation: String? = null, val message: String? = null)

@JsonClass(generateAdapter = true)
data class RagRequest(val question: String, val topK: Int = 3, val includeReferences: Boolean = true, val sessionId: Long? = null)

@JsonClass(generateAdapter = true)
data class ReviewPreference(val intervalDays: List<Int>, val reviewEmailEnabled: Boolean)

@JsonClass(generateAdapter = true)
data class ReviewReminder(val nodeId: Long, val scheduledAt: String, val status: String)

@JsonClass(generateAdapter = true)
data class ReminderRequest(val scheduledAt: String)

@JsonClass(generateAdapter = true)
data class SquarePost(val postId: Long, val nodeTitle: String? = null, val nodeSummary: String? = null, val recommendText: String? = null, val authorId: Long? = null, val authorName: String? = null, val likeCount: Int = 0, val commentCount: Int = 0, val bookmarkCount: Int = 0, val isLiked: Boolean = false, val isBookmarked: Boolean = false, val comments: List<SquareComment>? = null, val knowledgeNodes: List<SharedKnowledgeNode>? = null, val createdAt: String? = null)

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
data class PageResult<T>(val records: List<T> = emptyList(), val total: Long = 0)

fun <T> ApiResult<T>.requireData(): T {
    check(code == 200) { message.ifBlank { "请求失败，请稍后重试" } }
    return data ?: error(message.ifBlank { "服务未返回可用数据" })
}

/** 删除等操作成功时允许空载荷，仍必须校验业务状态码。 */
fun ApiResult<*>.requireSuccess() {
    check(code == 200) { message.ifBlank { "请求失败，请稍后重试" } }
}
