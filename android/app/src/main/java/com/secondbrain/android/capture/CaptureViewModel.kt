package com.secondbrain.android.capture

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.android.data.remote.CreateKnowledgeRequest
import com.secondbrain.android.data.repository.KnowledgeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CaptureUiState(
    val draft: OcrDraft? = null,
    val recognizing: Boolean = false,
    val saving: Boolean = false,
    val message: String? = null
)

/** Coordinates local OCR and explicit confirmation so recognized content cannot be auto-published. */
@HiltViewModel
class CaptureViewModel @Inject constructor(
    private val drafts: SecondBrainDatabase,
    private val repository: KnowledgeRepository
) : ViewModel() {
    private val recognizer = OcrRecognizer()
    private val _state = MutableStateFlow(CaptureUiState())
    val state: StateFlow<CaptureUiState> = _state
    val savedDrafts = drafts.ocrDraftDao().observeAll()

    fun recognize(context: Context, uri: Uri) = viewModelScope.launch {
        _state.value = CaptureUiState(recognizing = true)
        runCatching { recognizer.recognize(context, uri) }
            .onSuccess { text ->
                val lines = text.lineSequence().filter(String::isNotBlank).toList()
                val draft = OcrDraft(title = lines.firstOrNull()?.take(80) ?: "未命名采集", content = text, imageUri = uri.toString())
                val id = drafts.ocrDraftDao().save(draft)
                _state.value = CaptureUiState(draft = draft.copy(id = id))
            }
            .onFailure { _state.value = CaptureUiState(message = it.message ?: "文字识别失败，可直接手动录入。") }
    }

    fun update(title: String, content: String) {
        _state.value = _state.value.copy(draft = _state.value.draft?.copy(title = title, content = content))
    }

    fun restore(draft: OcrDraft) {
        _state.value = CaptureUiState(draft = draft)
    }

    fun discard(draftId: Long) = viewModelScope.launch {
        drafts.ocrDraftDao().delete(draftId)
        if (_state.value.draft?.id == draftId) {
            _state.value = CaptureUiState()
        }
    }

    fun saveConfirmed() {
        val draft = _state.value.draft ?: return
        if (_state.value.saving || draft.title.isBlank() || draft.content.isBlank()) return
        _state.value = _state.value.copy(saving = true, message = null)
        viewModelScope.launch {
            repository.create(CreateKnowledgeRequest(draft.title, draft.content.take(240), draft.content)).fold(
                onSuccess = {
                    drafts.ocrDraftDao().delete(draft.id)
                    _state.value = CaptureUiState(message = "已保存到知识库")
                },
                onFailure = { _state.value = _state.value.copy(saving = false, message = it.message ?: "保存失败，草稿已保留") }
            )
        }
    }
}
