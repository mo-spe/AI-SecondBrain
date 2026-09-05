package com.secondbrain.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.secondbrain.android.review.ReviewViewModel
import com.secondbrain.android.review.parseReviewPrompt

@Composable
internal fun ReviewSessionScreen(onBack: () -> Unit, viewModel: ReviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.ensureLoaded() }
    val card = state.cards.getOrNull(state.position)
    val prompt = remember(card) { parseReviewPrompt(card?.question.orEmpty()) }
    val scroll = rememberLazyListState()
    LaunchedEffect(card?.id) { scroll.scrollToItem(0) }
    LaunchedEffect(state.result) { if (state.result != null) scroll.animateScrollToItem(if (prompt.options.size >= 2) 4 + prompt.options.size else 5) }
    ReadingScaffold("今日复习", onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), state = scroll, contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (state.loading) {
                item { LoadingContent("正在准备复习卡片…") }
            } else if (card == null) {
                item {
                    if (state.message != null) RetryContent(state.message!!, viewModel::load)
                    else Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                        Text(if (state.cards.isEmpty()) "今天暂时没有待复习内容" else "本轮复习已完成", style = MaterialTheme.typography.headlineSmall)
                        Text("已完成 ${state.position} 张卡片", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onBack) { Text("返回今日") }
                    }
                }
            } else {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("第 ${state.position + 1} / ${state.cards.size} 题", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(if (prompt.options.size >= 2) "选择题" else "回忆练习", style = MaterialTheme.typography.labelSmall)
                    }
                    LinearProgressIndicator(progress = { state.position.toFloat() / state.cards.size }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
                }
                item { Text(card.nodeTitle ?: "知识回顾", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                item {
                    Text(prompt.question.ifBlank { "这张卡片缺少题干" }, style = MaterialTheme.typography.headlineSmall)
                    if (prompt.question.isBlank()) Text("可跳过本题，不会记录为错误答案。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (prompt.options.size >= 2) {
                    items(prompt.options, key = { it.key }) { option ->
                        val selected = state.answer == option.key
                        val correct = state.result?.correctAnswer?.trim()?.uppercase() == option.key
                        val wrong = state.result != null && selected && !state.result!!.isCorrect
                        Surface(
                            modifier = Modifier.fillMaxWidth().selectable(selected = selected, enabled = state.result == null && !state.submitting, role = Role.RadioButton, onClick = { viewModel.updateAnswer(option.key) }),
                            shape = RoundedCornerShape(14.dp),
                            color = when { wrong -> MaterialTheme.colorScheme.errorContainer; correct || selected -> MaterialTheme.colorScheme.primaryContainer; else -> MaterialTheme.colorScheme.surface },
                            border = BorderStroke(1.dp, if (selected || correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                                Text(option.key, style = MaterialTheme.typography.titleMedium)
                                Column(Modifier.weight(1f)) {
                                    Text(option.text, style = MaterialTheme.typography.bodyLarge)
                                    if (correct) Text("正确答案", style = MaterialTheme.typography.labelSmall)
                                    else if (wrong) Text("你的选择 · 回看解析", style = MaterialTheme.typography.labelSmall)
                                    else if (selected) Text("已选择", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                } else item {
                    OutlinedTextField(state.answer, viewModel::updateAnswer, enabled = !state.submitting && state.result == null, label = { Text("写下你的理解") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                }
                item {
                    if (state.result == null) Button(onClick = viewModel::submit, enabled = state.answer.isNotBlank() && !state.submitting && prompt.question.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) {
                        Text(if (state.submitting) "正在核对答案…" else "提交答案")
                    }
                }
                state.result?.let { result ->
                    item {
                        Surface(color = if (result.isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (result.isCorrect) "回答正确" else "再巩固一下", style = MaterialTheme.typography.titleLarge)
                                Text("你的答案：${state.answer}", style = MaterialTheme.typography.bodyMedium)
                                Text("正确答案：${result.correctAnswer ?: card.answer ?: "服务未提供"}", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                    item {
                        Text("答案解析", style = MaterialTheme.typography.titleMedium)
                        ReadingBody(result.explanation?.takeIf { it.isNotBlank() } ?: prompt.explanation.ifBlank { card.answer ?: "本题暂未提供详细解析。" })
                    }
                    item { Button(onClick = viewModel::next, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) { Text(if (state.position + 1 == state.cards.size) "完成复习" else "下一题") } }
                }
                state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                if (prompt.question.isBlank()) item { OutlinedButton(onClick = viewModel::skipInvalid) { Text("跳过缺少题干的卡片") } }
            }
        }
    }
}
