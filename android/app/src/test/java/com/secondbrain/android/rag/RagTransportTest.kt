package com.secondbrain.android.rag

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.secondbrain.android.data.remote.RagRequest
import com.secondbrain.android.data.session.SessionStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class RagTransportTest {
    @Test fun requestRunsOffCollectorThreadAndPreservesPayloadAndCredentials() = runBlocking {
        val collectorThread = Thread.currentThread()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            assertNotSame(collectorThread, Thread.currentThread())
            assertEquals("Bearer test-token", chain.request().header("Authorization"))
            assertEquals("text/event-stream", chain.request().header("Accept"))
            val body = Buffer().also { chain.request().body!!.writeTo(it) }.readUtf8()
            assertTrue(body.contains("空格 "))
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("event: token\ndata: answer \n\nevent: done\ndata: ok\n\n".toResponseBody()).build()
        }.build()
        try {
            val session = session().also { it.save("test-token") }
            val events = withTimeout(5000) { RagStreamClient(client, session).answer(RagRequest("空格 ")).toList() }
            assertEquals(listOf(RagStreamEvent("token", "answer "), RagStreamEvent("done", "ok")), events)
        } finally { client.dispatcher.executorService.shutdownNow(); client.connectionPool.evictAll() }
    }

    @Test fun cancellingCollectorCancelsPendingHttpCall() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val cancelled = CountDownLatch(1)
        val client = OkHttpClient.Builder().eventListener(object : EventListener() {
            override fun canceled(call: Call) { cancelled.countDown() }
        }).addInterceptor { chain ->
            entered.complete(Unit)
            check(cancelled.await(5, TimeUnit.SECONDS)) { "Request was not cancelled" }
            throw java.io.IOException("cancelled")
        }.build()
        try {
            val job = launch { RagStreamClient(client, session()).answer(RagRequest("问题")).collect() }
            withTimeout(5000) { entered.await(); job.cancelAndJoin() }
            assertEquals(0L, cancelled.count)
        } finally { client.dispatcher.executorService.shutdownNow(); client.connectionPool.evictAll() }
    }

    private fun session(): SessionStore {
        val preferences = MutableStateFlow<Preferences>(emptyPreferences())
        return SessionStore(object : DataStore<Preferences> {
            override val data = preferences
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = transform(preferences.value).also { preferences.value = it }
        })
    }
}
