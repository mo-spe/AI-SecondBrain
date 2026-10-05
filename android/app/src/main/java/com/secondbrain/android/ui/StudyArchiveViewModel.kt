package com.secondbrain.android.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.android.capture.OcrRecognizer
import com.secondbrain.android.capture.StudyArchiveDraft
import com.secondbrain.android.capture.StudyArchiveDraftDao
import com.secondbrain.android.data.remote.AddUnderstandingRequest
import com.secondbrain.android.data.remote.ApiResult
import com.secondbrain.android.data.remote.CreateDoubtRequest
import com.secondbrain.android.data.remote.CreateWrongQuestionRequest
import com.secondbrain.android.data.remote.DoubtDetail
import com.secondbrain.android.data.remote.DoubtRecord
import com.secondbrain.android.data.remote.SecondBrainApi
import com.secondbrain.android.data.remote.StudyAiSuggestion
import com.secondbrain.android.data.remote.VisualQuestionSuggestion
import com.secondbrain.android.data.remote.StudyScheduleRequest
import com.secondbrain.android.data.remote.StudyTextRequest
import com.secondbrain.android.data.remote.UpdateWrongQuestionRequest
import com.secondbrain.android.data.remote.TodayStudyArchives
import com.secondbrain.android.data.remote.WrongQuestionDetail
import com.secondbrain.android.data.remote.WrongQuestionRecord
import com.secondbrain.android.data.remote.WrongQuestionReviewRequest
import com.secondbrain.android.data.remote.requireData
import com.secondbrain.android.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class StudyArchiveUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val message: String? = null,
    val wrongQuestions: List<WrongQuestionRecord> = emptyList(),
    val doubts: List<DoubtRecord> = emptyList(),
    val today: TodayStudyArchives = TodayStudyArchives(),
    val drafts: List<StudyArchiveDraft> = emptyList(),
    val draft: StudyArchiveDraft? = null,
    val suggestion: StudyAiSuggestion? = null,
    val visualSuggestion: VisualQuestionSuggestion? = null,
    val wrongDetail: WrongQuestionDetail? = null,
    val doubtDetail: DoubtDetail? = null,
    val aiExplanation: String? = null,
    val imageBytes: ByteArray? = null,
    val imageMessage: String? = null,
    val imageLoading: Boolean = false
)

