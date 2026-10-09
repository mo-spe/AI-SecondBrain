package com.secondbrain.android.ui

import android.content.Context
import android.app.Activity
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.core.view.WindowCompat
import com.secondbrain.android.data.remote.VocabularyArticle
import java.io.File
import java.util.Locale

private enum class ReaderTone { WARM, CLEAR, NIGHT }

private data class ReaderPalette(
    val background: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val onAccent: Color,
    val softAccent: Color,
    val section: Color,
    val track: Color
)

@Composable
private fun readerPalette(tone: ReaderTone): ReaderPalette {
    val scheme = MaterialTheme.colorScheme
    return when (tone) {
        ReaderTone.WARM -> ReaderPalette(
            Color(0xFFF5F0E6), Color(0xFF29342E), Color(0xFF5C6A60), Color(0xFF326B5C),
            Color.White, Color(0xFFE2EBDD), Color(0xFFEDE7DA), Color(0xFFCAD7C8))
        ReaderTone.CLEAR -> ReaderPalette(
            scheme.background, scheme.onBackground, scheme.onSurfaceVariant, scheme.primary,
            scheme.onPrimary, scheme.primaryContainer, scheme.surfaceContainerLow, scheme.outlineVariant)
        ReaderTone.NIGHT -> ReaderPalette(
            Color(0xFF171E1C), Color(0xFFE7ECE5), Color(0xFFB4C2B8), Color(0xFFAAD8BE),
            Color(0xFF183228), Color(0xFF253A31), Color(0xFF222E29), Color(0xFF3C5146))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VocabularyArticleScreen(onBack: () -> Unit,
                                     viewModel: VocabularyArticleViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val readingPreferences = remember(context) {
        context.getSharedPreferences("vocabulary_reader", Context.MODE_PRIVATE)
    }
    val defaultTone = if (isSystemInDarkTheme()) ReaderTone.NIGHT else ReaderTone.WARM
    var toneName by rememberSaveable {
        mutableStateOf(readingPreferences.getString("tone", defaultTone.name) ?: defaultTone.name)
    }
    var fontSize by rememberSaveable {
        mutableStateOf(readingPreferences.getInt("font_size", 18).coerceIn(16, 26))
    }
    var spacious by rememberSaveable {
        mutableStateOf(readingPreferences.getBoolean("spacious", true))
    }
    var focusMode by rememberSaveable {
        mutableStateOf(readingPreferences.getBoolean("focus_mode", false))
    }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val tone = ReaderTone.entries.firstOrNull { it.name == toneName } ?: defaultTone
    val palette = readerPalette(tone)
    val article = state.article
    var translationOpen by remember(article?.id, article?.article) { mutableStateOf(false) }
    val darkReader = tone == ReaderTone.NIGHT ||
        (tone == ReaderTone.CLEAR && isSystemInDarkTheme())
    DisposableEffect(article != null, darkReader, context) {
        val activity = context as? Activity
        val bars = activity?.let { WindowCompat.getInsetsController(it.window, it.window.decorView) }
        val previousStatus = bars?.isAppearanceLightStatusBars
        val previousNavigation = bars?.isAppearanceLightNavigationBars
        if (article != null) {
            bars?.isAppearanceLightStatusBars = !darkReader
            bars?.isAppearanceLightNavigationBars = !darkReader
        }
        onDispose {
            if (bars != null && previousStatus != null) bars.isAppearanceLightStatusBars = previousStatus
            if (bars != null && previousNavigation != null) bars.isAppearanceLightNavigationBars = previousNavigation
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) {
        viewModel.addImages(it)
    }
    Scaffold(containerColor = if (article == null) MaterialTheme.colorScheme.background else palette.background,
        topBar = { TopAppBar(title = { Text(if (article == null) "词表文章" else "阅读") }, navigationIcon = {
        IconButton(onClick = { if (state.article != null) viewModel.closeArticle() else onBack() }) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
        }
    }, actions = {
        if (article != null) {
            TextButton(onClick = { settingsOpen = true },
                modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = palette.accent)) {
                Text("Aa  阅读设置", fontWeight = FontWeight.SemiBold)
            }
        }
    }, colors = TopAppBarDefaults.topAppBarColors(
        containerColor = if (article == null) MaterialTheme.colorScheme.background else palette.background,
        titleContentColor = if (article == null) MaterialTheme.colorScheme.onBackground else palette.ink,
        navigationIconContentColor = if (article == null) MaterialTheme.colorScheme.onBackground else palette.ink)) }) { padding ->
        if (article != null) {
            VocabularyReader(padding, article, state.busy, state.translationBusy, state.message, palette,
                fontSize, spacious, focusMode,
                onComplete = viewModel::completeMissingWords, onClose = viewModel::closeArticle,
                onTranslate = { viewModel.dismissWord(); translationOpen = true; viewModel.loadTranslation() },
                wordLookup = state.wordLookup, onWord = viewModel::selectWord, onDismissWord = viewModel::dismissWord)
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text("在语境里重逢生词", style = MaterialTheme.typography.headlineSmall)
                    Text("选择背词截图，核对词表后生成一篇完整英文文章。单词截图必须用视觉 AI 提取。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
                item {
                    val currentStep = if (state.confirmed) 2 else if (state.extracted) 1 else 0
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("选择截图", "核对词表", "生成文章").forEachIndexed { index, title ->
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("${index + 1}  $title", style = MaterialTheme.typography.labelMedium,
                                    color = if (index <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                HorizontalDivider(thickness = 3.dp,
                                    color = if (index <= currentStep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
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
                                singleLine = true, shape = RoundedCornerShape(12.dp), modifier = Modifier.weight(1f))
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
                item {
                    Column {
                        Text("再次阅读", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { viewModel.refreshHistory(reportFailure = true) }, enabled = !state.busy) {
                            Text("刷新历史文章")
                        }
                        if (state.history.isEmpty()) Text("已保存的文章会显示在这里。",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (state.history.isNotEmpty()) {
                    itemsIndexed(state.history) { _, saved ->
                        Card(Modifier.fillMaxWidth().clickable(enabled = !state.busy) { viewModel.openArticle(saved.id) }) {
                            Column(Modifier.padding(16.dp)) {
                                Text(saved.topic, style = MaterialTheme.typography.titleMedium)
                                Text("${saved.coveredCount}/${saved.wordCount} 词 · ${saved.createTime.take(16).replace('T', ' ')}",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
    if (translationOpen && article != null) {
        ModalBottomSheet(onDismissRequest = { translationOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = palette.background) {
            Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("完整翻译", style = MaterialTheme.typography.headlineSmall, color = palette.ink)
                    TextButton(onClick = { translationOpen = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = palette.accent)) { Text("返回英文") }
                }
                when {
                    state.translationBusy -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth(), color = palette.accent,
                            trackColor = palette.track)
                        Text("正在翻译全文…", color = palette.muted)
                    }
                    state.translationError != null -> {
                        Text(state.translationError!!, color = palette.ink)
                        OutlinedButton(onClick = viewModel::loadTranslation,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent)) {
                            Text("重试全文翻译")
                        }
                    }
                    state.translation != null -> {
                        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(if (spacious) 24.dp else 18.dp)) {
                            items(state.translation!!.split(Regex("\\n\\s*\\n"))) { paragraph ->
                                Text(paragraph, color = palette.ink,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp,
                                        lineHeight = (fontSize * if (spacious) 1.8f else 1.55f).sp))
                            }
                        }
                    }
                }
            }
        }
    }
    if (settingsOpen && article != null) {
        ModalBottomSheet(onDismissRequest = { settingsOpen = false },
            containerColor = palette.background) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("阅读设置", style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, color = palette.ink)
                Text("选择舒服的纸面和文字节奏，设置会在下次阅读时保留。",
                    style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("纸面", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(ReaderTone.WARM to "暖纸", ReaderTone.CLEAR to "纯净",
                            ReaderTone.NIGHT to "夜间").forEach { (option, label) ->
                            FilterChip(selected = tone == option, onClick = {
                                toneName = option.name
                                readingPreferences.edit().putString("tone", toneName).apply()
                            }, modifier = Modifier.heightIn(min = 48.dp),
                                label = { Text(label) }, colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = palette.softAccent,
                                selectedLabelColor = palette.accent,
                                labelColor = palette.muted))
                        }
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("字号", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        OutlinedButton(onClick = {
                            fontSize = (fontSize - 1).coerceAtLeast(16)
                            readingPreferences.edit().putInt("font_size", fontSize).apply()
                        }, enabled = fontSize > 16, modifier = Modifier.heightIn(min = 48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent)) {
                            Text("A−")
                        }
                        Text("$fontSize", style = MaterialTheme.typography.titleLarge,
                            color = palette.ink)
                        OutlinedButton(onClick = {
                            fontSize = (fontSize + 1).coerceAtMost(26)
                            readingPreferences.edit().putInt("font_size", fontSize).apply()
                        }, enabled = fontSize < 26, modifier = Modifier.heightIn(min = 48.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent)) {
                            Text("A+")
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("舒展行距", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                        Text("长文阅读时给每行更多呼吸空间",
                            style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                    Switch(checked = spacious, onCheckedChange = {
                        spacious = it
                        readingPreferences.edit().putBoolean("spacious", it).apply()
                    }, colors = SwitchDefaults.colors(checkedTrackColor = palette.accent))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text("专注模式", style = MaterialTheme.typography.titleSmall, color = palette.ink)
                        Text("阅读时收起顶部的覆盖统计",
                            style = MaterialTheme.typography.bodySmall, color = palette.muted)
                    }
                    Switch(checked = focusMode, onCheckedChange = {
                        focusMode = it
                        readingPreferences.edit().putBoolean("focus_mode", it).apply()
                    }, colors = SwitchDefaults.colors(checkedTrackColor = palette.accent))
                }
                TextButton(onClick = { settingsOpen = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = palette.accent)) {
                    Text("完成")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VocabularyReader(padding: PaddingValues, article: VocabularyArticle, busy: Boolean,
                             translationBusy: Boolean, message: String?, palette: ReaderPalette, fontSize: Int,
                             spacious: Boolean, focusMode: Boolean,
                             onComplete: () -> Unit, onClose: () -> Unit, onTranslate: () -> Unit,
                             wordLookup: VocabularyWordLookup?, onWord: (Int, Int) -> Unit,
                             onDismissWord: () -> Unit) {
    val targetWords = remember(article.words) { article.words.associateBy { it.lowercase(Locale.ROOT) } }
    val targetWordColor = palette.accent
    val paragraphs = remember(article.article) {
        readingParagraphs(article.article)
    }
    val covered = article.words.size - article.missingWords.size
    LazyColumn(Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(24.dp, 16.dp, 24.dp, 96.dp),
        verticalArrangement = Arrangement.spacedBy(if (spacious) 24.dp else 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        item {
            Column(Modifier.widthIn(max = 680.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("词表阅读  ·  ${article.difficulty}", style = MaterialTheme.typography.labelLarge,
                    color = palette.accent)
                Text(article.topic, style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold, color = palette.ink)
                Text("轻点任意英文词查看释义；高亮词是本次目标词。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.muted)
                OutlinedButton(onClick = onTranslate, enabled = !busy,
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent)) {
                    Text("查看完整翻译")
                }
            }
        }
        if (!focusMode) item {
            Surface(color = palette.softAccent,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("目标词覆盖", style = MaterialTheme.typography.labelLarge, color = palette.muted)
                        Text("$covered / ${article.words.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold, color = palette.ink)
                    }
                    LinearProgressIndicator(
                        progress = { if (article.words.isEmpty()) 0f else covered.toFloat() / article.words.size },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                        color = palette.accent, trackColor = palette.track)
                }
            }
        }
        itemsIndexed(paragraphs, key = { _, item -> item.start }) { paragraphIndex, section ->
            val paragraph = section.text
            if (paragraph == "补充阅读" || paragraph.equals("Supplementary reading", ignoreCase = true)) {
                Text("补充阅读", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = palette.accent,
                    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth().padding(top = 12.dp))
            } else {
                val annotated = remember(article.article, section, palette, targetWords, onWord) {
                    buildAnnotatedString {
                        append(paragraph)
                        readingWordPattern.findAll(paragraph).forEach { match ->
                            val start = match.range.first
                            val end = match.range.last + 1
                            val key = match.value.lowercase(Locale.ROOT)
                            val target = targetWords.containsKey(key)
                            addLink(LinkAnnotation.Clickable(
                                tag = "word-${section.start + start}",
                                styles = TextLinkStyles(
                                    style = SpanStyle(color = if (target) targetWordColor else palette.ink,
                                        fontWeight = if (target) FontWeight.SemiBold else FontWeight.Normal,
                                        textDecoration = TextDecoration.None),
                                    focusedStyle = SpanStyle(textDecoration = TextDecoration.Underline),
                                    pressedStyle = SpanStyle(background = palette.softAccent)),
                                linkInteractionListener = { onWord(section.start + start, section.start + end) }
                            ), start, end)
                        }
                    }
                }
                Text(text = annotated,
                    modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()
                        .testTag("vocabulary-paragraph-$paragraphIndex"),
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = fontSize.sp,
                        lineHeight = (fontSize * if (spacious) 1.8f else 1.55f).sp,
                        color = palette.ink))
            }
        }
        if (article.missingWords.isNotEmpty()) item {
            Surface(color = palette.section,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("待补齐的词", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold, color = palette.ink)
                    Text(article.missingWords.joinToString("  ·  "),
                        style = MaterialTheme.typography.bodyMedium, color = palette.ink)
                    Button(onClick = onComplete, enabled = !busy && !translationBusy,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent,
                            contentColor = palette.onAccent)) {
                        Text(if (busy) "正在补齐并核验…" else "生成补充阅读")
                    }
                    Text("会接在这篇文章末尾；完成后重新核验覆盖。",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted)
                }
            }
        }
        message?.let { notice -> item {
            Text(notice, style = MaterialTheme.typography.bodyMedium,
                color = palette.accent,
                modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth())
        } }
        item { OutlinedButton(onClick = onClose,
            modifier = Modifier.widthIn(max = 680.dp).fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent)) {
            Text("返回词表")
        } }
    }
    wordLookup?.let { lookup ->
        ModalBottomSheet(onDismissRequest = onDismissWord,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = palette.background) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("当前语境", style = MaterialTheme.typography.labelLarge,
                    color = palette.accent)
                Text(lookup.selection.word, style = MaterialTheme.typography.headlineSmall, color = palette.ink)
                if (lookup.busy) {
                    LinearProgressIndicator(Modifier.fillMaxWidth(), color = palette.accent, trackColor = palette.track)
                    Text("正在查询这句话中的释义…", color = palette.muted)
                }
                lookup.meaning?.let {
                    Text(it, style = MaterialTheme.typography.bodyLarge, color = palette.ink)
                }
                lookup.error?.let {
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = palette.ink)
                    OutlinedButton(onClick = { onWord(lookup.selection.start, lookup.selection.end) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent)) {
                        Text("重试释义")
                    }
                }
                Text(lookup.selection.sentence, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
                TextButton(onClick = onDismissWord,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = palette.accent)) {
                    Text("关闭")
                }
            }
        }
    }
}
