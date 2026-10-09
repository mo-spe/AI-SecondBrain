package com.secondbrain.android.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.android.data.remote.SecondBrainApi
import com.secondbrain.android.data.remote.VocabularyArticle
import com.secondbrain.android.data.remote.VocabularyArticleSummary
import com.secondbrain.android.data.remote.VocabularyCandidate
import com.secondbrain.android.data.remote.VocabularyGenerateRequest
import com.secondbrain.android.data.remote.VocabularyWordMeaningRequest
import com.secondbrain.android.data.remote.requireData
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import retrofit2.HttpException
import javax.inject.Inject

data class VocabularyArticleState(
    val images: List<String> = emptyList(),
    val candidates: List<VocabularyCandidate> = emptyList(),
    val extracted: Boolean = false,
    val confirmed: Boolean = false,
    val article: VocabularyArticle? = null,
    val translation: String? = null,
    val translationBusy: Boolean = false,
    val translationError: String? = null,
    val wordLookup: VocabularyWordLookup? = null,
    val history: List<VocabularyArticleSummary> = emptyList(),
    val topic: String = "日常与学习",
    val difficulty: String = "中级",
    val busy: Boolean = false,
    val visionReady: Boolean? = null,
    val visionChecking: Boolean = false,
    val visionStatusMessage: String? = null,
    val message: String? = null
)

data class VocabularyWordLookup(val selection: ReadingWordSelection, val meaning: String? = null,
                                val busy: Boolean = false, val error: String? = null)

