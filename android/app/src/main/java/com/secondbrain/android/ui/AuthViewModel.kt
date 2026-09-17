package com.secondbrain.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.android.data.remote.LoginRequest
import com.secondbrain.android.data.remote.SecondBrainApi
import com.secondbrain.android.data.remote.requireData
import com.secondbrain.android.data.session.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(val checking: Boolean = true, val signedIn: Boolean = false, val error: String? = null)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val api: SecondBrainApi,
    private val sessionStore: SessionStore
) : ViewModel() {
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state

    init {
        viewModelScope.launch {
            sessionStore.tokenFlow.collectLatest { token ->
                _state.value = AuthUiState(checking = false, signedIn = token != null)
            }
        }
    }

    fun login(username: String, password: String) = viewModelScope.launch {
        if (username.isBlank() || password.isBlank()) {
            _state.value = AuthUiState(checking = false, error = "请输入用户名和密码")
            return@launch
        }
        _state.value = AuthUiState(checking = true)
        runCatching { api.login(LoginRequest(username, password)).requireData() }
            .onSuccess {
                sessionStore.save(it.token)
                _state.value = AuthUiState(checking = false, signedIn = true)
            }
            .onFailure { _state.value = AuthUiState(checking = false, error = it.message ?: "登录失败，请稍后重试") }
    }
}
