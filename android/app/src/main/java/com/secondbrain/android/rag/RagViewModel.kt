package com.secondbrain.android.rag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.android.data.remote.RagRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RagUiState(
    val question: String = "",
    val answer: String = "",
    val references: String? = null,
    val asking: Boolean = false,
    val error: String? = null
)

/** Streams only answer text and references into UI state; unrecognized server events are intentionally ignored. */
@HiltViewModel
class RagViewModel @Inject constructor(private val streamClient: RagStreamClient) : ViewModel() {
    private val _state = MutableStateFlow(RagUiState())
    val state: StateFlow<RagUiState> = _state

    fun updateQuestion(question: String) {
        _state.value = _state.value.copy(question = question)
    }

    fun ask() = viewModelScope.launch {
        val question = _state.value.question.trim()
        if (question.isBlank()) {
            _state.value = _state.value.copy(error = "请输入问题")
            return@launch
        }
        _state.value = _state.value.copy(answer = "", references = null, asking = true, error = null)
        runCatching {
            streamClient.answer(RagRequest(question)).collect { event ->
                when (event.name) {
                    "token" -> _state.value = _state.value.copy(answer = _state.value.answer + event.data)
                    "references" -> _state.value = _state.value.copy(references = event.data)
                    "error" -> _state.value = _state.value.copy(
                        error = event.data.ifBlank { "问答服务暂时不可用，请稍后重试。" }
                    )
                }
            }
        }.onFailure {
            _state.value = _state.value.copy(
                error = it.message ?: "问答服务暂时不可用，请检查网络后重试。"
            )
        }
        _state.value = _state.value.copy(asking = false)
    }
}
