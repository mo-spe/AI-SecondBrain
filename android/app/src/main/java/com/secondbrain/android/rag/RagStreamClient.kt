package com.secondbrain.android.rag

import com.secondbrain.android.BuildConfig
import com.secondbrain.android.data.remote.RagRequest
import com.secondbrain.android.data.session.SessionStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class RagStreamEvent(val name: String, val data: String)

/** Parses named SSE events for the existing POST stream without exposing model reasoning. */
@Singleton
class RagStreamClient @Inject constructor(
    private val client: OkHttpClient,
    private val sessionStore: SessionStore
) {
    fun answer(request: RagRequest): Flow<RagStreamEvent> = flow {
        val payload = JSONObject().apply {
            put("question", request.question)
            put("topK", request.topK)
            put("includeReferences", request.includeReferences)
            request.sessionId?.let { put("sessionId", it) }
        }.toString().toRequestBody("application/json".toMediaType())
        val token = runBlocking { sessionStore.token() }
        val httpRequest = Request.Builder()
            .url("${BuildConfig.API_BASE_URL}rag/answer/stream")
            .header("Accept", "text/event-stream")
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .post(payload)
            .build()
        client.newCall(httpRequest).execute().use { response ->
            if (!response.isSuccessful) {
                val detail = response.body?.string()?.take(160).orEmpty()
                val suffix = if (detail.isBlank()) "" else "：$detail"
                throw IOException("问答服务暂时不可用（${response.code}）$suffix")
            }
            val source = requireNotNull(response.body).source()
            var eventName = "message"
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                when {
                    line.startsWith("event:") -> eventName = line.removePrefix("event:").trim()
                    line.startsWith("data:") -> emit(RagStreamEvent(eventName, line.removePrefix("data:").trim()))
                    line.isBlank() -> eventName = "message"
                }
            }
        }
    }
}
