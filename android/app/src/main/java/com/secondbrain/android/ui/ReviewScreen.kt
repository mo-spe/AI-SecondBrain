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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lightbulb
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
internal fun ReviewSessionScreen(
    onBack: () -> Unit,
    viewModel: ReviewViewModel = hiltViewModel(),
    onOpenKnowledge: (Long) -> Unit = {}
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.ensureLoaded() }
    val card = state.cards.getOrNull(state.position)
    val prompt = remember(card) { parseReviewPrompt(card?.question.orEmpty()) }
    val scroll = rememberLazyListState()
    LaunchedEffect(card?.id) { scroll.scrollToItem(0) }
    LaunchedEffect(state.result) { if (state.result != null) scroll.animateScrollToItem(if (prompt.options.size >= 2) 4 + prompt.options.size else 5) }
    ReadingScaffold("今日复习", onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).imePadding(), state = scroll, contentPadding = PaddingValues(20.dp, 20.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
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
                        Text("第 ${state.position + 1} / ${state.cards.size} 张", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(if (prompt.options.size >= 2) "选择题" else "回忆练习", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    LinearProgressIndicator(progress = { (state.position + 1).toFloat() / state.cards.size }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(6.dp), trackColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                }
                item {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
                        Text(card.nodeTitle ?: "知识回顾", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                    }
                }
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
                            shape = RoundedCornerShape(18.dp),
                            color = when { wrong -> MaterialTheme.colorScheme.errorContainer; correct -> MaterialTheme.colorScheme.primaryContainer; selected -> MaterialTheme.colorScheme.surfaceContainerHigh; else -> MaterialTheme.colorScheme.surface },
                            border = BorderStroke(1.dp, when { wrong -> MaterialTheme.colorScheme.error; correct || selected -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.outlineVariant })
                        ) {
                            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    color = when { wrong -> MaterialTheme.colorScheme.error; correct -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.surfaceContainerHighest }
                                ) {
                                    Box(contentAlignment = Alignment.Center) { Text(option.key, style = MaterialTheme.typography.labelLarge, color = if (wrong || correct) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
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
                    if (state.result == null) Button(onClick = viewModel::submit, enabled = state.answer.isNotBlank() && !state.submitting && prompt.question.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) {
                        Text(if (state.submitting) "正在核对答案…" else "提交答案")
                    }
                }
                state.result?.let { result ->
                    val correctAnswer = result.correctAnswer ?: card.answer ?: "服务未提供"
                    val explanation = result.explanation?.takeIf { it.isNotBlank() }
                        ?: prompt.explanation.ifBlank { card.answer ?: "本题暂未提供详细解析。" }
                    item {
                        Surface(color = if (result.isCorrect) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(20.dp)) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Outlined.CheckCircle, null, tint = if (result.isCorrect) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                                    Text(if (result.isCorrect) "回答正确" else "再巩固一下", style = MaterialTheme.typography.titleLarge)
                                }
                                if (!result.isCorrect) Text("你的答案：${state.answer}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }
                    item {
                        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(20.dp)) {
                            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Column {
                                        Text("答案解析", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                        Text(if (prompt.options.size >= 2) "为什么是 $correctAnswer" else "理解这张卡片", style = MaterialTheme.typography.titleMedium)
                                    }
                                }
                                ReadingBody(explanation)
                            }
                        }
                    }
                    item {
                        Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(20.dp)) {
                            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("记住这一点", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Text(
                                        card.nodeTitle?.let { "下次回忆「$it」时，先从本题的判断依据开始。" }
                                            ?: "下次复习时，先从本题的判断依据开始回忆。",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                    item { Button(onClick = viewModel::next, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(16.dp)) { Text(if (state.position + 1 == state.cards.size) "完成复习" else "下一题") } }
                    card.nodeId?.let { nodeId -> item { TextButton(onClick = { onOpenKnowledge(nodeId) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("查看原知识") } } }
                }
                state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                if (prompt.question.isBlank()) item { OutlinedButton(onClick = viewModel::skipInvalid) { Text("跳过缺少题干的卡片") } }
            }
        }
    }
}
