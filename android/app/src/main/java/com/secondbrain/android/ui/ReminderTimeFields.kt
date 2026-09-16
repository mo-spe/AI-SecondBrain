package com.secondbrain.android.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.secondbrain.android.data.remote.KnowledgeNode
import com.secondbrain.android.reminder.ReminderViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
internal fun ReminderTimeFields(value: String, onChange: (String) -> Unit, enabled: Boolean = true) {
    val context = LocalContext.current
    val dateTime = LocalDateTime.parse(value)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("提醒时间", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(enabled = enabled, modifier = Modifier.weight(1f), onClick = {
                DatePickerDialog(context, { _, year, month, day ->
                    onChange(LocalDateTime.of(year, month + 1, day, dateTime.hour, dateTime.minute).toString())
                }, dateTime.year, dateTime.monthValue - 1, dateTime.dayOfMonth).show()
            }) { Text(dateTime.format(DateTimeFormatter.ofPattern("yyyy/MM/dd"))) }
            OutlinedButton(enabled = enabled, modifier = Modifier.weight(1f), onClick = {
                TimePickerDialog(context, { _, hour, minute -> onChange(dateTime.withHour(hour).withMinute(minute).withSecond(0).withNano(0).toString()) }, dateTime.hour, dateTime.minute, true).show()
            }) { Text(dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))) }
        }
    }
}

@Composable
internal fun KnowledgeReminderDialog(node: KnowledgeNode, onDismiss: () -> Unit, viewModel: ReminderViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    var time by rememberSaveable(node.id) { mutableStateOf(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0).toString()) }
    LaunchedEffect(node.id) { viewModel.prepareEditor() }
    AlertDialog(onDismissRequest = { if (!state.saving) onDismiss() },
        title = { Text("安排一次复习") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(node.title, style = MaterialTheme.typography.bodyLarge)
            ReminderTimeFields(time, { time = it; viewModel.prepareEditor() }, enabled = !state.saving)
            NotificationPermissionHint()
            state.message?.let { Text(it, color = if (state.savedNodeId == node.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(onClick = { viewModel.save(context, node.id, time) }, enabled = !state.saving) { Text(if (state.saving) "正在保存…" else "保存提醒") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !state.saving) { Text(if (state.savedNodeId == node.id) "完成" else "取消") } })
}

@Composable
internal fun NotificationPermissionHint() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    var requested by rememberSaveable { mutableStateOf(false) }
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) {
        enabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        enabled = androidx.core.app.NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }
    if (!enabled) Column {
        Text("本机通知未开启。提醒仍会保存到账号；开启通知后，请重新保存提醒以安排本机通知。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = {
            if (android.os.Build.VERSION.SDK_INT >= 33 && !requested) {
                requested = true
                launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName))
            }
        }) { Text("开启通知") }
    }
}

