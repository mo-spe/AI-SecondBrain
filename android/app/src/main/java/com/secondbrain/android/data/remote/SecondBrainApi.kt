package com.secondbrain.android.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query

interface SecondBrainApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): ApiResult<LoginResponse>

    @GET("workspace")
    suspend fun workspaces(): ApiResult<List<Workspace>>

    @PUT("workspace/{id}/switch")
    suspend fun switchWorkspace(@Path("id") id: Long): ApiResult<WorkspaceSwitchResponse>

    @PUT("workspace/personal")
    suspend fun switchPersonalWorkspace(): ApiResult<WorkspaceSwitchResponse>

    @GET("knowledge/list")
    suspend fun knowledge(@Query("current") page: Int = 1, @Query("size") size: Int = 20, @Query("keyword") keyword: String? = null): ApiResult<KnowledgePage>

    @GET("knowledge/{id}")
    suspend fun knowledgeDetail(@Path("id") id: Long): ApiResult<KnowledgeNode>

    @POST("knowledge")
    suspend fun createKnowledge(@Body request: CreateKnowledgeRequest): ApiResult<KnowledgeNode>

    @GET("review/today")
    suspend fun todayReview(): ApiResult<List<ReviewCard>>

    @POST("review/submit")
    suspend fun submitReview(@Body request: SubmitReviewRequest): ApiResult<ReviewResult>

    @GET("review/preferences")
    suspend fun reviewPreference(): ApiResult<ReviewPreference>

    @PUT("review/preferences")
    suspend fun updateReviewPreference(@Body request: ReviewPreference): ApiResult<ReviewPreference>

    @GET("review/reminders")
    suspend fun reminders(): ApiResult<List<ReviewReminder>>

    @PUT("review/reminders/nodes/{nodeId}")
    suspend fun saveReminder(@Path("nodeId") nodeId: Long, @Body request: ReminderRequest): ApiResult<ReviewReminder>

    @DELETE("review/reminders/nodes/{nodeId}")
    suspend fun cancelReminder(@Path("nodeId") nodeId: Long): ApiResult<Any?>

    @GET("square/list")
    suspend fun square(@Query("current") current: Int = 1, @Query("size") size: Int = 20): ApiResult<PageResult<SquarePost>>

    @GET("square/{id}")
    suspend fun squareDetail(@Path("id") id: Long): ApiResult<SquarePost>

    @POST("square/{id}/like")
    suspend fun toggleLike(@Path("id") id: Long): ApiResult<Boolean>

    @POST("square/{id}/bookmark")
    suspend fun toggleBookmark(@Path("id") id: Long): ApiResult<Boolean>

    @GET("community/questions")
    suspend fun questions(@Query("current") current: Int = 1, @Query("size") size: Int = 20): ApiResult<PageResult<CommunityQuestion>>

    @GET("community/questions/{id}")
    suspend fun questionDetail(@Path("id") id: Long): ApiResult<CommunityQuestion>

    @POST("community/questions/{id}/answers")
    suspend fun answerQuestion(@Path("id") id: Long, @Body request: CreateCommunityAnswerRequest): ApiResult<CommunityAnswer>
}
