package com.secondbrain.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.secondbrain.android.data.remote.KnowledgeNode
import com.secondbrain.android.data.remote.SquarePost
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingScaffold(title: String, onBack: () -> Unit, content: @Composable (PaddingValues) -> Unit) {
    var largeText by rememberSaveable { mutableStateOf(false) }
    val base = MaterialTheme.typography
    val readingType = if (largeText) base.copy(bodyLarge = base.bodyLarge.copy(fontSize = base.bodyLarge.fontSize * 1.2f, lineHeight = base.bodyLarge.lineHeight * 1.2f)) else base
    MaterialTheme(typography = readingType) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text(title, style = MaterialTheme.typography.titleMedium) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "返回") } },
            actions = { TextButton(onClick = { largeText = !largeText }, modifier = Modifier.semantics { contentDescription = "切换阅读字号" }) { Text(if (largeText) "标准字号" else "大字阅读", style = MaterialTheme.typography.labelSmall) } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background))
    }, content = content)
    }
}

@Composable
internal fun KnowledgeDetailScreen(id: Long, onBack: () -> Unit, loadDetail: suspend (Long) -> KnowledgeNode) {
    var state by remember(id) { mutableStateOf<LoadState<KnowledgeNode>>(LoadState.Loading) }
    var retry by remember(id) { mutableIntStateOf(0) }
    LaunchedEffect(id, retry) {
        state = LoadState.Loading
        try { state = LoadState.Content(loadDetail(id)) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { state = LoadState.Failure(error.message ?: "知识详情加载失败") }
    }
    ReadingScaffold("知识详情", onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            when (val value = state) {
                LoadState.Loading -> item { LoadingContent("正在打开知识…") }
                is LoadState.Failure -> item { RetryContent(value.message) { retry++ } }
                is LoadState.Content -> {
                    item {
                        Text("知识库 / 阅读", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(value.value.title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 12.dp))
                    }
                    value.value.summary?.takeIf { it.isNotBlank() }?.let { summary -> item { Text(summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    item { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant) }
                    item { ReadingBody(value.value.contentMd?.takeIf { it.isNotBlank() } ?: "这条知识暂未添加正文。") }
                }
                is LoadState.Empty -> item { Text(value.message) }
            }
        }
    }
}

@Composable
internal fun SquarePostScreen(post: SquarePost, onBack: () -> Unit, onLike: suspend (Long) -> Boolean, onBookmark: suspend (Long) -> Boolean, loadDetail: suspend (Long) -> SquarePost) {
    var detail by remember(post.postId) { mutableStateOf(post) }
    var loading by remember(post.postId) { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(post.postId, retry) {
        loading = true
        error = null
        try { detail = loadDetail(post.postId) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { error = failure.message ?: "分享加载失败" }
        finally { loading = false }
    }
    fun react(like: Boolean) {
        if (busy) return
        busy = true
        error = null
        scope.launch {
            try {
                if (like) {
                    val liked = onLike(detail.postId)
                    detail = detail.copy(isLiked = liked, likeCount = (detail.likeCount + (if (liked) 1 else 0) - (if (detail.isLiked) 1 else 0)).coerceAtLeast(0))
                } else {
                    val saved = onBookmark(detail.postId)
                    detail = detail.copy(isBookmarked = saved, bookmarkCount = (detail.bookmarkCount + (if (saved) 1 else 0) - (if (detail.isBookmarked) 1 else 0)).coerceAtLeast(0))
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.message ?: "操作失败，请重试" }
            finally { busy = false }
        }
    }
    ReadingScaffold("知识分享", onBack) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text((detail.authorName ?: "知").take(1), style = MaterialTheme.typography.titleMedium) }
                    }
                    Column {
                        Text(detail.authorName ?: "知识贡献者", style = MaterialTheme.typography.titleMedium)
                        Text(detail.createdAt?.replace('T', ' ')?.take(16) ?: "分享于知识广场", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item { Text(detail.nodeTitle ?: "知识分享", style = MaterialTheme.typography.headlineMedium) }
            detail.recommendText?.takeIf { it.isNotBlank() }?.let { text -> item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("分享者的话", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            } }
            if (loading) item { LoadingContent("正在读取分享正文…") }
            error?.let { message -> item { RetryContent(message) { retry++ } } }
            items(detail.knowledgeNodes.orEmpty(), key = { "shared-node-${it.nodeId}" }) { node ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (detail.knowledgeNodes.orEmpty().size > 1) Text(node.title ?: "关联知识", style = MaterialTheme.typography.titleLarge)
                    ReadingBody(node.contentMd?.takeIf { it.isNotBlank() } ?: node.summary ?: "这条知识暂未提供正文。")
                }
            }
            if (!loading && detail.knowledgeNodes.isNullOrEmpty()) item { ReadingBody(detail.nodeSummary ?: "分享者暂未附上知识正文。") }
            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(selected = detail.isLiked, enabled = !busy && !loading, onClick = { react(true) }, label = { Text("${if (detail.isLiked) "已赞" else "点赞"} ${detail.likeCount}") }, leadingIcon = { Icon(Icons.Outlined.ThumbUp, null, Modifier.size(18.dp)) }, modifier = Modifier.heightIn(min = 48.dp))
                    FilterChip(selected = detail.isBookmarked, enabled = !busy && !loading, onClick = { react(false) }, label = { Text("${if (detail.isBookmarked) "已收藏" else "收藏"} ${detail.bookmarkCount}") }, leadingIcon = { Icon(Icons.Outlined.BookmarkBorder, null, Modifier.size(18.dp)) }, modifier = Modifier.heightIn(min = 48.dp))
                }
                if (busy) Text("正在更新…", style = MaterialTheme.typography.labelSmall)
            }
            item { Text("讨论 · ${detail.commentCount}", style = MaterialTheme.typography.titleLarge) }
            if (!loading && detail.comments.isNullOrEmpty()) item { Text("还没有评论", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(detail.comments.orEmpty(), key = { "comment-${it.id}" }) { comment ->
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(comment.username ?: "知识读者", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                    Text(comment.content, style = MaterialTheme.typography.bodyLarge)
                    comment.createdAt?.let { Text(it.replace('T', ' ').take(16), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    HorizontalDivider(Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
internal fun WorkspaceProfile(padding: PaddingValues, viewModel: WorkspaceViewModel, onReminders: () -> Unit, onDrafts: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val selection by viewModel.selection.collectAsState()
    val workspaces = (state as? LoadState.Content)?.value.orEmpty()
    val spaceName = if (selection.activeId == null) "个人空间" else workspaces.find { it.id == selection.activeId }?.name ?: "协作工作区"
    LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("我的", style = MaterialTheme.typography.headlineMedium)
            Text("学习有自己的节奏", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
        item {
            Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(if (selection.activeId == null) Icons.Outlined.Person else Icons.Outlined.Groups, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Column(Modifier.weight(1f)) {
                        Text("当前空间", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (selection.ready) spaceName else "正在读取…", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
        item { SectionHeading("学习管理") }
        item { SettingsRow("复习提醒", "管理知识回顾的时间", Icons.Outlined.AutoAwesome, onClick = onReminders) }
        item { SettingsRow("采集草稿", "继续整理尚未保存的内容", Icons.Outlined.Collections, onClick = onDrafts) }
        item { SectionHeading("切换空间") }
        item { SettingsRow("个人空间", "只属于自己的知识库", Icons.Outlined.Person, selected = selection.ready && selection.activeId == null, enabled = selection.ready && !selection.switching, onClick = viewModel::selectPersonal) }
        if (selection.switching) item { LoadingContent("正在切换空间…") }
        selection.message?.let { message -> item { Text(message, style = MaterialTheme.typography.bodyMedium) } }
        when (val value = state) {
            LoadState.Loading -> item { LoadingContent("正在读取协作工作区…") }
            is LoadState.Failure -> item { RetryContent(value.message, viewModel::load) }
            else -> if (workspaces.isEmpty()) item { Text("尚未加入协作工作区，仍可使用个人空间。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        items(workspaces, key = { it.id }) { workspace ->
            SettingsRow(workspace.name, workspace.description?.takeIf { it.isNotBlank() } ?: "协作知识库", Icons.Outlined.Groups,
                selected = selection.activeId == workspace.id, enabled = selection.ready && !selection.switching,
                onClick = { viewModel.select(workspace.id) })
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
}

@Composable
internal fun LoadingContent(message: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(message, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
internal fun RetryContent(message: String, retry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = retry) { Text("重新加载") }
    }
}

/** 使用原生可选择文本阅读正文，代码块保留空白，正文不会执行嵌入的 HTML 或脚本。 */
@Composable
internal fun ReadingBody(markdown: String) {
    val blocks = remember(markdown) { markdown.split("```") }
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            blocks.forEachIndexed { index, block ->
                if (index % 2 == 1) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp)) {
                        Text(block.substringAfter('\n', block).trimEnd(), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), modifier = Modifier.fillMaxWidth().padding(16.dp))
                    }
                } else block.trim().split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() }.forEach { paragraph ->
                    val heading = paragraph.startsWith("#")
                    Text(paragraph.replace(Regex("(?m)^#{1,6}\\s+"), ""), style = if (heading) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