/** Retains image selections and word edits so a failed model request does not erase the user's work. */
@HiltViewModel
class VocabularyArticleViewModel @Inject constructor(
    private val api: SecondBrainApi,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val preferences = context.getSharedPreferences("vocabulary_draft", Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(restoreDraft())
    private val translations = mutableMapOf<Pair<Long, String>, String>()
    private var translationJob: Job? = null
    private var wordJob: Job? = null
    private data class WordKey(val articleId: Long, val sourceHash: String, val start: Int, val end: Int)
    private val wordMeanings = linkedMapOf<WordKey, String>()
    val state: StateFlow<VocabularyArticleState> = _state

    init { refreshHistory(); refreshVisionStatus() }

    fun refreshVisionStatus() {
        viewModelScope.launch {
            _state.value = _state.value.copy(visionChecking = true, visionStatusMessage = null)
            runCatching { api.vocabularyVisionReady().requireData() }
                .onSuccess { result ->
                    _state.value = _state.value.copy(visionReady = result.ready, visionChecking = false,
                        visionStatusMessage = result.message, message = null)
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(visionReady = null, visionChecking = false,
                        visionStatusMessage = visionCheckError(error))
                }
        }
    }

    fun addImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val existing = _state.value.images
            if (existing.size + uris.size > 8) {
                _state.value = _state.value.copy(message = "一次最多选择 8 张截图")
                return@launch
            }
            _state.value = _state.value.copy(busy = true, message = null)
            runCatching {
                withContext(Dispatchers.IO) {
                    val paths = uris.map(::compressToPrivateJpeg)
                    if ((existing + paths).sumOf { File(it).length() } > 24L * 1024 * 1024) {
                        paths.forEach { File(it).delete() }
                        error("截图总大小不能超过 24 MB，请先裁剪")
                    }
                    paths
                }
            }
                .onSuccess { paths ->
                    _state.value = _state.value.copy(images = existing + paths, candidates = emptyList(), extracted = false,
                        confirmed = false, busy = false)
                    persistDraft()
                }.onFailure {
                    _state.value = _state.value.copy(busy = false, message = it.message ?: "截图读取失败")
                }
        }
    }

    fun removeImage(index: Int) {
        val current = _state.value
        val path = current.images.getOrNull(index) ?: return
        File(path).delete()
        _state.value = current.copy(images = current.images.filterIndexed { i, _ -> i != index },
            candidates = emptyList(), extracted = false, confirmed = false, message = null)
        persistDraft()
    }

    fun extract() {
        val images = _state.value.images
        if (images.isEmpty()) {
            _state.value = _state.value.copy(message = "请先选择单词截图")
            return
        }
        if (_state.value.visionReady != true) {
            _state.value = _state.value.copy(message = _state.value.visionStatusMessage
                ?: "请先检查视觉模型配置，再提取截图中的单词。")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            runCatching {
                val parts = images.mapIndexed { index, path ->
                    val file = File(path)
                    check(file.isFile) { "本机截图已丢失，请重新选择" }
                    MultipartBody.Part.createFormData("files", "word-list-$index.jpg",
                        file.asRequestBody("image/jpeg".toMediaType()))
                }
                api.extractVocabulary(parts).requireData()
            }.onSuccess { extraction ->
                _state.value = _state.value.copy(candidates = extraction.words, extracted = true, confirmed = false,
                    busy = false, message = if (extraction.words.isEmpty()) "没有提取到清晰单词，请检查图片后重试" else "请逐词核对；不清楚的词已经标记。")
                persistDraft()
            }.onFailure {
                _state.value = _state.value.copy(busy = false,
                    message = userFacingLoadError(it, "视觉识别失败。请先在“我的”配置视觉模型和个人 API Key。"))
            }
        }
    }

    fun editWord(index: Int, value: String) {
        val current = _state.value
        val item = current.candidates.getOrNull(index) ?: return
        _state.value = current.copy(candidates = current.candidates.toMutableList().apply {
            this[index] = item.copy(word = value)
        }, confirmed = false)
        persistDraft()
    }

    fun addWord() {
        _state.value = _state.value.copy(candidates = _state.value.candidates +
            VocabularyCandidate("", -1, true), confirmed = false)
        persistDraft()
    }

    fun removeWord(index: Int) {
        _state.value = _state.value.copy(candidates = _state.value.candidates.filterIndexed { i, _ -> i != index },
            confirmed = false)
        persistDraft()
    }

    fun confirm() {
        val current = _state.value
        val words = current.candidates.map { it.word.trim() }
        if (words.isEmpty() || words.any { !it.matches(Regex("[A-Za-z][A-Za-z'-]{0,49}")) }) {
            _state.value = current.copy(message = "请删除空词并改正无法确认的英文拼写")
            return
        }
        val distinct = words.distinctBy(String::lowercase)
        _state.value = current.copy(candidates = distinct.map { word ->
            current.candidates.first { it.word.equals(word, ignoreCase = true) }.copy(word = word, uncertain = false)
        }, confirmed = true, message = "已确认 ${distinct.size} 个词，可以生成文章。")
        persistDraft()
    }

    fun setTopic(value: String) { _state.value = _state.value.copy(topic = value); persistDraft() }
    fun setDifficulty(value: String) { _state.value = _state.value.copy(difficulty = value); persistDraft() }

    fun generate() {
        val current = _state.value
        if (current.busy) return
        if (!current.confirmed) {
            _state.value = current.copy(message = "请先确认词表")
            return
        }
        _state.value = current.copy(busy = true, message = null)
        viewModelScope.launch {
            runCatching {
                api.generateVocabularyArticle(VocabularyGenerateRequest(current.candidates.map { it.word },
                    current.topic, current.difficulty)).requireData()
            }.onSuccess { article ->
                _state.value = _state.value.copy(busy = false, article = article,
                    history = listOf(VocabularyArticleSummary(article.id, article.topic, article.difficulty,
                        article.words.size, article.words.size - article.missingWords.size, article.createTime)) +
                        _state.value.history.filter { it.id != article.id },
                    message = if (article.missingWords.isEmpty()) "全部目标词已在正文出现" else "仍有 ${article.missingWords.size} 个词未覆盖，已在文首列出")
                clearDraftFiles()
            }.onFailure {
                if (it is CancellationException) throw it
                _state.value = _state.value.copy(busy = false,
                    message = if (it is IOException)
                        "连接中断，文章可能已保存。请先刷新下方历史文章，避免重复生成；确认的词表已保留。"
                    else userFacingLoadError(it, "文章生成失败；确认的词表已保留，请重试"))
                if (it is IOException) refreshHistory()
            }
        }
    }

    fun refreshHistory(reportFailure: Boolean = false) {
        viewModelScope.launch {
            runCatching { api.vocabularyArticles().requireData() }
                .onSuccess { _state.value = _state.value.copy(history = it) }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    if (reportFailure) _state.value = _state.value.copy(
                        message = userFacingLoadError(error, "历史文章刷新失败，请稍后再试"))
                }
        }
    }

    fun openArticle(id: Long) {
        if (_state.value.busy) return
        translationJob?.cancel()
        dismissWord()
        _state.value = _state.value.copy(busy = true, message = null, translation = null,
            translationBusy = false, translationError = null)
        viewModelScope.launch {
            runCatching { api.vocabularyArticle(id).requireData() }
                .onSuccess { _state.value = _state.value.copy(article = it, busy = false) }
                .onFailure { _state.value = _state.value.copy(busy = false,
                    message = userFacingLoadError(it, "文章读取失败")) }
        }
    }

    fun completeMissingWords() {
        val current = _state.value.article ?: return
        if (current.missingWords.isEmpty() || _state.value.busy || _state.value.translationBusy) return
        _state.value = _state.value.copy(busy = true, message = null)
        viewModelScope.launch {
            runCatching { api.completeVocabularyArticle(current.id).requireData() }
                .onSuccess { updated ->
                    val covered = updated.words.size - updated.missingWords.size
                    val stillReading = _state.value.article?.id == current.id
                    if (stillReading && updated.article != current.article) dismissWord()
                    _state.value = _state.value.copy(article = if (stillReading) updated else _state.value.article,
                        busy = false,
                        translation = if (stillReading && updated.article != current.article) null else _state.value.translation,
                        translationError = null,
                        history = _state.value.history.map { summary ->
                            if (summary.id == updated.id) summary.copy(coveredCount = covered) else summary
                        },
                        message = if (!stillReading) null else if (updated.missingWords.isEmpty())
                            "目标词已全部覆盖" else "本次补齐后仍有 ${updated.missingWords.size} 个词未覆盖，可稍后再试")
                }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    _state.value = _state.value.copy(busy = false,
                        message = if (error is IOException)
                            "连接中断，补充阅读可能已保存。请返回词表页刷新历史文章后重新打开，原文章仍可阅读。"
                        else userFacingLoadError(error, "补充阅读生成失败，原文章仍可继续阅读"))
                    if (error is IOException) refreshHistory()
                }
        }
    }

    fun loadTranslation() {
        val current = _state.value
        val article = current.article ?: return
        if (current.busy || current.translationBusy) return
        val key = article.id to article.article
        translations[key]?.let {
            _state.value = current.copy(translation = it, translationError = null)
            return
        }
        _state.value = current.copy(translationBusy = true, translationError = null)
        translationJob = viewModelScope.launch {
            runCatching {
                val result = api.translateVocabularyArticle(article.id).requireData()
                val sourceHash = readingSourceHash(article.article)
                check(result.articleId == article.id && result.sourceHash == sourceHash) {
                    "文章内容已更新，请返回词表重新打开后查看翻译"
                }
                check(result.translation.isNotBlank()) { "全文翻译为空，请重试" }
                result.translation
            }.onSuccess { translated ->
                translations[key] = translated
                if (_state.value.article?.let { it.id to it.article } == key) {
                    _state.value = _state.value.copy(translation = translated, translationBusy = false)
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                if (_state.value.article?.let { it.id to it.article } == key) {
                    _state.value = _state.value.copy(translationBusy = false,
                        translationError = if (error is HttpException && error.code() == 404)
                            "后端尚未提供全文翻译，请更新并重启后端后重试。"
                        else userFacingLoadError(error, "全文翻译失败，请重试；英文原文仍可阅读"))
                }
            }
        }
    }

    fun closeArticle() {
        translationJob?.cancel()
        dismissWord()
        _state.value = _state.value.copy(article = null, message = null, translation = null,
            translationBusy = false, translationError = null)
    }

    fun selectWord(start: Int, end: Int) {
        val current = _state.value
        val article = current.article ?: return
        if (current.busy) return
        val selection = readingWordAt(article.article, start, end) ?: return
        if (current.wordLookup?.let { it.selection == selection && it.busy } == true) return
        wordJob?.cancel()
        val key = WordKey(article.id, readingSourceHash(article.article), start, end)
        val existing = article.meanings.entries.firstOrNull { it.key.equals(selection.word, ignoreCase = true) }
            ?.value?.takeIf(String::isNotBlank) ?: wordMeanings[key]
        if (existing != null) {
            _state.value = current.copy(wordLookup = VocabularyWordLookup(selection, meaning = existing))
            return
        }
        _state.value = current.copy(wordLookup = VocabularyWordLookup(selection, busy = true))
        wordJob = viewModelScope.launch {
            runCatching {
                val result = api.vocabularyWordMeaning(article.id,
                    VocabularyWordMeaningRequest(key.sourceHash, start, end)).requireData()
                check(result.articleId == article.id && result.sourceHash == key.sourceHash &&
                    result.start == start && result.end == end && result.word == selection.word) {
                    "文章内容已更新，请返回词表重新打开后点词"
                }
                check(result.meaning.isNotBlank()) { "释义为空，请重试" }
                result
            }.onSuccess { result ->
                wordMeanings[key] = result.meaning
                if (wordMeanings.size > 128) wordMeanings.remove(wordMeanings.keys.first())
                // 切换词或关闭面板后，较早的网络结果不能覆盖当前选择。
                if (isCurrentWord(article, selection)) {
                    _state.value = _state.value.copy(wordLookup = VocabularyWordLookup(
                        selection.copy(sentence = result.sentence), meaning = result.meaning))
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                if (isCurrentWord(article, selection)) {
                    _state.value = _state.value.copy(wordLookup = VocabularyWordLookup(selection,
                        error = if (error is HttpException && error.code() == 404)
                            "后端尚未提供任意词释义，请更新并重启后端后重试。"
                        else userFacingLoadError(error, "释义查询失败，请重试；原文仍可阅读")))
                }
            }
        }
    }

    fun dismissWord() {
        wordJob?.cancel()
        _state.value = _state.value.copy(wordLookup = null)
    }

    private fun isCurrentWord(article: VocabularyArticle, selection: ReadingWordSelection): Boolean =
        _state.value.article?.let { it.id == article.id && it.article == article.article } == true &&
            _state.value.wordLookup?.selection?.let { it.start == selection.start && it.end == selection.end } == true

    private fun compressToPrivateJpeg(uri: Uri): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法读取这张截图" }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2200) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("无法解码这张截图")
        val folder = File(context.filesDir, "vocabulary-draft").apply { mkdirs() }
        val file = File(folder, "${System.nanoTime()}.jpg")
        try {
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 84, it)) }
            require(file.length() <= 4L * 1024 * 1024) { "截图仍超过 4 MB，请先裁剪后再选择" }
            return file.absolutePath
        } catch (error: Exception) {
            file.delete()
            throw error
        } finally {
            bitmap.recycle()
        }
    }

    private fun persistDraft() {
        val current = _state.value
        val json = JSONObject().put("images", JSONArray(current.images))
            .put("words", JSONArray().apply { current.candidates.forEach { item ->
                put(JSONObject().put("word", item.word).put("source", item.sourceImageIndex)
                    .put("uncertain", item.uncertain))
            } })
            .put("confirmed", current.confirmed).put("extracted", current.extracted).put("topic", current.topic)
            .put("difficulty", current.difficulty)
        preferences.edit().putString("draft", json.toString()).apply()
    }

    private fun restoreDraft(): VocabularyArticleState = runCatching {
        val json = JSONObject(preferences.getString("draft", "{}") ?: "{}")
        val images = json.optJSONArray("images") ?: JSONArray()
        val words = json.optJSONArray("words") ?: JSONArray()
        VocabularyArticleState(
            images = (0 until images.length()).map { images.getString(it) }.filter { File(it).isFile },
            candidates = (0 until words.length()).map { index ->
                words.getJSONObject(index).let { VocabularyCandidate(it.optString("word"),
                    it.optInt("source", -1), it.optBoolean("uncertain")) }
            },
            confirmed = json.optBoolean("confirmed"), extracted = json.optBoolean("extracted"),
            topic = json.optString("topic", "日常与学习"),
            difficulty = json.optString("difficulty", "中级")
        )
    }.getOrDefault(VocabularyArticleState())

    private fun clearDraftFiles() {
        _state.value.images.forEach { File(it).delete() }
        preferences.edit().remove("draft").apply()
        _state.value = _state.value.copy(images = emptyList(), candidates = emptyList(), extracted = false, confirmed = false)
    }

    private fun visionCheckError(error: Throwable): String {
        if (error is HttpException && error.code() == 404) {
            return "后端尚未提供词表文章接口，请更新并重启后端服务。"
        }
        if (error is com.squareup.moshi.JsonDataException) {
            return "后端与当前应用版本不一致，请更新并重启后端服务。"
        }
        return "配置检查失败：${userFacingLoadError(error, "请稍后重试")}" 
    }
}
