package com.secondbrain.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
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
            keyboardActions = KeyboardActions(onSearch = { viewModel.load(query); keyboard?.hide() }))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${listing.total} 个知识点", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 16.dp))
            TextButton(onClick = { viewModel.load(query); keyboard?.hide() }) { Text(if (query.isBlank()) "刷新" else "搜索") }
        }
    }) { nodes ->
        items(nodes, key = { it.id }) { node ->
            KnowledgeRow(node.title, node.summary?.takeIf { it.isNotBlank() } ?: node.contentMd?.take(120) ?: "打开阅读完整内容", "知", onClick = { onOpen(node.id) })
        }
        if (nodes.size < listing.total) item {
            listing.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = viewModel::loadMore, enabled = !listing.loadingMore, modifier = Modifier.fillMaxWidth()) {
                Text(if (listing.loadingMore) "正在读取…" else "加载更多 · 已显示 ${nodes.size} 条")
            }
        }
    }
}
