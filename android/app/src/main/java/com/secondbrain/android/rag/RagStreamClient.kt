package com.secondbrain.android.rag

import com.secondbrain.android.BuildConfig
import com.secondbrain.android.data.remote.RagRequest
import com.secondbrain.android.data.session.SessionStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSource
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class RagStreamEvent(val name: String, val data: String)

fun interface RagAnswerSource { fun answer(request: RagRequest): Flow<RagStreamEvent> }

/** 网络回调负责读取流，取消收集时关闭连接，避免阻塞界面或残留后台请求。 */
@Singleton
class RagStreamClient @Inject constructor(
    private val client: OkHttpClient,
    private val sessionStore: SessionStore
) : RagAnswerSource {
    private val adapter = Moshi.Builder().add(KotlinJsonAdapterFactory()).build().adapter(RagRequest::class.java)

    override fun answer(request: RagRequest): Flow<RagStreamEvent> = callbackFlow {
        val token = sessionStore.token()
        val httpRequest = Request.Builder()
            .url("${BuildConfig.API_BASE_URL}rag/answer/stream")
            .header("Accept", "text/event-stream")
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .post(adapter.toJson(request).toRequestBody("application/json".toMediaType()))
            .build()
        val call = client.newCall(httpRequest)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, error: IOException) { close(error) }
            override fun onResponse(call: Call, response: Response) {
                try {
                    response.use {
                        if (!it.isSuccessful) throw IOException("问答服务暂时不可用（${it.code}），请稍后重试")
                        val source = it.body?.source() ?: throw IOException("服务没有返回回答")
                        readRagEvents(source) { event ->
                            trySendBlocking(event).isSuccess && event.name != "done"
                        }
                    }
                    close()
                } catch (error: Exception) { close(error) }
            }
        })
        awaitClose { call.cancel() }
    }
}

/** SSE 按空行分帧，保留 token 的空格和换行，防止代码及英文单词粘连。 */
internal fun readRagEvents(source: BufferedSource, consume: (RagStreamEvent) -> Boolean) {
    var name = "message"
    val data = mutableListOf<String>()
    while (!source.exhausted()) {
        val line = source.readUtf8Line() ?: break
        if (line.isEmpty()) {
            if (data.isNotEmpty() && !consume(RagStreamEvent(name, data.joinToString("\n")))) return
            name = "message"
            data.clear()
        } else if (!line.startsWith(":")) {
            val field = line.substringBefore(':')
            val value = line.substringAfter(':', "").removePrefix(" ")
            when (field) {
                "event" -> name = value.ifEmpty { "message" }
                "data" -> data.add(value)
            }
        }
    }
}
