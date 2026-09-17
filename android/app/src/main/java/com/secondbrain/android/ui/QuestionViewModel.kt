package com.secondbrain.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.android.data.remote.CommunityQuestion
import com.secondbrain.android.data.remote.CreateCommunityAnswerRequest
import com.secondbrain.android.data.remote.SecondBrainApi
import com.secondbrain.android.data.remote.requireData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuestionState(
    val question: CommunityQuestion? = null,
    val draft: String = "",
    val loading: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val published: Boolean = false
)

/** 提交状态独立于页面生命周期，返回或旋转时不会取消已发出的回答请求。 */
@HiltViewModel
class QuestionViewModel @Inject constructor(private val api: SecondBrainApi) : ViewModel() {
    private val _state = MutableStateFlow(QuestionState())
    val state: StateFlow<QuestionState> = _state
    private var loadJob: Job? = null

    fun load(id: Long) {
        if (_state.value.submitting) return
        loadJob?.cancel()
        _state.value = if (_state.value.question?.id == id) _state.value.copy(loading = true, error = null)
            else QuestionState(loading = true)
        loadJob = viewModelScope.launch {
            try { _state.value = _state.value.copy(question = api.questionDetail(id).requireData(), loading = false) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { _state.value = _state.value.copy(loading = false, error = failure.message ?: "问题加载失败，请重试") }
        }
    }

    fun edit(content: String) {
        if (!_state.value.submitting) _state.value = _state.value.copy(draft = content.take(10000), published = false)
    }

    fun submit() {
        val current = _state.value
        val question = current.question ?: return
        val content = current.draft.trim()
        if (current.submitting || current.loading || content.length !in 10..10000) return
        _state.value = current.copy(submitting = true, error = null, published = false)
        viewModelScope.launch {
            try {
                val answer = api.answerQuestion(question.id, CreateCommunityAnswerRequest(content)).requireData()
                // 使用服务端返回的回答确认发布，避免把随后刷新失败误报为发布失败并导致重复提交。
                val answers = (question.answers.orEmpty() + answer).distinctBy { it.id }
                _state.value = _state.value.copy(question = question.copy(answers = answers,
                    answerCount = maxOf(question.answerCount + 1, answers.size)),
                    submitting = false, draft = "", published = true)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { _state.value = _state.value.copy(submitting = false, error = failure.message ?: "发布失败，回答已保留，请重试") }
        }
    }
}
