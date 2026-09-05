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

data class ReminderUiState(val reminders: List<ReviewReminder> = emptyList(), val message: String? = null)

/** Saves first to the service, then mirrors the accepted schedule locally to avoid phantom notifications. */
@HiltViewModel
class ReminderViewModel @Inject constructor(private val api: SecondBrainApi) : ViewModel() {
    private val _state = MutableStateFlow(ReminderUiState())
    val state: StateFlow<ReminderUiState> = _state
    fun load() = viewModelScope.launch {
        runCatching { api.reminders().requireData() }
            .onSuccess { _state.value = ReminderUiState(it) }
            .onFailure { _state.value = ReminderUiState(message = it.message ?: "提醒加载失败") }
    }
    fun cancel(context: Context, nodeId: Long) = viewModelScope.launch {
        runCatching { api.cancelReminder(nodeId).requireSuccess() }
            .onSuccess { ReminderScheduler.cancel(context, nodeId); load() }
            .onFailure { _state.value = _state.value.copy(message = it.message ?: "取消失败") }
    }
    fun save(context: Context, nodeId: Long, scheduledAt: String) = viewModelScope.launch {
        runCatching { api.saveReminder(nodeId, ReminderRequest(scheduledAt)).requireData() }
            .onSuccess {
                ReminderScheduler.schedule(context, nodeId, "知识点 #$nodeId 的复习时间到了", scheduledAt)
                load()
            }
            .onFailure { _state.value = _state.value.copy(message = it.message ?: "提醒保存失败") }
    }
}
