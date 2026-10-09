package com.secondbrain.android.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.secondbrain.android.capture.StudyArchiveDraft
import com.secondbrain.android.data.remote.DoubtRecord
import com.secondbrain.android.data.remote.UpdateDoubtRequest
import com.secondbrain.android.data.remote.UpdateWrongQuestionRequest
import com.secondbrain.android.data.remote.WrongQuestionRecord
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudyArchiveScreen(onBack: () -> Unit, viewModel: StudyArchiveViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var typeDialog by remember { mutableStateOf(false) }
    var addDialog by remember { mutableStateOf(false) }
    var pendingType by remember { mutableStateOf("WRONG") }
    var cameraOpen by remember { mutableStateOf(false) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.beginDraft(pendingType, it) }
    }
    if (cameraOpen) {
        com.secondbrain.android.capture.CameraCaptureScreen(
            onCaptured = { uri -> cameraOpen = false; viewModel.beginDraft(pendingType, uri) },
            onBack = { cameraOpen = false },
            onError = { viewModel.showMessage(it) }
        )
        return
    }
    val draft = state.draft
    val wrongDetail = state.wrongDetail
    val doubtDetail = state.doubtDetail
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (draft != null) if (draft.archiveType == "WRONG") "整理错题" else "记录疑问" else if (wrongDetail != null) "错题复盘" else if (doubtDetail != null) "疑问详情" else "学习记录") },
                navigationIcon = {
                    IconButton(onClick = { if (draft != null) viewModel.cancelEditor() else if (wrongDetail != null || doubtDetail != null) viewModel.closeDetail() else onBack() }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            if (draft == null && wrongDetail == null && doubtDetail == null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (selectedTab == 2) typeDialog = true
                        else { pendingType = if (selectedTab == 0) "WRONG" else "DOUBT"; addDialog = true }
                    },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text(if (selectedTab == 0) "收录错题" else if (selectedTab == 1) "记录疑问" else "新增记录") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { padding ->
        when {
            draft != null -> ArchiveEditor(padding, draft, state.saving, state.message, state.suggestion,
                state.visualSuggestion, onEdit = viewModel::editDraft, onSuggest = viewModel::requestSuggestion,
                onVisualRecognize = viewModel::requestVisualRecognition, onSave = viewModel::saveDraft)
            wrongDetail != null -> WrongQuestionDetailScreen(padding, wrongDetail.record, wrongDetail.reviews.map { "${it.result} · ${it.createTime.orEmpty()} ${it.note.orEmpty()}" }, state.imageBytes,
                imageMessage = state.imageMessage,
                imageLoading = state.imageLoading,
                onRetryImage = viewModel::retryWrongImage,
                onSchedule = { days -> viewModel.scheduleWrong(wrongDetail.record.id, days) },
                onScheduleAt = { time -> viewModel.scheduleWrongAt(wrongDetail.record.id, time) },
                onResult = { result, note -> viewModel.recordWrongReview(wrongDetail.record.id, result, note) },
                onUpdate = { request -> viewModel.updateWrong(wrongDetail.record.id, request) },
                onMaster = { viewModel.masterWrong(wrongDetail.record.id) }, message = state.message)
            doubtDetail != null -> DoubtDetailScreen(padding, doubtDetail.record, doubtDetail.understandings.map { "${it.understandingStatus} · ${it.createTime.orEmpty()}\n${it.content}" }, state.imageBytes, state.imageMessage, state.aiExplanation, state.saving, state.message,
                imageLoading = state.imageLoading,
                onRetryImage = viewModel::retryDoubtImage,
                onSchedule = { days -> viewModel.scheduleDoubt(doubtDetail.record.id, days) },
                onScheduleAt = { time -> viewModel.scheduleDoubtAt(doubtDetail.record.id, time) },
                onUnderstanding = { content, status -> viewModel.addUnderstanding(doubtDetail.record.id, content, status) },
                onResolve = { viewModel.resolveDoubt(doubtDetail.record.id) },
                onExplain = { viewModel.explainDoubt(doubtDetail.record.id) },
                onReopen = { viewModel.updateDoubt(doubtDetail.record.id, "PENDING") },
                onAiFeedback = { feedback -> viewModel.setAiExplanationFeedback(doubtDetail.record.id, feedback) },
                onUpdate = { request -> viewModel.updateDoubt(doubtDetail.record.id, request) })
            else -> ArchiveLists(padding, selectedTab, { selectedTab = it }, state, viewModel::refresh,
                onOpenWrong = viewModel::openWrong, onOpenDoubt = viewModel::openDoubt, onResumeDraft = viewModel::resumeDraft)
        }
    }
    if (typeDialog) {
        AlertDialog(
            onDismissRequest = { typeDialog = false },
            title = { Text("要记录什么？") },
            text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { typeDialog = false; selectedTab = 0; pendingType = "WRONG"; addDialog = true }) { Text("错题：拍下原题，之后自己安排复习") }
                OutlinedButton(onClick = { typeDialog = false; selectedTab = 1; pendingType = "DOUBT"; addDialog = true }) { Text("疑问：记下问题和资料出处") }
            } },
            confirmButton = { TextButton(onClick = { typeDialog = false }) { Text("取消") } }
        )
    }
    if (addDialog) {
        AlertDialog(
            onDismissRequest = { addDialog = false },
            title = { Text(if (pendingType == "DOUBT") "记录疑问" else "收录错题") },
            text = { Text(if (pendingType == "DOUBT") "可先写下问题，也可以拍照或从相册选择讲义页面。" else "拍摄或选择原题图片，之后可以补充答案、解析和来源。") },
            confirmButton = { TextButton(onClick = { addDialog = false; gallery.launch("image/*") }) { Icon(Icons.Outlined.Collections, null); Text("相册") } },
            dismissButton = { Row {
                if (pendingType == "DOUBT") TextButton(onClick = { addDialog = false; viewModel.beginTextDoubt() }) { Text("只写文字") }
                TextButton(onClick = { addDialog = false; cameraOpen = true }) { Icon(Icons.Outlined.CameraAlt, null); Text("拍照") }
            } }
        )
    }
}

