package com.secondbrain.android.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.android.data.remote.ReviewCard
import com.secondbrain.android.data.remote.ReviewResult
import com.secondbrain.android.data.remote.SecondBrainApi
import com.secondbrain.android.data.remote.SubmitReviewRequest
import com.secondbrain.android.data.remote.requireData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val cards: List<ReviewCard> = emptyList(),
    val position: Int = 0,
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val answer: String = "",
    val result: ReviewResult? = null,
    val message: String? = null
)

/** 答题、查看解析和下一题分开推进，服务确认前保留答案，避免重试时丢失输入。 */
@HiltViewModel
class ReviewViewModel @Inject constructor(private val api: SecondBrainApi) : ViewModel() {
    private val _state = MutableStateFlow(ReviewUiState())
    val state: StateFlow<ReviewUiState> = _state
    private var startedAt = System.nanoTime()
    private var loadJob: Job? = null

    private var initialized = false

    fun ensureLoaded() { if (!initialized) load() }

    fun load() = loadFrom(null)

    fun loadFrom(cardId: Long?) {
        if (_state.value.submitting) return
        initialized = true
        loadJob?.cancel()
        _state.value = ReviewUiState()
        loadJob = viewModelScope.launch {
            val response = runCatching { api.todayReview().requireData() }
            ensureActive()
            response.onSuccess {
                val ordered = if (cardId == null) it else it.sortedBy { card -> if (card.id == cardId) 0 else 1 }
                _state.value = ReviewUiState(cards = ordered, loading = false)
                startedAt = System.nanoTime()
            }.onFailure { _state.value = ReviewUiState(loading = false, message = it.message ?: "无法加载复习卡片") }
        }
    }

    fun updateAnswer(answer: String) {
        if (!_state.value.submitting && _state.value.result == null) {
            _state.value = _state.value.copy(answer = answer, message = null)
        }
    }

    fun submit() {
        val state = _state.value
        val card = state.cards.getOrNull(state.position) ?: return
        if (state.submitting || state.result != null || state.answer.isBlank()) return
        _state.value = state.copy(submitting = true, message = null)
        val seconds = ((System.nanoTime() - startedAt) / 1_000_000_000).coerceAtLeast(1).toInt()
        viewModelScope.launch {
            runCatching { api.submitReview(SubmitReviewRequest(card.id, state.answer.trim(), seconds)).requireData() }
                .onSuccess { _state.value = _state.value.copy(submitting = false, result = it) }
                .onFailure { _state.value = _state.value.copy(submitting = false, message = it.message ?: "提交失败，请重试") }
        }
    }

    fun skipInvalid() {
        val card = _state.value.cards.getOrNull(_state.value.position) ?: return
        if (parseReviewPrompt(card.question).question.isNotBlank() || _state.value.submitting) return
        _state.value = _state.value.copy(cards = _state.value.cards.filter { it.id != card.id }, answer = "", message = null)
        startedAt = System.nanoTime()
    }

    fun next() {
        if (_state.value.result == null) return
        _state.value = _state.value.copy(position = _state.value.position + 1, answer = "", result = null, message = null)
        startedAt = System.nanoTime()
    }
}
