package com.secondbrain.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
internal fun KnowledgeScreen(padding: PaddingValues, onOpen: (Long) -> Unit, viewModel: KnowledgeViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val listing by viewModel.listing.collectAsState()
    var query by rememberSaveable { mutableStateOf(listing.keyword.orEmpty()) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(listing.keyword) { query = listing.keyword.orEmpty() }
    ContentScreen("知识库", "收藏、理解，然后成为自己的知识", padding, state, { viewModel.load() }, header = {
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            placeholder = { Text("搜索知识") }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = ""; viewModel.load(null) }) { Icon(Icons.Outlined.Close, "清除搜索") } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.load(query); keyboard?.hide() }),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp)
        )
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("${listing.total} 个知识点", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { viewModel.load(query); keyboard?.hide() }) { Text(if (query.isBlank()) "刷新" else "搜索") }
        }
    }) { nodes ->
        items(nodes, key = { it.id }) { node ->
            KnowledgeLibraryRow(node, onClick = { onOpen(node.id) })
        }
        if (nodes.size < listing.total) item {
            listing.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = viewModel::loadMore, enabled = !listing.loadingMore, modifier = Modifier.fillMaxWidth()) {
                Text(if (listing.loadingMore) "正在读取…" else "加载更多 · 已显示 ${nodes.size} 条")
            }
        }
    }
}

@Composable
private fun KnowledgeLibraryRow(
    node: com.secondbrain.android.data.remote.KnowledgeNode,
    onClick: () -> Unit
) {
    val summary = node.summary?.takeIf { it.isNotBlank() }
        ?: node.contentMd?.takeIf { it.isNotBlank() }
        ?: "打开阅读完整内容。"
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Top) {
                Surface(
                    modifier = Modifier.size(44.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(node.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 12.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp)) {
                    Text(
                        if (node.needReview == 1) "待复习" else "知识笔记",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                node.importance?.let { importance ->
                    Text("重要度 $importance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                Text(
                    if (node.needReview == 1) "回顾这条知识，巩固记忆" else "已收录到当前知识库",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
