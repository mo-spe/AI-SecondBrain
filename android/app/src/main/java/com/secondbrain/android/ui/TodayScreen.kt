package com.secondbrain.android.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.secondbrain.android.R
import com.secondbrain.android.data.remote.ReviewCard

@Composable
internal fun TodayScreen(
    padding: PaddingValues,
    onOpenRag: () -> Unit,
    onOpenCapture: () -> Unit,
    onOpenStudyArchive: () -> Unit,
    onOpenVocabulary: () -> Unit,
    onOpenReview: (Long?) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    TodayContent(padding, state, viewModel::load, onOpenRag, onOpenCapture,
        onOpenStudyArchive, onOpenVocabulary, { onOpenReview(null) })
}

@Composable
internal fun TodayContent(
    padding: PaddingValues,
    state: LoadState<List<ReviewCard>>,
    onRetry: () -> Unit,
    onOpenRag: () -> Unit,
    onOpenCapture: () -> Unit,
    onOpenStudyArchive: () -> Unit,
    onOpenVocabulary: () -> Unit,
    onOpenReview: () -> Unit
) {
    LazyColumn(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(padding),
        contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("今日", style = MaterialTheme.typography.headlineMedium)
            Text("从一次回顾开始", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }
        item { LearningHero((state as? LoadState.Content)?.value.orEmpty(), state == LoadState.Loading, onOpenReview) }
        when (state) {
            LoadState.Loading -> item { LoadingContent("正在更新今日学习计划…") }
            is LoadState.Failure -> item { RetryContent(state.message, onRetry) }
            is LoadState.Empty -> item { Text(state.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            is LoadState.Content -> Unit
        }
        item { Text("继续探索", style = MaterialTheme.typography.titleLarge) }
        item {
            val tiles = listOf(
                ExploreAction("问问知识库", "基于资料找到答案", Icons.Outlined.ChatBubbleOutline, onOpenRag),
                ExploreAction("采集新知识", "拍照、相册、手动", Icons.Outlined.Add, onOpenCapture),
                ExploreAction("错题与疑问", "留住原题与思路", Icons.Outlined.Description, onOpenStudyArchive),
                ExploreAction("词表文章", "在语境里重逢生词", Icons.Outlined.MenuBook, onOpenVocabulary)
            )
            val fontScale = LocalDensity.current.fontScale
            BoxWithConstraints {
                // 大字号和窄屏改用单列，保留完整入口名称和足够的触控面积。
                val columns = if (maxWidth < 300.dp || fontScale > 1.3f) 1 else 2
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    tiles.chunked(columns).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { action -> ExploreTile(action, Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LearningHero(cards: List<ReviewCard>, loading: Boolean, onReview: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("今日待复习", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 8.dp)) {
                        Text(if (loading) "—" else cards.size.toString(), style = MaterialTheme.typography.displaySmall)
                        Text(" 张", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(bottom = 5.dp))
                    }
                }
                Image(painterResource(R.drawable.knowledge_papers), contentDescription = null, modifier = Modifier.size(64.dp))
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
            Text(if (cards.isEmpty()) if (loading) "正在读取计划" else "暂时没有到期内容" else "下一张 · 1 / ${cards.size}",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(cards.firstOrNull()?.nodeTitle ?: "让知识慢慢成为自己的",
                style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp))
            Button(onClick = onReview, enabled = cards.isNotEmpty() && !loading,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) {
                Text(if (loading) "正在读取…" else if (cards.isEmpty()) "暂无待复习" else "开始复习")
            }
        }
    }
}

private data class ExploreAction(val title: String, val description: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun ExploreTile(action: ExploreAction, modifier: Modifier) {
    Surface(onClick = action.onClick, modifier = modifier.heightIn(min = 112.dp),
        color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(action.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(action.title, style = MaterialTheme.typography.titleMedium)
            Text(action.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