@Composable
private fun ArchiveLists(
    padding: PaddingValues,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    state: StudyArchiveUiState,
    onRefresh: () -> Unit,
    onOpenWrong: (Long) -> Unit,
    onOpenDoubt: (Long) -> Unit,
    onResumeDraft: (StudyArchiveDraft) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable(selectedTab) { mutableStateOf("ALL") }
    val term = query.trim()
    val baseWrong = when (selectedTab) { 0 -> state.wrongQuestions; 2 -> state.today.wrongQuestions; else -> emptyList() }
    val baseDoubts = when (selectedTab) { 1 -> state.doubts; 2 -> state.today.doubts; else -> emptyList() }
    val visibleDrafts = state.drafts.filter { draft ->
        statusFilter == "ALL" &&
            (selectedTab == 2 || (selectedTab == 0 && draft.archiveType == "WRONG") || (selectedTab == 1 && draft.archiveType == "DOUBT")) &&
            (term.isBlank() || listOf(draft.sourceBook, draft.ocrText, draft.content, draft.chapter, draft.knowledgePoints).any { it.contains(term, ignoreCase = true) })
    }
    val visibleWrong = baseWrong.filter { row ->
        (statusFilter == "ALL" || row.reviewStatus == statusFilter) &&
            (term.isBlank() || listOfNotNull(row.subject, row.sourceBook, row.sourcePage, row.chapter, row.knowledgePoints, row.errorType, row.ocrText).any { it.contains(term, ignoreCase = true) })
    }
    val visibleDoubts = baseDoubts.filter { row ->
        (statusFilter == "ALL" || row.status == statusFilter) &&
            (term.isBlank() || listOfNotNull(row.content, row.sourceBook, row.sourcePage, row.chapter, row.doubtType).any { it.contains(term, ignoreCase = true) })
    }
    val filters = when (selectedTab) {
        0 -> listOf("ALL" to "全部", "UNSCHEDULED" to "未安排", "SCHEDULED" to "已安排", "MASTERED" to "已掌握", "ARCHIVED" to "已归档")
        1 -> listOf("ALL" to "全部", "PENDING" to "待处理", "UNDERSTOOD" to "已有理解", "VERIFYING" to "待验证", "RESOLVED" to "已解决")
        else -> emptyList()
    }
    Column(Modifier.fillMaxSize().padding(padding)) {
        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background) {
            listOf("错题本", "疑问箱", "今日待处理").forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { onSelectTab(index) }, text = { Text(title) })
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(when (selectedTab) { 0 -> "把错题，变成收获"; 1 -> "给每个疑问一个去处"; else -> "今天，回看一点" },
                        style = MaterialTheme.typography.titleLarge)
                    Text(when (selectedTab) { 0 -> "留住原题与思路，按自己的节奏复盘。"; 1 -> "记录卡住的地方，让理解慢慢清晰。"; else -> "只展示你主动安排到期的错题与疑问。" },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (!state.loading) {
                        Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                                ArchiveMetric("${baseWrong.size + baseDoubts.size}", if (selectedTab == 2) "今日待处理" else "已收录")
                                ArchiveMetric(
                                    "${if (selectedTab == 0) baseWrong.count { it.reviewStatus == "SCHEDULED" } else if (selectedTab == 1) baseDoubts.count { it.status == "RESOLVED" } else baseWrong.size}",
                                    if (selectedTab == 0) "已安排复习" else if (selectedTab == 1) "已解决" else "错题复盘"
                                )
                            }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(query, { query = it }, label = { Text("搜索学习记录") }, placeholder = { Text("题目、书名或知识点") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "清空搜索") } },
                    shape = RoundedCornerShape(16.dp), singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (filters.isNotEmpty()) item {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    filters.forEach { (value, label) ->
                        FilterChip(selected = statusFilter == value, onClick = { statusFilter = value }, label = { Text(label) }, modifier = Modifier.heightIn(min = 48.dp))
                    }
                }
            }
            state.message?.let { message -> item {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = onRefresh) { Text("重新读取") }
                    }
                }
            } }
            if (state.loading) item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                if (visibleDrafts.isNotEmpty()) item { ArchiveListHeading("继续整理", "${visibleDrafts.size} 份本机草稿") }
                items(visibleDrafts, key = { "draft-${it.id}" }) { draft ->
                    ArchiveRowCard(
                        title = if (draft.archiveType == "WRONG") "待保存错题" else "待保存疑问",
                        subtitle = draft.ocrText.ifBlank { draft.content.ifBlank { "补充来源和笔记，完成这份记录。" } },
                        metadata = draft.sourceBook.ifBlank { "本机草稿 · 尚未上传" }, badge = "草稿", isDoubt = draft.archiveType == "DOUBT",
                        onClick = { onResumeDraft(draft) }
                    )
                }
                if (visibleWrong.isNotEmpty() || visibleDoubts.isNotEmpty()) item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${visibleWrong.size + visibleDoubts.size} 条记录", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "刷新学习记录", tint = MaterialTheme.colorScheme.primary) }
                    }
                }
                items(visibleWrong, key = { "wrong-${it.id}" }) { row ->
                    ArchiveRowCard(
                        title = row.sourceBook?.takeIf(String::isNotBlank) ?: row.subject?.takeIf(String::isNotBlank) ?: "错题记录",
                        subtitle = row.ocrText?.takeIf(String::isNotBlank) ?: "查看原题照片，回忆当时的解题思路。",
                        metadata = listOfNotNull(row.sourcePage?.takeIf(String::isNotBlank)?.let { "第 $it 页" }, row.chapter?.takeIf(String::isNotBlank), row.errorType?.takeIf(String::isNotBlank)).joinToString(" · "),
                        badge = wrongStatus(row.reviewStatus), isDoubt = false,
                        footer = row.nextReviewTime?.let { "复习安排 · ${it.replace('T', ' ').take(16)}" },
                        onClick = { onOpenWrong(row.id) }
                    )
                }
                items(visibleDoubts, key = { "doubt-${it.id}" }) { row ->
                    ArchiveRowCard(
                        title = row.content,
                        subtitle = listOfNotNull(row.sourceBook?.takeIf(String::isNotBlank), row.sourcePage?.takeIf(String::isNotBlank)?.let { "第 $it 页" }, row.chapter?.takeIf(String::isNotBlank)).joinToString(" · "),
                        metadata = row.doubtType.orEmpty(), badge = doubtStatus(row.status), isDoubt = true,
                        footer = row.nextProcessTime?.let { "处理安排 · ${it.replace('T', ' ').take(16)}" },
                        onClick = { onOpenDoubt(row.id) }
                    )
                }
                if (visibleWrong.isEmpty() && visibleDoubts.isEmpty() && visibleDrafts.isEmpty()) item {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(if (selectedTab == 1) Icons.Outlined.HelpOutline else Icons.Outlined.Description, null,
                                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                            Text(if (state.message != null) "记录暂时未能加载" else if (term.isNotBlank() || statusFilter != "ALL") "没有匹配的记录" else if (selectedTab == 2) "今天暂时没有安排" else if (selectedTab == 0) "从一道错题开始" else "先记下来，之后再解开",
                                style = MaterialTheme.typography.titleLarge)
                            Text(if (state.message != null) "请重试读取；这不代表你的档案为空。" else if (term.isNotBlank() || statusFilter != "ALL") "试试其他关键词，或切换到全部记录。" else if (selectedTab == 2) "错题和疑问仍在档案里。只有你设置了时间，才会进入今日待处理。" else if (selectedTab == 0) "点击下方「收录错题」，拍下原题并记录出处。保存后不会自动安排复习。" else "点击下方「记录疑问」，留下问题与资料出处，之后持续补充自己的理解。",
                                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (term.isNotBlank() || statusFilter != "ALL") TextButton(onClick = { query = ""; statusFilter = "ALL" }) { Text("查看全部记录") }
                            else TextButton(onClick = onRefresh) { Text("重新读取") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchiveMetric(value: String, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ArchiveListHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ArchiveBadge(label: String) {
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
    }
}

@Composable
private fun ArchiveRowCard(
    title: String, subtitle: String, metadata: String, badge: String, isDoubt: Boolean,
    footer: String? = null, onClick: () -> Unit
) {
    Column {
        Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
            Row(Modifier.padding(vertical = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(if (isDoubt) Icons.Outlined.HelpOutline else Icons.Outlined.Description, null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (isDoubt) "疑问记录" else "错题档案", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        ArchiveBadge(badge)
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (metadata.isNotBlank()) Text(metadata, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (footer != null) Text(footer, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ArchiveEditor(
    padding: PaddingValues,
    draft: StudyArchiveDraft,
    saving: Boolean,
    message: String?,
    suggestion: com.secondbrain.android.data.remote.StudyAiSuggestion?,
    visualSuggestion: com.secondbrain.android.data.remote.VisualQuestionSuggestion?,
    onEdit: ((StudyArchiveDraft) -> StudyArchiveDraft) -> Unit,
    onSuggest: () -> Unit,
    onVisualRecognize: () -> Unit,
    onSave: () -> Unit
) {
    Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (draft.archiveType == "WRONG") "原题照片会留在错题档案中。" else "先把问题和出处记下来，之后再回来整理。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        draft.imagePath.takeIf(String::isNotBlank)?.let { path ->
            Text("图片已保存在本机草稿", color = MaterialTheme.colorScheme.primary)
            val bitmap = remember(path) { BitmapFactory.decodeFile(path)?.asImageBitmap() }
            bitmap?.let { Image(it, contentDescription = "待归档学习资料", modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp), contentScale = ContentScale.Fit) }
        }
        if (draft.archiveType == "WRONG") {
            OutlinedTextField(draft.ocrText, { onEdit { old -> old.copy(ocrText = it) } }, label = { Text("题目文字（可编辑）") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            Text("识别结果仅供参考。数学公式、上下标容易出错；建议一次拍一题，对照原图检查并修改。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onVisualRecognize, enabled = !saving && draft.imagePath.isNotBlank(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                Text("视觉 AI 精准识别 · 使用我的 API Key")
            }
            Text("默认使用本机 OCR。点击后才会把原图发送到你配置的视觉模型服务商；无需启用也能保存错题。",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            visualSuggestion?.needsConfirmation?.takeIf(List<String>::isNotEmpty)?.let {
                Text("请重点核对：${it.joinToString("、")}", color = MaterialTheme.colorScheme.error)
            }
            OutlinedTextField(draft.userAnswer, { onEdit { old -> old.copy(userAnswer = it) } }, label = { Text("我当时的答案（可选）") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.correctAnswer, { onEdit { old -> old.copy(correctAnswer = it) } }, label = { Text("正确答案（可选）") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(draft.explanation, { onEdit { old -> old.copy(explanation = it) } }, label = { Text("解析（可选）") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            OutlinedButton(onClick = onSuggest, enabled = !saving && draft.ocrText.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                Icon(Icons.Outlined.AutoAwesome, null); Text(if (saving) "正在整理…" else "AI 给我整理建议")
            }
            suggestion?.let { Text("AI 建议已填入表单，请核对后保存。置信度：${it.confidence?.let { value -> "%.0f%%".format(value * 100) } ?: "未提供"}", color = MaterialTheme.colorScheme.primary) }
            ArchiveListHeading("来源与分类", "留下资料出处，之后更容易找到原题。")
            Field(draft.subject, "科目") { onEdit { old -> old.copy(subject = it) } }
            Field(draft.sourceBook, "书名或资料名") { onEdit { old -> old.copy(sourceBook = it) } }
            Field(draft.sourcePage, "页码（可留空）") { onEdit { old -> old.copy(sourcePage = it) } }
            Field(draft.chapter, "章节") { onEdit { old -> old.copy(chapter = it) } }
            Field(draft.knowledgePoints, "知识点，用顿号分隔") { onEdit { old -> old.copy(knowledgePoints = it) } }
            ArchiveChoiceField(draft.errorType, "错误类型", listOf("概念不清", "公式记错", "计算错误", "审题错误", "方法不会", "粗心", "不确定")) {
                onEdit { old -> old.copy(errorType = it) }
            }
            OutlinedTextField(draft.userNote, { onEdit { old -> old.copy(userNote = it) } }, label = { Text("我的备注") }, minLines = 2, modifier = Modifier.fillMaxWidth())
        } else {
            OutlinedTextField(draft.content, { onEdit { old -> old.copy(content = it) } }, label = { Text("我现在卡在哪里？") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            ArchiveListHeading("资料出处", "记下书名、页码，让之后的跟进有迹可循。")
            Field(draft.sourceBook, "书名或讲义名称") { onEdit { old -> old.copy(sourceBook = it) } }
            Field(draft.sourcePage, "页码（可留空）") { onEdit { old -> old.copy(sourcePage = it) } }
            Field(draft.chapter, "章节") { onEdit { old -> old.copy(chapter = it) } }
            ArchiveChoiceField(draft.doubtType, "疑问类型", listOf("概念理解", "公式推导", "解题步骤", "例题看不懂", "知识点关系", "资料说法不一致")) {
                onEdit { old -> old.copy(doubtType = it) }
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = onSave, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(if (saving) "正在保存…" else "保存到个人档案") }
        if (draft.archiveType == "WRONG") Text("错题保存后默认不安排复习；时间由你自己决定。", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ArchiveChoiceField(value: String, label: String, choices: List<String>, onValue: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Field(value, label, onValue)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            choices.forEach { choice ->
                FilterChip(selected = value == choice, onClick = { onValue(choice) }, label = { Text(choice) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
    }
}

@Composable
private fun Field(value: String, label: String, onValue: (String) -> Unit) {
    OutlinedTextField(value, onValue, label = { Text(label) }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
private fun WrongQuestionDetailScreen(
    padding: PaddingValues,
    record: WrongQuestionRecord,
    reviews: List<String>,
    imageBytes: ByteArray?,
    imageMessage: String?,
    imageLoading: Boolean,
    onRetryImage: () -> Unit,
    onSchedule: (Int?) -> Unit,
    onScheduleAt: (String?) -> Unit,
    onResult: (String, String) -> Unit,
    onUpdate: (UpdateWrongQuestionRequest) -> Unit,
    onMaster: () -> Unit,
    message: String?
) {
    var revealText by rememberSaveable(record.id) { mutableStateOf(false) }
    var note by rememberSaveable(record.id) { mutableStateOf("") }
    var scheduleText by rememberSaveable(record.id) { mutableStateOf(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))) }
    var editing by rememberSaveable(record.id) { mutableStateOf(false) }
    var editBook by rememberSaveable(record.id) { mutableStateOf(record.sourceBook.orEmpty()) }
    var editPage by rememberSaveable(record.id) { mutableStateOf(record.sourcePage.orEmpty()) }
    var editError by rememberSaveable(record.id) { mutableStateOf(record.errorType.orEmpty()) }
    var editPoints by rememberSaveable(record.id) { mutableStateOf(record.knowledgePoints.orEmpty()) }
    var editOcr by rememberSaveable(record.id) { mutableStateOf(record.ocrText.orEmpty()) }
    var editAnswer by rememberSaveable(record.id) { mutableStateOf(record.userAnswer.orEmpty()) }
    var editCorrectAnswer by rememberSaveable(record.id) { mutableStateOf(record.correctAnswer.orEmpty()) }
    var editExplanation by rememberSaveable(record.id) { mutableStateOf(record.explanation.orEmpty()) }
    var customScheduleOpen by rememberSaveable(record.id) { mutableStateOf(false) }
    var scheduleError by rememberSaveable(record.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(record.sourceBook ?: record.subject ?: "考研错题", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                val location = listOfNotNull(record.sourcePage?.let { "第 $it 页" }, record.chapter, record.errorType).filter(String::isNotBlank).joinToString(" · ")
                if (location.isNotBlank()) Text(location, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                ArchiveBadge(wrongStatus(record.reviewStatus))
            }
        }
        StudyArchiveImagePanel(
            bytes = imageBytes,
            loading = imageLoading,
            message = imageMessage,
            onRetry = onRetryImage
        )
        DetailSection(title = "先回忆解题过程") {
            Text("先看原题，试着自己回忆解题过程。", style = MaterialTheme.typography.bodyLarge)
            TextButton(onClick = { revealText = !revealText }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (revealText) "收起题目文字和个人备注" else "展开题目文字和个人备注")
            }
            if (revealText) {
                record.ocrText?.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                record.userAnswer?.takeIf(String::isNotBlank)?.let { Text("我的答案：$it") }
                record.correctAnswer?.takeIf(String::isNotBlank)?.let { Text("正确答案：$it", fontWeight = FontWeight.SemiBold) }
                record.explanation?.takeIf(String::isNotBlank)?.let { Text("解析：$it") }
                record.userNote?.takeIf(String::isNotBlank)?.let { Text("我的备注：$it") }
            }
            TextButton(onClick = { editing = !editing }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (editing) "收起编辑" else "编辑来源和错题信息")
            }
        }
        if (editing) {
            DetailSection(title = "编辑错题信息") {
                Field(editBook, "书名或资料名") { editBook = it }
                Field(editPage, "页码") { editPage = it }
                Field(editError, "错误类型") { editError = it }
                Field(editPoints, "知识点") { editPoints = it }
                OutlinedTextField(editOcr, { editOcr = it }, label = { Text("题目文字") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(editAnswer, { editAnswer = it }, label = { Text("我的答案") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(editCorrectAnswer, { editCorrectAnswer = it }, label = { Text("正确答案") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(editExplanation, { editExplanation = it }, label = { Text("解析") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = {
                    onUpdate(UpdateWrongQuestionRequest(
                        subject = record.subject, ocrText = editOcr, userAnswer = editAnswer,
                        correctAnswer = editCorrectAnswer, explanation = editExplanation,
                        sourceBook = editBook, sourcePage = editPage, chapter = record.chapter,
                        knowledgePoints = editPoints.split(',', '，').map(String::trim).filter(String::isNotBlank),
                        errorType = editError, userNote = record.userNote
                    )); editing = false
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("保存错题信息") }
            }
        }
        DetailSection(title = "复习安排") {
            if (record.reviewStatus == "SCHEDULED") {
                Text("下次复习 · ${record.nextReviewTime.orEmpty().replace('T', ' ').take(16)}", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("当前未安排复习；错题保留在档案中，你可以随时设置时间。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSchedule(1) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("明天") }
                OutlinedButton(onClick = { onSchedule(3) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("3 天后") }
                OutlinedButton(onClick = { onSchedule(7) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("下周") }
            }
            TextButton(onClick = { customScheduleOpen = !customScheduleOpen }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (customScheduleOpen) "收起自定义时间" else "选择其他时间")
            }
            if (customScheduleOpen) {
                OutlinedTextField(scheduleText, { scheduleText = it }, label = { Text("自定义时间（yyyy-MM-ddTHH:mm）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = {
                    runCatching { LocalDateTime.parse(scheduleText).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
                        .onSuccess { scheduleError = false; onScheduleAt(it) }
                        .onFailure { scheduleError = true }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("按自定义时间安排") }
                if (scheduleError) Text("时间格式请使用 yyyy-MM-ddTHH:mm，例如 2026-10-02T09:00", color = MaterialTheme.colorScheme.error)
            }
            if (record.reviewStatus == "SCHEDULED") {
                OutlinedButton(onClick = { onSchedule(null) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null)
                    Text("取消复习安排")
                }
                Text("只清除下次复习时间；错题和历史记录会保留，之后仍可重新安排。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DetailSection(title = "记录这次复盘") {
            OutlinedTextField(note, { note = it }, label = { Text("本次复盘备注") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onResult("CORRECT", note) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("这次答对") }
                OutlinedButton(onClick = { onResult("INCORRECT", note) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("仍然不会") }
            }
            TextButton(onClick = { onResult("VIEWED", note) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("这次只查看，不记录对错") }
            Text("记录本次结果不会自动安排下一次复习。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedButton(onClick = onMaster, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("标记为已掌握") }
        if (reviews.isNotEmpty()) {
            DetailSection(title = "复盘历史") {
                reviews.forEach { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        content()
        HorizontalDivider(Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun StudyArchiveImagePanel(
    bytes: ByteArray?,
    loading: Boolean,
    message: String?,
    onRetry: () -> Unit,
    title: String = "原题照片"
) {
    var decoding by remember(bytes) { mutableStateOf(bytes != null) }
    var bitmap by remember(bytes) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(bytes) {
        // 解码留在 IO 线程；图片变化时清空旧预览，避免切换档案后短暂显示上一题。
        bitmap = withContext(Dispatchers.IO) {
            bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
        }
        decoding = false
    }
    var previewOpen by rememberSaveable(bytes) { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            when {
                bitmap != null -> {
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 340.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable(onClick = { previewOpen = true }),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap!!,
                            contentDescription = "点按查看错题原图大图",
                            modifier = Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 340.dp).padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ) {
                            Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.ZoomIn, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("查看大图", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
                loading || decoding -> Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                }
                else -> {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("暂时无法显示原图", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                            val detail = message ?: if (bytes != null) "这张图片无法解码，请重新加载原图。" else "错题文字和复盘记录仍然保留。"
                            Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = onRetry, enabled = !loading, modifier = Modifier.heightIn(min = 48.dp)) {
                                Text(if (loading) "正在重新加载…" else "重新加载原题")
                            }
                        }
                    }
                }
            }
        }
    }
    if (previewOpen && bitmap != null) {
        StudyImagePreview(bitmap!!, title) { previewOpen = false }
    }
}

@Composable
private fun StudyImagePreview(bitmap: ImageBitmap, title: String, onDismiss: () -> Unit) {
    var scale by remember(bitmap) { mutableFloatStateOf(1f) }
    var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offset = if (scale == 1f) Offset.Zero else offset + panChange
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Box(Modifier.fillMaxSize().transformable(transformState)) {
                Image(
                    bitmap = bitmap,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                    contentScale = ContentScale.Fit
                )
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(48.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭原图预览", tint = Color.White)
                }
                Text("双指缩放 · 拖动查看", modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp), color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun DoubtDetailScreen(
    padding: PaddingValues,
    record: DoubtRecord,
    understandings: List<String>,
    imageBytes: ByteArray?,
    imageMessage: String?,
    aiExplanation: String?,
    saving: Boolean,
    message: String?,
    imageLoading: Boolean,
    onSchedule: (Int?) -> Unit,
    onScheduleAt: (String?) -> Unit,
    onUnderstanding: (String, String) -> Unit,
    onResolve: () -> Unit,
    onExplain: () -> Unit,
    onReopen: () -> Unit,
    onAiFeedback: (String) -> Unit,
    onUpdate: (UpdateDoubtRequest) -> Unit,
    onRetryImage: () -> Unit
) {
    var understanding by rememberSaveable(record.id) { mutableStateOf("") }
    var understandingStatus by rememberSaveable(record.id) { mutableStateOf("INITIAL") }
    var scheduleText by rememberSaveable(record.id) { mutableStateOf(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))) }
    var customScheduleOpen by rememberSaveable(record.id) { mutableStateOf(false) }
    var scheduleError by rememberSaveable(record.id) { mutableStateOf(false) }
    var editing by rememberSaveable(record.id) { mutableStateOf(false) }
    var editContent by rememberSaveable(record.id) { mutableStateOf(record.content) }
    var editBook by rememberSaveable(record.id) { mutableStateOf(record.sourceBook.orEmpty()) }
    var editPage by rememberSaveable(record.id) { mutableStateOf(record.sourcePage.orEmpty()) }
    var editChapter by rememberSaveable(record.id) { mutableStateOf(record.chapter.orEmpty()) }
    var editType by rememberSaveable(record.id) { mutableStateOf(record.doubtType.orEmpty()) }
    Column(Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ArchiveBadge(doubtStatus(record.status))
                Text(record.content, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                val source = listOfNotNull(record.sourceBook, record.sourcePage?.let { "第 $it 页" }, record.chapter, record.doubtType)
                    .filter(String::isNotBlank).joinToString(" · ")
                if (source.isNotBlank()) Text(source, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { editing = !editing }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(if (editing) "收起编辑" else "编辑问题和资料出处")
                }
            }
        }
        if (editing) {
            DetailSection("问题与出处") {
                OutlinedTextField(editContent, { editContent = it }, label = { Text("疑问内容") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Field(editBook, "书名或讲义名称") { editBook = it }
                Field(editPage, "页码") { editPage = it }
                Field(editChapter, "章节") { editChapter = it }
                Field(editType, "疑问类型") { editType = it }
                OutlinedButton(onClick = {
                    onUpdate(UpdateDoubtRequest(content = editContent, sourceBook = editBook, sourcePage = editPage, chapter = editChapter, doubtType = editType))
                    editing = false
                }, modifier = Modifier.fillMaxWidth()) { Text("保存疑问信息") }
            }
        }
        if (!record.imagePath.isNullOrBlank()) {
            StudyArchiveImagePanel(imageBytes, imageLoading, imageMessage, onRetryImage, title = "资料图片")
        }
        DetailSection("留给之后的自己") {
            Text(record.nextProcessTime?.let { "下次处理 · ${it.replace('T', ' ').take(16)}" } ?: "还未安排时间，疑问会一直保留在档案中。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSchedule(1) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("明天处理") }
                OutlinedButton(onClick = { onSchedule(7) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("下周处理") }
            }
            if (record.nextProcessTime != null) {
                TextButton(onClick = { onSchedule(null) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("取消处理安排") }
                Text("仅清除处理时间，问题和理解历史会保留。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = { customScheduleOpen = !customScheduleOpen }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(if (customScheduleOpen) "收起自定义时间" else "选择其他时间")
            }
            if (customScheduleOpen) {
                OutlinedTextField(scheduleText, { scheduleText = it }, label = { Text("自定义处理时间（yyyy-MM-ddTHH:mm）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = {
                    runCatching { LocalDateTime.parse(scheduleText).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
                        .onSuccess { scheduleError = false; onScheduleAt(it) }
                        .onFailure { scheduleError = true }
                }, modifier = Modifier.fillMaxWidth()) { Text("按自定义时间安排") }
                if (scheduleError) Text("时间格式请使用 yyyy-MM-ddTHH:mm，例如 2026-10-02T09:00", color = MaterialTheme.colorScheme.error)
            }
        }
        DetailSection("换一个角度理解") {
            Text("AI 解释仅供参考，是否解决由你确认。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onExplain, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Icon(Icons.Outlined.AutoAwesome, null); Text(if (saving) "正在整理…" else "获取 AI 参考解释") }
            aiExplanation?.let { explanation ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AI 参考 · 由你判断是否解决", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        ReadingBody(explanation)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { onAiFeedback("HELPFUL") }, enabled = !saving) { Text(if (record.aiExplanationFeedback == "HELPFUL") "✓ 有帮助" else "有帮助") }
                            OutlinedButton(onClick = { onAiFeedback("NOT_HELPFUL") }, enabled = !saving) { Text(if (record.aiExplanationFeedback == "NOT_HELPFUL") "✓ 没帮助" else "没帮助") }
                        }
                    }
                }
            }
        }
        DetailSection("理解的足迹 · ${understandings.size}") {
            if (understandings.isEmpty()) Text("还没有理解记录。先写下你现在的想法，之后再回来修订。",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            understandings.forEach { item ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                    val displayText = when {
                        item.startsWith("INITIAL ·") -> item.replaceFirst("INITIAL", "初步理解")
                        item.startsWith("VERIFYING ·") -> item.replaceFirst("VERIFYING", "待验证")
                        item.startsWith("CONFIRMED ·") -> item.replaceFirst("CONFIRMED", "已确认")
                        else -> item
                    }
                    Text(displayText,
                        modifier = Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        DetailSection("追加我的理解") {
            OutlinedTextField(understanding, { understanding = it }, label = { Text("追加一条新的理解") }, minLines = 3, modifier = Modifier.fillMaxWidth())
            listOf("INITIAL" to "初步理解", "VERIFYING" to "待验证", "CONFIRMED" to "已确认").forEach { (status, label) ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .selectable(selected = understandingStatus == status, role = Role.RadioButton, onClick = { understandingStatus = status }).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = understandingStatus == status, onClick = null)
                    Text(label, modifier = Modifier.padding(start = 12.dp))
                }
            }
            Button(onClick = { if (understanding.isNotBlank()) { onUnderstanding(understanding, understandingStatus); understanding = "" } }, enabled = !saving && understanding.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("保存理解历史") }
        }
        if (record.status == "RESOLVED") {
            OutlinedButton(onClick = onReopen, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("重新打开，继续跟进") }
        } else {
            OutlinedButton(onClick = onResolve, enabled = !saving, modifier = Modifier.fillMaxWidth()) { Text("我确认这个疑问已解决") }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
    }
}

private fun wrongStatus(status: String): String = when (status) {
    "SCHEDULED" -> "已安排"
    "MASTERED" -> "已掌握"
    "ARCHIVED" -> "已归档"
    else -> "未安排"
}

private fun doubtStatus(status: String): String = when (status) {
    "UNDERSTOOD" -> "已有理解"
    "VERIFYING" -> "待验证"
    "RESOLVED" -> "已解决"
    else -> "待处理"
}
