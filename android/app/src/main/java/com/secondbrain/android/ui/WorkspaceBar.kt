package com.secondbrain.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WorkspaceBar(onCapture: () -> Unit, viewModel: WorkspaceViewModel = hiltViewModel()) {
    val selection by viewModel.selection.collectAsState()
    val state by viewModel.state.collectAsState()
    val spaces = (state as? LoadState.Content)?.value.orEmpty()
    var open by remember { mutableStateOf(false) }
    val name = if (selection.activeId == null) "个人空间" else spaces.find { it.id == selection.activeId }?.name ?: "协作工作区"
    Row(Modifier.fillMaxWidth().padding(start = 12.dp, end = 16.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { open = true }, modifier = Modifier.weight(1f)) {
            Icon(if (selection.activeId == null) Icons.Outlined.Person else Icons.Outlined.Groups, null, Modifier.size(18.dp))
            Text(if (selection.switching) "正在切换…" else name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 8.dp))
            Icon(Icons.Outlined.ExpandMore, "切换空间", Modifier.size(18.dp))
            Spacer(Modifier.weight(1f))
        }
        IconButton(onClick = onCapture) { Icon(Icons.Outlined.Add, "采集知识") }
    }
    if (open) ModalBottomSheet(onDismissRequest = { open = false }) {
        LazyColumn(contentPadding = PaddingValues(20.dp, 4.dp, 20.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("切换空间", style = MaterialTheme.typography.titleLarge)
                Text("知识与复习计划随当前空间更新", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { SettingsRow("个人空间", "我的知识库", Icons.Outlined.Person, selection.ready && selection.activeId == null, selection.ready && !selection.switching, viewModel::selectPersonal) }
            items(spaces, key = { it.id }) { space ->
                SettingsRow(space.name, space.description ?: "协作知识库", Icons.Outlined.Groups, selection.activeId == space.id, selection.ready && !selection.switching) { viewModel.select(space.id) }
            }
            if (state is LoadState.Loading || selection.switching) item { LoadingContent("正在读取空间…") }
            (state as? LoadState.Failure)?.let { item { RetryContent(it.message, viewModel::load) } }
            selection.message?.let { item { Text(it, style = MaterialTheme.typography.bodyMedium) } }
            item { Button(onClick = { open = false }, modifier = Modifier.fillMaxWidth()) { Text("完成") } }
        }
    }
}
