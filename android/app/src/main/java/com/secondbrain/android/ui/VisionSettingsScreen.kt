package com.secondbrain.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.android.data.remote.AiProviderOption
import com.secondbrain.android.data.remote.AiScenarioConfig
import com.secondbrain.android.data.remote.SecondBrainApi
import com.secondbrain.android.data.remote.requireData
import com.squareup.moshi.JsonDataException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class VisionSettingsState(
    val providers: List<AiProviderOption> = emptyList(),
    val provider: AiProviderOption? = null,
    val model: String = "qwen3-vl-flash",
    val keyInput: String = "",
    val hasKey: Boolean = false,
    val ready: Boolean? = null,
    val busy: Boolean = false,
    val message: String? = null
)

/** Keeps the provider key on the server; the Android UI only holds newly typed text until save. */
@HiltViewModel
class VisionSettingsViewModel @Inject constructor(private val api: SecondBrainApi) : ViewModel() {
    private val _state = MutableStateFlow(VisionSettingsState())
    val state: StateFlow<VisionSettingsState> = _state

    init { reload() }

    fun reload() {
        viewModelScope.launch {
            _state.value = _state.value.copy(busy = true, message = null)
            runCatching {
                val providers = api.aiProviders().requireData()
                val configured = api.aiConfigurations().requireData().find { it.scenarioCode == "vision" }
                providers to configured
            }.onSuccess { (providers, configured) ->
                val compatible = providers.filter { it.apiType == "openai_compatible" }
                val provider = compatible.find { it.id == configured?.providerId }
                    ?: compatible.find { it.code == "qwen" } ?: compatible.firstOrNull()
                _state.value = _state.value.copy(providers = compatible, provider = provider,
                    model = configured?.modelName ?: "qwen3-vl-flash",
                    hasKey = !configured?.apiKey.isNullOrBlank(), busy = false)
                if (configured != null) checkReadiness()
            }.onFailure { _state.value = _state.value.copy(busy = false, message = it.message ?: "读取 AI 配置失败") }
        }
    }

    fun setModel(value: String) { _state.value = _state.value.copy(model = value, ready = null, message = null) }
    fun setKey(value: String) { _state.value = _state.value.copy(keyInput = value, ready = null, message = null) }
    fun selectProvider(provider: AiProviderOption) {
        _state.value = _state.value.copy(provider = provider, hasKey = false, ready = null, keyInput = "",
            model = if (provider.code == "qwen") "qwen3-vl-flash" else "", message = "更换服务商后请填写该服务商的 API Key")
    }

    fun save() {
        val current = _state.value
        val provider = current.provider ?: return
        if (current.model.isBlank() || (!current.hasKey && current.keyInput.isBlank())) {
            _state.value = current.copy(message = "请填写视觉模型名称和个人 API Key")
            return
        }
        viewModelScope.launch {
            _state.value = current.copy(busy = true, message = null)
            runCatching {
                api.saveAiConfigurations(listOf(AiScenarioConfig("vision", provider.id,
                    current.model.trim(), current.keyInput.ifBlank { null }))).requireDataOrSuccess()
            }.onSuccess {
                _state.value = _state.value.copy(keyInput = "", ready = null)
                runCatching {
                    val saved = api.aiConfigurations().requireData().find { it.scenarioCode == "vision" }
                    val readiness = api.vocabularyVisionReady().requireData()
                    saved to readiness
                }.onSuccess { (saved, readiness) ->
                    _state.value = _state.value.copy(busy = false,
                        hasKey = !saved?.apiKey.isNullOrBlank(), ready = readiness.ready,
                        message = if (readiness.ready) "配置已保存并通过检查。图片只在你主动识别时发送。"
                            else "配置已保存，但尚不能识别：${readiness.message}")
                }.onFailure { error ->
                    _state.value = _state.value.copy(busy = false, ready = null,
                        message = "配置已保存，但无法检查：${visionSettingsCheckError(error)}")
                }
            }.onFailure { _state.value = _state.value.copy(busy = false, message = it.message ?: "配置保存失败") }
        }
    }

    private fun checkReadiness() {
        viewModelScope.launch {
            runCatching { api.vocabularyVisionReady().requireData() }
                .onSuccess { result -> _state.value = _state.value.copy(ready = result.ready,
                    message = if (result.ready) null else result.message) }
                .onFailure { error -> _state.value = _state.value.copy(ready = null,
                    message = "配置状态暂不可验证：${visionSettingsCheckError(error)}") }
        }
    }
}

private fun visionSettingsCheckError(error: Throwable): String = when {
    error is HttpException && error.code() == 404 -> "后端接口未更新，请更新并重启后端服务"
    error is JsonDataException -> "后端与应用版本不一致，请更新并重启后端服务"
    else -> userFacingLoadError(error, "请稍后重试")
}

private fun com.secondbrain.android.data.remote.ApiResult<*>.requireDataOrSuccess() {
    if (code != 200) error(message)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VisionSettingsScreen(onBack: () -> Unit, viewModel: VisionSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var providerMenu by remember { mutableStateOf(false) }
    Scaffold(containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("视觉识别设置", style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface) {
                Button(onClick = viewModel::save, enabled = !state.busy && state.provider != null,
                    modifier = Modifier.imePadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().heightIn(min = 52.dp),
                    shape = RoundedCornerShape(12.dp)) { Text(if (state.busy) "正在保存…" else "保存视觉模型配置") }
            }
        }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text("自己的模型，按需调用", style = MaterialTheme.typography.headlineSmall)
            Text("错题默认仍用本机 OCR。只有你主动选择精准识别，或提交单词截图时，图片才会由后端发送给这里配置的服务商。费用由你的服务商账户结算。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("视觉模型服务商", style = MaterialTheme.typography.labelLarge)
                ExposedDropdownMenuBox(expanded = providerMenu, onExpandedChange = { if (!state.busy) providerMenu = it }) {
                    OutlinedTextField(value = state.provider?.name ?: "选择服务商", onValueChange = {}, readOnly = true,
                        enabled = !state.busy, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(providerMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
                    ExposedDropdownMenu(expanded = providerMenu, onDismissRequest = { providerMenu = false }) {
                        state.providers.forEach { provider ->
                            DropdownMenuItem(text = { Text(provider.name) }, onClick = { viewModel.selectProvider(provider); providerMenu = false })
                        }
                    }
                }
            }
            OutlinedTextField(state.model, viewModel::setModel, enabled = !state.busy, label = { Text("视觉模型名称") },
                supportingText = { Text("需支持图片输入与 OpenAI 兼容协议") }, singleLine = true,
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(state.keyInput, viewModel::setKey, enabled = !state.busy, label = { Text("个人 API Key") },
                placeholder = { Text(if (state.hasKey) "已配置；留空保留原 Key" else "输入个人 API Key") },
                visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth())
            state.message?.let { Text(it, color = if (state.ready == false) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
        }
    }
}
