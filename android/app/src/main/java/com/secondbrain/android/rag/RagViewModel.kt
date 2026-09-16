package com.secondbrain.android.rag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.android.data.remote.RagRequest
import com.secondbrain.android.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RagUiState(
    val question: String = "",
    val answer: String = "",
    val references: String? = null,
    val asking: Boolean = false,
    val ready: Boolean = false,
    val error: String? = null
)

/** 会话变化时取消旧空间的回答，避免旧结果出现在新空间。 */
@HiltViewModel
class RagViewModel @Inject constructor(private val streamClient: RagAnswerSource, sessionStore: SessionStore) : ViewModel() {
    private val _state = MutableStateFlow(RagUiState())
    val state: StateFlow<RagUiState> = _state
    private var answerJob: Job? = null

    init {
        viewModelScope.launch {
            sessionStore.tokenFlow.distinctUntilChanged().collect { token ->
                answerJob?.cancel()
                _state.value = RagUiState(ready = !token.isNullOrBlank())
            }
        }
    }

    fun updateQuestion(question: String) {
        if (!_state.value.asking) _state.value = _state.value.copy(question = question, error = null)
    }

    fun stop() {
        answerJob?.cancel()
        _state.value = _state.value.copy(asking = false)
    }

    fun ask() {
        if (_state.value.asking || !_state.value.ready) return
        val question = _state.value.question.trim()
        if (question.isBlank()) {
            _state.value = _state.value.copy(error = "请输入问题")
            return
        }
        _state.value = _state.value.copy(answer = "", references = null, asking = true, error = null)
        answerJob = viewModelScope.launch {
            try {
                var completed = false
                streamClient.answer(RagRequest(question)).collect { event ->
                    when (event.name) {
                        "token" -> _state.value = _state.value.copy(answer = _state.value.answer + event.data)
                        "references" -> _state.value = _state.value.copy(references = event.data)
                        "error" -> _state.value = _state.value.copy(error = event.data.ifBlank { "问答服务暂时不可用，请稍后重试" })
                        "done" -> completed = true
                    }
                }
                if (!completed && _state.value.error == null) _state.value = _state.value.copy(error = "连接提前结束，已保留收到的内容，可重新提问")
                _state.value = _state.value.copy(asking = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                _state.value = _state.value.copy(asking = false, error = error.message ?: "问答失败，请检查网络后重试")
            }
        }
    }
}
