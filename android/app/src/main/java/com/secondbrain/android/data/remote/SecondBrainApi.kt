package com.secondbrain.android.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.PATCH
import retrofit2.http.Streaming
import retrofit2.Response
import okhttp3.ResponseBody

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

    @Multipart
    @POST("study/media")
    suspend fun uploadStudyImage(@Part file: MultipartBody.Part): ApiResult<String>

    @POST("study/wrong-questions/suggestions")
    suspend fun suggestWrongQuestion(@Body request: StudyTextRequest): ApiResult<StudyAiSuggestion>

    @Multipart
    @POST("study/wrong-questions/recognize")
    suspend fun recognizeWrongQuestion(@Part file: MultipartBody.Part): ApiResult<VisualQuestionSuggestion>

    @GET("ai/providers")
    suspend fun aiProviders(): ApiResult<List<AiProviderOption>>

    @GET("user/ai-config")
    suspend fun aiConfigurations(): ApiResult<List<AiScenarioConfig>>

    @PUT("user/ai-config")
    suspend fun saveAiConfigurations(@Body configs: List<AiScenarioConfig>): ApiResult<Any?>

    @Multipart
    @POST("vocabulary/articles/extract")
    suspend fun extractVocabulary(@Part files: List<MultipartBody.Part>): ApiResult<VocabularyExtraction>

    @GET("vocabulary/articles/vision-ready")
    suspend fun vocabularyVisionReady(): ApiResult<VisionReadiness>

    @POST("vocabulary/articles")
    suspend fun generateVocabularyArticle(@Body request: VocabularyGenerateRequest): ApiResult<VocabularyArticle>

    @GET("vocabulary/articles")
    suspend fun vocabularyArticles(): ApiResult<List<VocabularyArticleSummary>>

    @GET("vocabulary/articles/{id}")
    suspend fun vocabularyArticle(@Path("id") id: Long): ApiResult<VocabularyArticle>

    @GET("study/wrong-questions")
    suspend fun wrongQuestions(@Query("status") status: String? = null, @Query("keyword") keyword: String? = null): ApiResult<List<WrongQuestionRecord>>

    @POST("study/wrong-questions")
    suspend fun createWrongQuestion(@Body request: CreateWrongQuestionRequest): ApiResult<WrongQuestionRecord>

    @GET("study/wrong-questions/{id}")
    suspend fun wrongQuestion(@Path("id") id: Long): ApiResult<WrongQuestionDetail>

    @PATCH("study/wrong-questions/{id}")
    suspend fun updateWrongQuestion(@Path("id") id: Long, @Body request: UpdateWrongQuestionRequest): ApiResult<WrongQuestionRecord>

    @POST("study/wrong-questions/{id}/schedule")
    suspend fun scheduleWrongQuestion(@Path("id") id: Long, @Body request: StudyScheduleRequest): ApiResult<WrongQuestionRecord>

    @POST("study/wrong-questions/{id}/reviews")
    suspend fun reviewWrongQuestion(@Path("id") id: Long, @Body request: WrongQuestionReviewRequest): ApiResult<WrongQuestionRecord>

    @POST("study/wrong-questions/{id}/master")
    suspend fun masterWrongQuestion(@Path("id") id: Long): ApiResult<WrongQuestionRecord>

    @DELETE("study/wrong-questions/{id}")
    suspend fun deleteWrongQuestion(@Path("id") id: Long): ApiResult<Any?>

    @GET("study/doubts")
    suspend fun doubts(@Query("status") status: String? = null, @Query("keyword") keyword: String? = null): ApiResult<List<DoubtRecord>>

    @POST("study/doubts")
    suspend fun createDoubt(@Body request: CreateDoubtRequest): ApiResult<DoubtRecord>

    @GET("study/doubts/{id}")
    suspend fun doubt(@Path("id") id: Long): ApiResult<DoubtDetail>

    @POST("study/doubts/{id}/understandings")
    suspend fun addUnderstanding(@Path("id") id: Long, @Body request: AddUnderstandingRequest): ApiResult<DoubtDetail>

    @POST("study/doubts/{id}/analyze")
    suspend fun explainDoubt(@Path("id") id: Long): ApiResult<String>

    @POST("study/doubts/{id}/schedule")
    suspend fun scheduleDoubt(@Path("id") id: Long, @Body request: StudyScheduleRequest): ApiResult<DoubtRecord>

    @PATCH("study/doubts/{id}")
    suspend fun updateDoubt(@Path("id") id: Long, @Body request: UpdateDoubtRequest): ApiResult<DoubtRecord>

    @DELETE("study/doubts/{id}")
    suspend fun deleteDoubt(@Path("id") id: Long): ApiResult<Any?>

    @GET("study/today")
    suspend fun todayStudyArchives(): ApiResult<TodayStudyArchives>

    @Streaming
    @GET("study/media/{fileName}")
    suspend fun studyImage(@Path("fileName") fileName: String): Response<ResponseBody>
}
