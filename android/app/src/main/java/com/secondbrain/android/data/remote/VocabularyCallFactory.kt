package com.secondbrain.android.data.remote

import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/** Gives multi-stage article writing a bounded budget without delaying ordinary API failures. */
class VocabularyCallFactory(private val client: OkHttpClient) : Call.Factory {
    // Initial writing, two revisions and a supplement can each wait up to two minutes on the server.
    private val writingClient = client.newBuilder()
        .readTimeout(10, TimeUnit.MINUTES)
        .callTimeout(630, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private val translationClient = client.newBuilder()
        .readTimeout(330, TimeUnit.SECONDS)
        .callTimeout(360, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private val wordClient = client.newBuilder()
        .readTimeout(150, TimeUnit.SECONDS)
        .callTimeout(180, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    override fun newCall(request: Request): Call {
        val writing = request.method == "POST" && WRITING_PATH.containsMatchIn(request.url.encodedPath)
        val translating = request.method == "POST" && TRANSLATION_PATH.containsMatchIn(request.url.encodedPath)
        val wordLookup = request.method == "POST" && WORD_PATH.containsMatchIn(request.url.encodedPath)
        return when {
            writing -> writingClient
            translating -> translationClient
            wordLookup -> wordClient
            else -> client
        }.newCall(request)
    }

    private companion object {
        val WRITING_PATH = Regex("/vocabulary/articles(?:/[0-9]+/complete)?/?$")
        val TRANSLATION_PATH = Regex("/vocabulary/articles/[0-9]+/translation/?$")
        val WORD_PATH = Regex("/vocabulary/articles/[0-9]+/word-meaning/?$")
    }
}
