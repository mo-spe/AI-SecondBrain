package com.secondbrain.android.reminder

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.android.data.remote.ReminderRequest
import com.secondbrain.android.data.remote.ReviewReminder
import com.secondbrain.android.data.remote.SecondBrainApi
import com.secondbrain.android.data.remote.requireData
import com.secondbrain.android.data.remote.requireSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReminderUiState(val reminders: List<ReviewReminder> = emptyList(), val message: String? = null, val saving: Boolean = false, val savedNodeId: Long? = null)

/** Saves first to the service, then mirrors the accepted schedule locally to avoid phantom notifications. */
@HiltViewModel
class ReminderViewModel @Inject constructor(private val api: SecondBrainApi) : ViewModel() {
    private val _state = MutableStateFlow(ReminderUiState())
    val state: StateFlow<ReminderUiState> = _state
    fun load() = viewModelScope.launch {
        runCatching { api.reminders().requireData() }
            .onSuccess { _state.value = _state.value.copy(reminders = it) }
            .onFailure { _state.value = _state.value.copy(message = it.message ?: "提醒加载失败") }
    }
    fun cancel(context: Context, nodeId: Long) = viewModelScope.launch {
        runCatching { api.cancelReminder(nodeId).requireSuccess() }
            .onSuccess { ReminderScheduler.cancel(context, nodeId); load() }
            .onFailure { _state.value = _state.value.copy(message = it.message ?: "取消失败") }
    }
    fun prepareEditor() { if (!_state.value.saving) _state.value = _state.value.copy(message = null, savedNodeId = null) }

    fun save(context: Context, nodeId: Long, scheduledAt: String) {
        if (_state.value.saving) return
        val validation = validateReminderTime(nodeId, scheduledAt)
        if (validation != null) { _state.value = _state.value.copy(message = validation); return }
        _state.value = _state.value.copy(saving = true, message = null, savedNodeId = null)
        viewModelScope.launch {
            try {
                val accepted = api.saveReminder(nodeId, ReminderRequest(scheduledAt)).requireData()
                val localWarning = try {
                    ReminderScheduler.schedule(context, nodeId, "知识点 #$nodeId 的复习时间到了", accepted.scheduledAt)
                    null
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) { "提醒已保存，本机通知暂未安排，请重新保存提醒以重试" }
                _state.value = _state.value.copy(saving = false, savedNodeId = nodeId, message = localWarning ?: "提醒已保存")
                load()
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { _state.value = _state.value.copy(saving = false, message = error.message ?: "提醒保存失败，请重试") }
        }
    }
}

internal fun validateReminderTime(nodeId: Long, scheduledAt: String, now: java.time.LocalDateTime = java.time.LocalDateTime.now()): String? {
    if (nodeId <= 0) return "请选择有效知识点"
    val time = try { java.time.LocalDateTime.parse(scheduledAt) } catch (error: java.time.format.DateTimeParseException) { return "请选择有效日期和时间" }
    return if (time.isAfter(now)) null else "提醒时间需要晚于现在"
}
