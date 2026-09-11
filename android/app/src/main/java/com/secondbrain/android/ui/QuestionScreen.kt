package com.secondbrain.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun QuestionScreen(id: Long, onBack: () -> Unit, viewModel: QuestionViewModel, onPublished: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(id) { viewModel.load(id) }
    LaunchedEffect(state.published) {
        if (state.published) { keyboard?.hide(); onPublished() }
    }
    ReadingScaffold("问答 · 讨论详情", onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding().testTag("question-content"), state = listState,
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (state.loading) item { LoadingContent("正在读取问题…") }
            state.question?.let { question ->
                item {
                    Text(question.authorName ?: "社区成员", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(question.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
                    question.content?.takeIf { it.isNotBlank() }?.let { ReadingBody(it) }
                }
                item {
                    HorizontalDivider()
                    Text("${question.answerCount} 个回答", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp))
                }
                if (question.answers.isNullOrEmpty()) item { Text("分享你的理解，帮助讨论向前一步。", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(question.answers.orEmpty(), key = { it.id }) { answer ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(answer.authorName ?: "社区成员", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            if (answer.accepted) Text("已采纳", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        ReadingBody(answer.content)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
                item {
                    Text("你的回答", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(value = state.draft, onValueChange = viewModel::edit, enabled = !state.submitting,
                        placeholder = { Text("写下思路、解释或具体例子…") }, minLines = 4, maxLines = 10,
                        supportingText = { Text("${state.draft.trim().length} / 10000 · 至少 10 个字") },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 12.dp)) }
                    Button(onClick = { keyboard?.hide(); viewModel.submit() }, enabled = !state.submitting && !state.loading && state.draft.trim().length in 10..10000,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (state.submitting) "正在发布…" else "发布回答")
                    }
                    if (state.published) Text("回答已发布，可在上方查看", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
                }
            }
            if (state.question == null) state.error?.let { message -> item {
                RetryContent(message) { viewModel.load(id) }
            } }

        }
    }
}
