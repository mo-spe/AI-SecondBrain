package com.secondbrain.android.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class ApiModelsTest {
    @Test
    fun requireDataReturnsPayloadForSuccessfulResponse() {
        val result = ApiResult(code = 200, message = "ok", data = "knowledge")

        assertEquals("knowledge", result.requireData())
    }

    @Test
    fun requireDataPreservesServiceMessageForMissingPayload() {
        val result = ApiResult<String>(code = 500, message = "服务暂不可用")

        val error = assertThrows(IllegalStateException::class.java) { result.requireData() }

        assertEquals("服务暂不可用", error.message)
    }

    @Test
    fun squareListAcceptsNullCommentsFromServer() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val pageType = Types.newParameterizedType(
            PageResult::class.java,
            SquarePost::class.java
        )
        val resultType = Types.newParameterizedType(ApiResult::class.java, pageType)
        val adapter = moshi.adapter<ApiResult<PageResult<SquarePost>>>(resultType)

        val result = adapter.fromJson(
            """{"code":200,"message":"ok","data":{"records":[{"postId":1,"comments":null}],"total":1}}"""
        )

        assertNull(result?.data?.records?.single()?.comments)
    }
    @Test fun rejectsErrorEnvelopeEvenWhenPayloadExists() {
        assertThrows(IllegalStateException::class.java) { ApiResult(403, "无权访问", "stale").requireData() }
    }

    @Test fun decodesReviewFeedbackAndSharedBody() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val result = moshi.adapter(ReviewResult::class.java).fromJson("""{"isCorrect":true,"correctAnswer":"B","explanation":"验证过的构件更稳定"}""")!!
        assertEquals(true, result.isCorrect)
        assertEquals("B", result.correctAnswer)
        val post = moshi.adapter(SquarePost::class.java).fromJson("""{"postId":1,"knowledgeNodes":[{"nodeId":8,"contentMd":"## 正文"}]}""")!!
        assertEquals("## 正文", post.knowledgeNodes!!.single().contentMd)
    }
    @Test fun retrofitDecodesCreatedAnswerAndEmptyReminderSuccess() = kotlinx.coroutines.runBlocking {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            val json = if (chain.request().method == "DELETE") """{"code":200,"message":"ok","data":null}"""
                else """{"code":200,"message":"ok","data":{"id":19,"authorName":"成员","content":"回答正文已经保存在服务端","accepted":false}}"""
            okhttp3.Response.Builder().request(chain.request()).protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK")
                .body(json.toResponseBody("application/json".toMediaType())).build()
        }.build()
        val api = retrofit2.Retrofit.Builder().baseUrl("https://isolated.invalid/api/").client(client)
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi)).build().create(SecondBrainApi::class.java)
        assertEquals(19L, api.answerQuestion(1, CreateCommunityAnswerRequest("回答正文已经保存在服务端")).requireData().id)
        api.cancelReminder(1).requireSuccess()
        assertThrows(IllegalStateException::class.java) { ApiResult<Any?>(403, "无权访问").requireSuccess() }
        Unit
    }
}
