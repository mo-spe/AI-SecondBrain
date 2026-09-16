package com.secondbrain.android.capture

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.android.data.remote.CreateKnowledgeRequest
import com.secondbrain.android.data.repository.KnowledgeRepository
import com.secondbrain.android.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

data class CaptureUiState(
    val draft: OcrDraft? = null,
    val recognizing: Boolean = false,
    val saving: Boolean = false,
    val message: String? = null,
    val localSaved: Boolean = true,
    val ready: Boolean = false,
    val workspaceId: Long? = null
)

/** 编辑先写入本机草稿，用户确认后才创建知识；写入与删除串行，防止已删除草稿复活。 */
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val drafts: OcrDraftDao,
    private val repository: KnowledgeRepository,
    private val sessionStore: SessionStore
) : ViewModel() {
    private val recognizer = OcrRecognizer()
    private val draftLock = Mutex()
    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state
    val savedDrafts = drafts.observeAll()

    init {
        viewModelScope.launch { sessionStore.workspaceFlow.collect { _state.value = _state.value.copy(workspaceId = it, ready = true) } }
    }

    fun recognize(context: Context, uri: Uri) {
        if (_state.value.recognizing || _state.value.saving || !_state.value.ready) return
        val workspaceId = _state.value.workspaceId
        _state.value = _state.value.copy(recognizing = true, message = null)
        viewModelScope.launch {
            try {
                val text = recognizer.recognize(context, uri)
                val title = text.lineSequence().firstOrNull { it.isNotBlank() }?.take(80) ?: "未命名采集"
                val draft = OcrDraft(title = title, content = text, imageUri = uri.toString(), workspaceId = workspaceId)
                val id = draftLock.withLock { drafts.save(draft) }
                _state.value = _state.value.copy(draft = draft.copy(id = id), recognizing = false, localSaved = true)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _state.value = _state.value.copy(recognizing = false, message = error.message ?: "文字识别失败，可手动录入") }
        }
    }

    fun createManual() {
        if (_state.value.recognizing || _state.value.saving || _state.value.draft != null || !_state.value.ready) return
        _state.value = _state.value.copy(recognizing = true, message = null)
        val draft = OcrDraft(title = "", content = "", imageUri = "", workspaceId = _state.value.workspaceId)
        viewModelScope.launch {
            try {
                val id = draftLock.withLock { drafts.save(draft) }
                _state.value = _state.value.copy(draft = draft.copy(id = id), recognizing = false, localSaved = true)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _state.value = _state.value.copy(recognizing = false, message = "无法创建本机草稿，请检查可用空间") }
        }
    }

    fun update(title: String, content: String) {
        if (_state.value.saving) return
        val updated = _state.value.draft?.copy(title = title, content = content) ?: return
        _state.value = _state.value.copy(draft = updated, localSaved = false, message = null)
        viewModelScope.launch {
            try {
                draftLock.withLock { drafts.save(updated) }
                if (_state.value.draft == updated) _state.value = _state.value.copy(localSaved = true)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _state.value = _state.value.copy(message = "草稿尚未保存到本机，请重试", localSaved = false) }
        }
    }

    fun restore(draft: OcrDraft) {
        if (!_state.value.saving && !_state.value.recognizing) _state.value = _state.value.copy(draft = draft, message = null, localSaved = true)
    }

    fun discard(draftId: Long) {
        if (_state.value.saving) return
        val editing = _state.value.draft?.takeIf { it.id == draftId }
        if (editing != null) _state.value = _state.value.copy(draft = null, message = null)
        viewModelScope.launch {
            try {
                draftLock.withLock { drafts.delete(draftId) }
                if (_state.value.draft?.id == draftId) _state.value = _state.value.copy(draft = null, message = null)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _state.value = _state.value.copy(draft = _state.value.draft ?: editing, message = "草稿删除失败，请重试") }
        }
    }

    fun saveConfirmed() {
        val draft = _state.value.draft ?: return
        if (_state.value.saving || !_state.value.ready || draft.title.isBlank() || draft.content.isBlank()) return
        if (draft.workspaceId != _state.value.workspaceId) {
            _state.value = _state.value.copy(message = "请先切回草稿所属空间，再确认保存")
            return
        }
        _state.value = _state.value.copy(saving = true, message = null)
        viewModelScope.launch {
            try {
                draftLock.withLock { drafts.save(draft) }
                check(sessionStore.workspaceId() == draft.workspaceId) { "空间已变化，请切回草稿所属空间后保存" }
                repository.create(CreateKnowledgeRequest(draft.title.trim(), draft.content.take(240), draft.content)).getOrThrow()
                // 服务端已确认后，不把本机清理失败误报为发布失败，防止重试产生重复知识。
                try { draftLock.withLock { drafts.delete(draft.id) } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) {
                    _state.value = _state.value.copy(draft = null, saving = false, message = "知识已保存，本机旧草稿未清理，请勿重复发布")
                    return@launch
                }
                _state.value = _state.value.copy(draft = null, saving = false, localSaved = true, message = "已保存到知识库")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _state.value = _state.value.copy(saving = false, message = error.message ?: "保存失败，草稿已保留") }
        }
    }
}
