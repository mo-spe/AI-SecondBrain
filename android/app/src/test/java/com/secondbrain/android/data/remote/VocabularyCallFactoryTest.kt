package com.secondbrain.android.data.remote

import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class VocabularyCallFactoryTest {
    private var readBudget = 0
    private val client = OkHttpClient.Builder()
        .readTimeout(120, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            readBudget = chain.readTimeoutMillis()
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(200).message("OK").body("{}".toResponseBody()).build()
        }
        .build()
    private val factory = VocabularyCallFactory(client)

    @Test fun writingAndCompletionHaveFiniteLongBudgets() {
        listOf("/api/vocabulary/articles", "/vocabulary/articles/", "/api/vocabulary/articles/12/complete")
            .forEach {
                assertEquals(TimeUnit.SECONDS.toNanos(630), callBudget("POST", it))
                assertEquals(600_000, readBudget)
            }
    }

    @Test fun historyExtractionAndUnrelatedPostsKeepOriginalBudget() {
        listOf("GET" to "/api/vocabulary/articles", "GET" to "/api/vocabulary/articles/12",
            "POST" to "/api/vocabulary/articles/extract", "POST" to "/api/vocabulary/articles/12",
            "POST" to "/api/vocabulary/articles/12/complete/other", "POST" to "/api/knowledge")
            .forEach { (method, path) ->
                assertEquals(TimeUnit.SECONDS.toNanos(45), callBudget(method, path))
                assertEquals(120_000, readBudget)
            }
        assertEquals(120_000, client.readTimeoutMillis)
    }

    @Test fun translationAllowsOneRepairAndDoesNotExtendHistoryRequests() {
        assertEquals(TimeUnit.SECONDS.toNanos(360), callBudget("POST", "/api/vocabulary/articles/12/translation"))
        assertEquals(330_000, readBudget)
        assertEquals(TimeUnit.SECONDS.toNanos(45), callBudget("GET", "/api/vocabulary/articles/12/translation"))
        assertEquals(120_000, readBudget)
    }

    private fun callBudget(method: String, path: String): Long {
        val request = Request.Builder().url("https://example.invalid$path")
            .method(method, if (method == "POST") "{}".toRequestBody() else null).build()
        val call = factory.newCall(request)
        val budget = call.timeout().timeoutNanos()
        call.execute().close()
        return budget
    }

    @Test fun wordLookupWaitsForOneModelCallWithoutExtendingOtherRequests() {
        assertEquals(TimeUnit.SECONDS.toNanos(180), callBudget("POST", "/api/vocabulary/articles/12/word-meaning"))
        assertEquals(150_000, readBudget)
        assertEquals(TimeUnit.SECONDS.toNanos(45), callBudget("GET", "/api/vocabulary/articles/12/word-meaning"))
        assertEquals(120_000, readBudget)
    }
}