/** Owns the personal study archive flow, keeping a local draft until the server acknowledges its save. */
@HiltViewModel
class StudyArchiveViewModel @Inject constructor(
    private val api: SecondBrainApi,
    private val draftsDao: StudyArchiveDraftDao,
    sessionStore: SessionStore,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow(StudyArchiveUiState())
    val state: StateFlow<StudyArchiveUiState> = _state
    private val recognizer = OcrRecognizer()

    init {
        viewModelScope.launch { draftsDao.observeAll().collectLatest { _state.value = _state.value.copy(drafts = it) } }
        viewModelScope.launch {
            sessionStore.tokenFlow.distinctUntilChanged().collectLatest { token ->
                if (token.isNullOrBlank()) {
                    _state.value = _state.value.copy(
                        loading = false,
                        wrongQuestions = emptyList(),
                        doubts = emptyList(),
                        today = TodayStudyArchives(),
                        wrongDetail = null,
                        doubtDetail = null,
                        imageBytes = null,
                        imageMessage = null,
                        imageLoading = false,
                        message = "登录后即可同步个人错题和疑问；本机草稿仍会保留。"
                    )
                } else {
                    _state.value = _state.value.copy(
                        loading = true,
                        wrongQuestions = emptyList(),
                        doubts = emptyList(),
                        today = TodayStudyArchives(),
                        wrongDetail = null,
                        doubtDetail = null,
                        imageBytes = null,
                        imageMessage = null,
                        imageLoading = false
                    )
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = null)
            runCatching {
                Triple(api.wrongQuestions().requireData(), api.doubts().requireData(), api.todayStudyArchives().requireData())
            }.onSuccess { (wrong, doubts, today) ->
                _state.value = _state.value.copy(loading = false, wrongQuestions = wrong, doubts = doubts, today = today)
            }.onFailure { _state.value = _state.value.copy(loading = false, message = userFacingLoadError(it, "学习档案加载失败")) }
        }
    }

    fun beginDraft(type: String, uri: Uri?) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null, suggestion = null, visualSuggestion = null)
            try {
                val localFile = uri?.let { copyToPrivateStorage(it) }
                val draft = StudyArchiveDraft(
                    archiveType = type,
                    imagePath = localFile?.absolutePath.orEmpty()
                )
                val id = draftsDao.save(draft)
                var savedDraft = draft.copy(id = id)
                _state.value = _state.value.copy(draft = savedDraft, message = null)
                val recognized = uri?.let { runCatching { recognizer.recognize(context, it) }.getOrDefault("") }.orEmpty()
                if (recognized.isNotBlank()) {
                    savedDraft = if (type == "DOUBT") savedDraft.copy(ocrText = recognized, content = recognized)
                    else savedDraft.copy(ocrText = recognized)
                    draftsDao.save(savedDraft)
                }
                _state.value = _state.value.copy(saving = false, draft = savedDraft)
            } catch (error: Exception) {
                _state.value = _state.value.copy(saving = false, message = error.message ?: "无法建立本机草稿")
            }
        }
    }

    fun beginTextDoubt() {
        viewModelScope.launch {
            val id = draftsDao.save(StudyArchiveDraft(archiveType = "DOUBT", imagePath = ""))
            _state.value = _state.value.copy(draft = StudyArchiveDraft(id = id, archiveType = "DOUBT", imagePath = ""), message = null)
        }
    }

    fun showMessage(message: String) { _state.value = _state.value.copy(message = message) }

    fun editDraft(transform: (StudyArchiveDraft) -> StudyArchiveDraft) {
        val current = _state.value.draft ?: return
        val edited = transform(current)
        _state.value = _state.value.copy(draft = edited, message = null)
        viewModelScope.launch { runCatching { draftsDao.save(edited) } }
    }

    fun resumeDraft(draft: StudyArchiveDraft) {
        _state.value = _state.value.copy(draft = draft, message = null, suggestion = null, visualSuggestion = null)
    }

    fun cancelEditor() { _state.value = _state.value.copy(draft = null, suggestion = null, visualSuggestion = null, message = null) }

    fun requestVisualRecognition() {
        val draft = _state.value.draft ?: return
        if (draft.archiveType != "WRONG" || draft.imagePath.isBlank()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            runCatching {
                val compressed = withContext(Dispatchers.IO) { compressImageForUpload(File(draft.imagePath)) }
                try {
                    val part = MultipartBody.Part.createFormData("file", compressed.name,
                        compressed.asRequestBody("image/jpeg".toMediaType()))
                    api.recognizeWrongQuestion(part).requireData()
                } finally {
                    compressed.delete()
                }
            }.onSuccess { result ->
                val updated = draft.copy(
                    ocrText = result.questionText.ifBlank { draft.ocrText },
                    subject = result.subject.ifBlank { draft.subject },
                    chapter = result.chapter.ifBlank { draft.chapter },
                    knowledgePoints = result.knowledgePoints.takeIf(List<String>::isNotEmpty)
                        ?.joinToString("，") ?: draft.knowledgePoints,
                    errorType = result.errorType.takeIf { it != "不确定" } ?: draft.errorType,
                    aiRaw = result.raw
                )
                draftsDao.save(updated)
                _state.value = _state.value.copy(saving = false, draft = updated, visualSuggestion = result,
                    message = "视觉识别是候选结果，请对照原图核对题干、公式和题号。")
            }.onFailure {
                _state.value = _state.value.copy(saving = false,
                    message = userFacingLoadError(it, "精准识别失败。请在“我的”中配置视觉模型和个人 API Key，或继续使用 OCR。"))
            }
        }
    }

    fun requestSuggestion() {
        val draft = _state.value.draft ?: return
        if (draft.ocrText.isBlank()) {
            _state.value = _state.value.copy(message = "先拍摄或填写题目文字，再请求 AI 整理建议")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            runCatching { api.suggestWrongQuestion(StudyTextRequest(draft.ocrText)).requireData() }
                .onSuccess { suggestion ->
                    val updated = draft.copy(
                        subject = suggestion.subject?.takeIf { it.isNotBlank() } ?: draft.subject,
                        chapter = suggestion.chapter?.takeIf { it.isNotBlank() } ?: draft.chapter,
                        errorType = suggestion.errorType?.takeIf { it.isNotBlank() } ?: draft.errorType,
                        knowledgePoints = suggestion.knowledgePoints.joinToString("，"),
                        aiRaw = suggestion.raw,
                        aiConfidence = suggestion.confidence
                    )
                    draftsDao.save(updated)
                    _state.value = _state.value.copy(saving = false, draft = updated, suggestion = suggestion)
                }
                .onFailure { _state.value = _state.value.copy(saving = false, message = userFacingLoadError(it, "AI 建议暂时不可用，可手动填写")) }
        }
    }

    fun saveDraft() {
        val draft = _state.value.draft ?: return
        if (_state.value.saving) return
        if (draft.archiveType == "WRONG" && draft.imagePath.isBlank()) {
            _state.value = _state.value.copy(message = "错题需要至少保存一张原题照片")
            return
        }
        if (draft.archiveType == "DOUBT" && draft.content.isBlank()) {
            _state.value = _state.value.copy(message = "请写下当前的问题，方便之后回来处理")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            runCatching {
                val imageName = draft.imagePath.takeIf { it.isNotBlank() }?.let { uploadImage(File(it)) }
                if (draft.archiveType == "WRONG") {
                    api.createWrongQuestion(
                        CreateWrongQuestionRequest(
                            imagePath = imageName ?: error("错题原图上传失败"),
                            ocrText = draft.ocrText.ifBlank { null }, subject = draft.subject,
                            userAnswer = draft.userAnswer.ifBlank { null },
                            correctAnswer = draft.correctAnswer.ifBlank { null },
                            explanation = draft.explanation.ifBlank { null },
                            sourceBook = draft.sourceBook.ifBlank { null }, sourcePage = draft.sourcePage.ifBlank { null },
                            chapter = draft.chapter.ifBlank { null },
                            knowledgePoints = draft.knowledgePoints.split(',', '，').map(String::trim).filter(String::isNotBlank),
                            errorType = draft.errorType.ifBlank { null }, userNote = draft.userNote.ifBlank { null },
                            aiSuggestionJson = draft.aiRaw.ifBlank { null }, aiConfidence = draft.aiConfidence
                        )
                    ).requireData()
                } else {
                    api.createDoubt(
                        CreateDoubtRequest(
                            content = draft.content.trim(), imagePath = imageName,
                            sourceBook = draft.sourceBook.ifBlank { null }, sourcePage = draft.sourcePage.ifBlank { null },
                            chapter = draft.chapter.ifBlank { null }, doubtType = draft.doubtType
                        )
                    ).requireData()
                }
            }.onSuccess {
                draftsDao.delete(draft.id)
                draft.imagePath.takeIf(String::isNotBlank)?.let { File(it).delete() }
                _state.value = _state.value.copy(saving = false, draft = null, message = "已保存到个人学习档案")
                refresh()
            }.onFailure { _state.value = _state.value.copy(saving = false, message = userFacingLoadError(it, "保存失败，草稿已保留")) }
        }
    }

    fun openWrong(id: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = null, imageBytes = null, imageMessage = null, imageLoading = true)
            runCatching { api.wrongQuestion(id).requireData() }.onSuccess { detail ->
                val (image, imageMessage) = loadStudyImage(detail.record.imagePath)
                _state.value = _state.value.copy(loading = false, wrongDetail = detail, doubtDetail = null, imageBytes = image, imageMessage = imageMessage, imageLoading = false)
            }.onFailure { _state.value = _state.value.copy(loading = false, imageLoading = false, message = userFacingLoadError(it, "错题读取失败")) }
        }
    }

    fun retryWrongImage() {
        val fileName = _state.value.wrongDetail?.record?.imagePath ?: return
        retryStudyImage(fileName)
    }

    fun openDoubt(id: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, message = null, imageBytes = null, imageMessage = null, imageLoading = true, aiExplanation = null)
            runCatching { api.doubt(id).requireData() }.onSuccess { detail ->
                val (image, imageMessage) = detail.record.imagePath?.takeIf(String::isNotBlank)
                    ?.let { loadStudyImage(it) } ?: (null to null)
                _state.value = _state.value.copy(loading = false, doubtDetail = detail, wrongDetail = null, imageBytes = image, imageMessage = imageMessage, imageLoading = false)
            }.onFailure { _state.value = _state.value.copy(loading = false, imageLoading = false, message = userFacingLoadError(it, "疑问读取失败")) }
        }
    }

    fun retryDoubtImage() {
        val fileName = _state.value.doubtDetail?.record?.imagePath?.takeIf(String::isNotBlank) ?: return
        retryStudyImage(fileName)
    }

    fun scheduleWrong(id: Long, days: Int?) = scheduleWrongAt(id, days?.let(::scheduleTime))
    fun scheduleWrongAt(id: Long, scheduledAt: String?) = mutate { api.scheduleWrongQuestion(id, StudyScheduleRequest(scheduledAt)).requireData() }
    fun masterWrong(id: Long) = mutate { api.masterWrongQuestion(id).requireData() }
    fun recordWrongReview(id: Long, result: String, note: String) = mutate { api.reviewWrongQuestion(id, WrongQuestionReviewRequest(result, note.ifBlank { null })).requireData() }
    fun updateWrong(id: Long, request: UpdateWrongQuestionRequest) = mutate { api.updateWrongQuestion(id, request).requireData() }
    fun updateDoubt(id: Long, status: String) = updateDoubt(id, com.secondbrain.android.data.remote.UpdateDoubtRequest(status = status))
    fun updateDoubt(id: Long, request: com.secondbrain.android.data.remote.UpdateDoubtRequest) = mutate { api.updateDoubt(id, request).requireData() }
    fun setAiExplanationFeedback(id: Long, feedback: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            runCatching {
                api.updateDoubt(id, com.secondbrain.android.data.remote.UpdateDoubtRequest(aiExplanationFeedback = feedback)).requireData()
            }.onSuccess { updated ->
                _state.value = _state.value.copy(
                    saving = false,
                    message = "已记录反馈",
                    doubtDetail = _state.value.doubtDetail?.copy(record = updated)
                )
                refresh()
            }.onFailure {
                _state.value = _state.value.copy(saving = false, message = userFacingLoadError(it, "反馈保存失败"))
            }
        }
    }
    fun scheduleDoubt(id: Long, days: Int?) = scheduleDoubtAt(id, days?.let(::scheduleTime))
    fun scheduleDoubtAt(id: Long, scheduledAt: String?) = mutate { api.scheduleDoubt(id, StudyScheduleRequest(scheduledAt)).requireData() }
    fun addUnderstanding(id: Long, content: String, status: String) = mutate { api.addUnderstanding(id, AddUnderstandingRequest(content, status)).requireData() }
    fun resolveDoubt(id: Long) = mutate { api.updateDoubt(id, com.secondbrain.android.data.remote.UpdateDoubtRequest("RESOLVED")).requireData() }

    fun explainDoubt(id: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            runCatching { api.explainDoubt(id).requireData() }.onSuccess { _state.value = _state.value.copy(saving = false, aiExplanation = it) }
                .onFailure { _state.value = _state.value.copy(saving = false, message = userFacingLoadError(it, "AI 解释暂时不可用")) }
        }
    }

    fun closeDetail() { _state.value = _state.value.copy(wrongDetail = null, doubtDetail = null, imageBytes = null, imageMessage = null, imageLoading = false, aiExplanation = null); refresh() }

    private fun retryStudyImage(fileName: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(imageBytes = null, imageMessage = null, imageLoading = true)
            val (image, imageMessage) = loadStudyImage(fileName)
            _state.value = _state.value.copy(imageBytes = image, imageMessage = imageMessage, imageLoading = false)
        }
    }

    private fun mutate(block: suspend () -> Any) {
        viewModelScope.launch {
            _state.value = _state.value.copy(saving = true, message = null)
            runCatching { block() }.onSuccess {
                _state.value = _state.value.copy(saving = false, message = "已更新")
                val wrongId = _state.value.wrongDetail?.record?.id
                val doubtId = _state.value.doubtDetail?.record?.id
                refresh()
                when {
                    wrongId != null -> openWrong(wrongId)
                    doubtId != null -> openDoubt(doubtId)
                }
            }
                .onFailure { _state.value = _state.value.copy(saving = false, message = userFacingLoadError(it, "操作失败")) }
        }
    }

    private suspend fun uploadImage(file: File): String {
        check(file.exists() && file.isFile) { "本机图片草稿不存在，请重新拍摄" }
        val compressedFile = withContext(Dispatchers.IO) { compressImageForUpload(file) }
        return try {
            val part = MultipartBody.Part.createFormData("file", compressedFile.name, compressedFile.asRequestBody("image/jpeg".toMediaType()))
            api.uploadStudyImage(part).requireData()
        } finally {
            compressedFile.delete()
        }
    }

    private suspend fun loadStudyImage(fileName: String): Pair<ByteArray?, String?> = try {
        val response = api.studyImage(fileName)
        if (!response.isSuccessful) {
            val message = when (response.code()) {
                401 -> "登录状态已失效，请重新登录后查看原图。"
                403 -> "当前账号没有读取这张原图的权限；错题记录仍会保留。"
                404 -> "服务器找不到这张原图；错题记录仍会保留，请重新上传照片。"
                in 500..599 -> "服务器读取原图失败（HTTP ${response.code()}），请稍后重试。"
                else -> "原图读取失败（HTTP ${response.code()}），请重试。"
            }
            null to message
        } else {
            val body = response.body()
            if (body == null || body.contentType()?.type != "image") {
                null to "服务器没有返回图片文件；这条错题可能需要重新上传原图。"
            } else {
                val (bytes, bounds) = withContext(Dispatchers.IO) {
                    val imageBytes = body.bytes()
                    val imageBounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, imageBounds)
                    imageBytes to imageBounds
                }
                if (bounds.outWidth > 0 && bounds.outHeight > 0) bytes to null
                else null to "图片文件无法解码；请重新上传原题照片。"
            }
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        null to userFacingLoadError(error, "原题图片加载失败，请重新尝试。")
    }

    private fun compressImageForUpload(source: File): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法读取图片，请重新选择照片" }
        var sampleSize = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_UPLOAD_DIMENSION) sampleSize *= 2
        val bitmap = BitmapFactory.decodeFile(source.absolutePath, BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
        }) ?: error("无法读取图片，请重新选择照片")
        val orientation = runCatching {
            ExifInterface(source.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val transform = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
            }
        }
        val oriented = if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED) bitmap
        else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, transform, true).also { if (it !== bitmap) bitmap.recycle() }
        val scale = minOf(1f, MAX_UPLOAD_DIMENSION.toFloat() / maxOf(oriented.width, oriented.height))
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(
            oriented,
            (oriented.width * scale).toInt().coerceAtLeast(1),
            (oriented.height * scale).toInt().coerceAtLeast(1),
            true
        ).also { if (it !== oriented) oriented.recycle() } else oriented
        val output = File.createTempFile("study-upload-", ".jpg", source.parentFile)
        try {
            for (quality in UPLOAD_JPEG_QUALITIES) {
                output.outputStream().use { stream -> check(resized.compress(android.graphics.Bitmap.CompressFormat.JPEG, quality, stream)) }
                if (output.length() <= MAX_UPLOAD_BYTES) return output
            }
            error("照片仍超过上传限制；请换一张较小的照片后重试")
        } catch (error: Exception) {
            output.delete()
            throw error
        } finally {
            resized.recycle()
        }
    }

    private fun copyToPrivateStorage(uri: Uri): File {
        val extension = context.contentResolver.getType(uri)?.substringAfter('/')?.takeIf { it.length <= 6 } ?: "jpg"
        val destination = File(context.filesDir, "study-archive/${System.currentTimeMillis()}.$extension")
        destination.parentFile?.mkdirs()
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "无法读取所选图片" }.use { source -> destination.outputStream().use(source::copyTo) }
        }
        return destination
    }

    private fun scheduleTime(days: Int): String = LocalDate.now().plusDays(days.toLong()).atTime(9, 0)
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))

    private companion object {
        const val MAX_UPLOAD_DIMENSION = 2560
        const val MAX_UPLOAD_BYTES = 8L * 1024 * 1024
        val UPLOAD_JPEG_QUALITIES = listOf(90, 82, 74, 66)
    }
}
