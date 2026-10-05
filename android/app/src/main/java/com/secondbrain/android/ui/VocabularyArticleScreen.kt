package com.secondbrain.android.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.secondbrain.android.data.remote.VocabularyArticle
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VocabularyArticleScreen(onBack: () -> Unit,
                                     viewModel: VocabularyArticleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {
        viewModel.addImages(it)
    }
    Scaffold(topBar = { TopAppBar(title = { Text("词表文章") }, navigationIcon = {
        IconButton(onClick = { if (state.article != null) viewModel.closeArticle() else onBack() }) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
        }
    }) }) { padding ->
        val article = state.article
        if (article != null) {
            VocabularyReader(padding, article, onClose = viewModel::closeArticle)
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text("从今天的词，读一篇好文章", style = MaterialTheme.typography.headlineMedium)
                    Text("选择背词截图，核对词表后生成一篇完整英文文章。单词截图必须用视觉 AI 提取。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
                item {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("上传前请确认", style = MaterialTheme.typography.titleMedium)
                            Text("点击“视觉 AI 提取”后，截图会经后端发给你配置的模型服务商。请先裁掉账号、头像等无关信息；图片不会写入公共知识库。",
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (state.visionReady != true) item {
                    OutlinedButton(onClick = viewModel::refreshVisionStatus, enabled = !state.visionChecking,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text(if (state.visionChecking) "正在检查视觉模型…" else "检查视觉模型配置")
                    }
                }
                state.visionStatusMessage?.let { status -> item {
                    Text(status, style = MaterialTheme.typography.bodyMedium,
                        color = if (state.visionReady == false) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant)
                } }
                item {
                    OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Text("选择截图 · ${state.images.size}/8")
                    }
                }
                itemsIndexed(state.images) { index, path ->
                    val bitmap = remember(path) {
                        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 8 })
                            ?.asImageBitmap()
                    }
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            bitmap?.let { Image(it, contentDescription = "第 ${index + 1} 张词表截图",
                                modifier = Modifier.size(72.dp), contentScale = ContentScale.Crop) }
                            Column(Modifier.weight(1f)) {
                                Text("截图 ${index + 1}", style = MaterialTheme.typography.titleSmall)
                                Text("${File(path).length() / 1024} KB", style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { viewModel.removeImage(index) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "移除截图 ${index + 1}")
                            }
                        }
                    }
                }
                if (state.images.isNotEmpty()) item {
                    Button(onClick = viewModel::extract, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(if (state.busy) "正在识别…" else "视觉 AI 提取候选词")
                    }
                }
                if (state.extracted) {
                    item {
                        Text("核对词表 · ${state.candidates.size} 个候选词", style = MaterialTheme.typography.titleLarge)
                        Text("删除误识别词，补齐遗漏。标记“待确认”的词必须对照截图检查。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    itemsIndexed(state.candidates) { index, candidate ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(candidate.word, { viewModel.editWord(index, it) },
                                label = { Text(when {
                                    candidate.sourceImageIndex < 0 -> "手动补录"
                                    candidate.uncertain -> "待确认 · 截图 ${candidate.sourceImageIndex + 1}"
                                    else -> "单词 ${index + 1}"
                                }) },
                                singleLine = true, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.removeWord(index) }) {
                                Icon(Icons.Outlined.Close, contentDescription = "删除 ${candidate.word}")
                            }
                        }
                    }
                    item { TextButton(onClick = viewModel::addWord) { Text("补录遗漏单词") } }
                    item {
                        OutlinedButton(onClick = viewModel::confirm, enabled = !state.busy,
                            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("确认词表") }
                    }
                }
                if (state.confirmed) {
                    item { OutlinedTextField(state.topic, viewModel::setTopic, label = { Text("文章题材") },
                        modifier = Modifier.fillMaxWidth()) }
                    item { OutlinedTextField(state.difficulty, viewModel::setDifficulty,
                        label = { Text("阅读难度") }, modifier = Modifier.fillMaxWidth()) }
                    item { Button(onClick = viewModel::generate, enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                        Text(if (state.busy) "正在写作和核验…" else "生成一篇文章")
                    } }
                }
                state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.primary) } }
                if (state.history.isNotEmpty()) {
                    item { Text("再次阅读", style = MaterialTheme.typography.titleLarge) }
                    itemsIndexed(state.history) { _, saved ->
                        Card(Modifier.fillMaxWidth().clickable { viewModel.openArticle(saved.id) }) {
                            Column(Modifier.padding(16.dp)) {
                                Text(saved.topic, style = MaterialTheme.typography.titleMedium)
                                Text("${saved.coveredCount}/${saved.wordCount} 词 · ${saved.createTime}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabularyReader(padding: PaddingValues, article: VocabularyArticle, onClose: () -> Unit) {
    var selectedWord by remember(article.id) { mutableStateOf<String?>(null) }
    val targetWords = remember(article.id) { article.words.associateBy { it.lowercase(Locale.ROOT) } }
    val targetWordColor = MaterialTheme.colorScheme.primary
    val annotated = remember(article.id, targetWordColor) {
        buildAnnotatedString {
            val pattern = Regex("[A-Za-z][A-Za-z'-]*")
            var cursor = 0
            pattern.findAll(article.article).forEach { match ->
                append(article.article.substring(cursor, match.range.first))
                val start = length
                append(match.value)
                val key = match.value.lowercase(Locale.ROOT)
                if (targetWords.containsKey(key)) {
                    addStyle(SpanStyle(color = targetWordColor, fontWeight = FontWeight.SemiBold),
                        start, length)
                    addStringAnnotation("word", key, start, length)
                }
                cursor = match.range.last + 1
            }
            append(article.article.substring(cursor))
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(22.dp, 12.dp, 22.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Text(article.topic, style = MaterialTheme.typography.headlineMedium)
            Text("已覆盖 ${article.words.size - article.missingWords.size}/${article.words.size} 个目标词",
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
            if (article.missingWords.isNotEmpty()) {
                Text("未覆盖：${article.missingWords.joinToString("、")}",
                    color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item {
            ClickableText(text = annotated, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 30.sp),
                onClick = { offset -> selectedWord = annotated.getStringAnnotations("word", offset, offset)
                    .firstOrNull()?.item })
        }
        item { OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("返回词表") } }
    }
    selectedWord?.let { word ->
        val contextSentence = remember(article.id, word) {
            val target = Regex("(?i)(?<![A-Za-z'-])${Regex.escape(word)}(?![A-Za-z'-])")
            article.article.split(Regex("(?<=[.!?])\\s+"))
                .firstOrNull { target.containsMatchIn(it) }
        }
        ModalBottomSheet(onDismissRequest = { selectedWord = null }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("当前语境", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text(word, style = MaterialTheme.typography.headlineSmall)
                Text(article.meanings[word] ?: "这篇文章暂未提供该词的可靠释义，请结合上下文理解。",
                    style = MaterialTheme.typography.bodyLarge)
                contextSentence?.let { sentence ->
                    Text(sentence, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { selectedWord = null },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("关闭") }
            }
        }
    }
}
