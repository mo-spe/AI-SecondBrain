package com.secondbrain.android.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

private data class Destination(val label: String, val icon: ImageVector)

private sealed interface FullScreenDestination {
    data object Capture : FullScreenDestination
    data object Rag : FullScreenDestination
    data object Review : FullScreenDestination
    data class Knowledge(val id: Long) : FullScreenDestination
    data class Post(val post: com.secondbrain.android.data.remote.SquarePost) : FullScreenDestination
    data class Question(val question: com.secondbrain.android.data.remote.CommunityQuestion) : FullScreenDestination
}

@Composable
fun SecondBrainApp() {
    val auth: AuthViewModel = hiltViewModel()
    val authState by auth.state.collectAsState()
    if (authState.checking) {
        FeaturePlaceholder("SecondBrain", PaddingValues())
    } else if (!authState.signedIn) {
        LoginScreen(authState.error, auth::login)
    } else {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().safeDrawingPadding()) { AppShell() }
        }
    }
}

@Composable
private fun AppShell() {
    val screenStates = rememberSaveableStateHolder()
    val reviewViewModel: com.secondbrain.android.review.ReviewViewModel = hiltViewModel()
    val destinations = listOf(
        Destination("今日", Icons.Outlined.Home),
        Destination("知识", Icons.Outlined.MenuBook),
        Destination("社区", Icons.Outlined.Groups),
        Destination("我的", Icons.Outlined.Person)
    )
    var selected by rememberSaveable { mutableIntStateOf(0) }
    val destinationSaver = androidx.compose.runtime.saveable.listSaver<FullScreenDestination?, String>(
        save = { destination -> when (destination) {
            FullScreenDestination.Capture -> listOf("capture")
            FullScreenDestination.Rag -> listOf("rag")
            FullScreenDestination.Review -> listOf("review")
            is FullScreenDestination.Knowledge -> listOf("knowledge", destination.id.toString())
            is FullScreenDestination.Post -> listOf("post", destination.post.postId.toString())
            is FullScreenDestination.Question -> listOf("question", destination.question.id.toString())
            null -> emptyList()
        } },
        restore = { route -> when (route.firstOrNull()) {
            "capture" -> FullScreenDestination.Capture
            "rag" -> FullScreenDestination.Rag
            "review" -> FullScreenDestination.Review
            "knowledge" -> FullScreenDestination.Knowledge(route[1].toLong())
            "post" -> FullScreenDestination.Post(com.secondbrain.android.data.remote.SquarePost(route[1].toLong()))
            "question" -> FullScreenDestination.Question(com.secondbrain.android.data.remote.CommunityQuestion(route[1].toLong(), ""))
            else -> null
        } }
    )
    var fullScreen by rememberSaveable(stateSaver = destinationSaver) { androidx.compose.runtime.mutableStateOf<FullScreenDestination?>(null) }
    BackHandler(enabled = fullScreen != null) { fullScreen = null }
    when (val destination = fullScreen) {
        FullScreenDestination.Capture -> {
            CaptureScreen(onBack = { fullScreen = null })
            return
        }
        FullScreenDestination.Rag -> {
            RagScreen(onBack = { fullScreen = null })
            return
        }
        FullScreenDestination.Review -> {
            ReviewSessionScreen(
                onBack = { fullScreen = null },
                onOpenKnowledge = { id -> fullScreen = FullScreenDestination.Knowledge(id) }
            )
            return
        }
        is FullScreenDestination.Knowledge -> {
            val knowledgeViewModel: KnowledgeViewModel = hiltViewModel()
            KnowledgeDetailScreen(destination.id, { fullScreen = null }, knowledgeViewModel::detail)
            return
        }
        is FullScreenDestination.Post -> {
            val communityViewModel: CommunityViewModel = hiltViewModel()
            SquarePostScreen(
                post = destination.post,
                onBack = { fullScreen = null },
                onLike = communityViewModel::toggleLike,
                onBookmark = communityViewModel::toggleBookmark,
                loadDetail = communityViewModel::postDetail
            )
            return
        }
        is FullScreenDestination.Question -> {
            val communityViewModel: CommunityViewModel = hiltViewModel()
            val questionViewModel: QuestionViewModel = hiltViewModel(key = "question-${destination.question.id}")
            QuestionScreen(destination.question.id, { fullScreen = null }, questionViewModel, communityViewModel::loadQuestions)
            return
        }
        null -> Unit
    }
    Scaffold(
        topBar = { WorkspaceBar(onCapture = { fullScreen = FullScreenDestination.Capture }) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                destinations.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
    ) { padding ->
        screenStates.SaveableStateProvider("tab-$selected") {
        when (selected) {
            0 -> TodayScreen(
                padding = padding,
                onOpenRag = { fullScreen = FullScreenDestination.Rag },
                onOpenCapture = { fullScreen = FullScreenDestination.Capture },
                onOpenReview = { cardId -> reviewViewModel.loadFrom(cardId); fullScreen = FullScreenDestination.Review }
            )
            1 -> KnowledgeScreen(padding, onOpen = { fullScreen = FullScreenDestination.Knowledge(it) })
            2 -> CommunityScreen(
                padding = padding,
                onOpenPost = { fullScreen = FullScreenDestination.Post(it) },
                onOpenQuestion = { fullScreen = FullScreenDestination.Question(it) }
            )
            else -> ProfileScreen(padding, onOpenCapture = { fullScreen = FullScreenDestination.Capture })
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureScreen(onBack: () -> Unit, viewModel: com.secondbrain.android.capture.CaptureViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.recognize(context, it) }
    }
    var cameraOpen by remember { androidx.compose.runtime.mutableStateOf(false) }
    var cameraError by remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    if (cameraOpen) {
        com.secondbrain.android.capture.CameraCaptureScreen(
            onCaptured = { uri -> cameraOpen = false; viewModel.recognize(context, uri) },
            onBack = { cameraOpen = false },
            onError = { cameraError = it }
        )
        return
    }
    val draft = state.draft
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(20.dp)) {
        TopAppBar(title = { Text("采集知识") }, navigationIcon = { BackNavigation(onBack) })
        if (draft == null) {
            Text("拍照或从相册导入后，文字将在本机识别。确认前不会上传图片或创建知识点。", modifier = Modifier.padding(top = 24.dp))
            Button(onClick = { picker.launch("image/*") }, enabled = !state.recognizing, modifier = Modifier.fillMaxWidth().padding(top = 20.dp)) {
                Text(if (state.recognizing) "正在识别…" else "从相册选择图片")
            }
            Button(onClick = {
                cameraError = null
                cameraOpen = true
            }, enabled = !state.recognizing, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text("打开相机拍摄")
            }
            OutlinedButton(onClick = viewModel::createManual, enabled = state.ready && !state.recognizing, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("手动输入知识") }
            state.message?.let { Text(it, color = if (it.startsWith("已保存") || it.startsWith("知识已保存")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
            cameraError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        } else {
            val title = draft.title
            val content = draft.content
            Text(if (state.localSaved) "草稿已保存到本机" else if (state.message != null) "草稿尚未保存" else "正在保存草稿…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!state.localSaved && state.message != null) TextButton(onClick = { viewModel.update(title, content) }, enabled = !state.saving) { Text("重试保存草稿") }
            if (draft.workspaceId != state.workspaceId) Text("请切回草稿所属空间后保存", color = MaterialTheme.colorScheme.error)
            OutlinedTextField(title, { viewModel.update(it, content) }, enabled = !state.saving, label = { Text("知识标题") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            OutlinedTextField(content, { viewModel.update(title, it) }, enabled = !state.saving, label = { Text("识别正文，可编辑") }, modifier = Modifier.fillMaxWidth().height(280.dp).padding(top = 12.dp))
            Button(onClick = viewModel::saveConfirmed, enabled = state.ready && !state.saving && draft.workspaceId == state.workspaceId && title.isNotBlank() && content.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text(if (state.saving) "正在保存…" else "确认保存到知识库")
            }
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        }
    }
}

@Composable
private fun LoginScreen(error: String?, onLogin: (String, String) -> Unit) {
    var username by remember { androidx.compose.runtime.mutableStateOf("") }
    var password by remember { androidx.compose.runtime.mutableStateOf("") }
    var passwordVisible by remember { androidx.compose.runtime.mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            BrandMonogram()
            Text(
                "把碎片\n沉淀成体系",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.padding(top = 28.dp)
            )
            Text(
                "在一个安静的地方，继续你的学习脉络。",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp)
            )
            ElevatedCard(
                modifier = Modifier.fillMaxWidth().padding(top = 36.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("进入工作台", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "使用已有的 SecondBrain 账号登录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("用户名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("密码") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            TextButton(onClick = { passwordVisible = !passwordVisible }) {
                                Text(if (passwordVisible) "隐藏" else "显示")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                    )
                    error?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                    Button(
                        onClick = { onLogin(username.trim(), password) },
                        enabled = username.isNotBlank() && password.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(56.dp)
                    ) { Text("登录并继续") }
                }
            }
            Text(
                "你的知识从这里开始保持连贯。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 18.dp)
            )
        }
    }
}

@Composable
private fun BrandMonogram() {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.size(56.dp)
    ) {
        Column(verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Lightbulb, contentDescription = "SecondBrain", modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
private fun BackNavigation(onBack: () -> Unit) {
    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回") }
}

@Composable
private fun FeaturePlaceholder(title: String, padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            "正在连接你的 SecondBrain",
            modifier = Modifier.padding(top = 12.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TodayScreen(
    padding: PaddingValues,
    onOpenRag: () -> Unit,
    onOpenCapture: () -> Unit,
    onOpenReview: (Long?) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text("今日", style = MaterialTheme.typography.displaySmall)
                    Text("从一次回顾开始", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                    Text("积累，", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("让选择更从容。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("—", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        when (val viewState = state) {
            LoadState.Loading -> {
                item { LearningHero(0, null, onReview = {}, reviewEnabled = false) }
                item { LoadingContent("正在更新今日学习计划…") }
            }
            is LoadState.Empty -> {
                item { LearningHero(0, null, onReview = {}, reviewEnabled = false) }
                item { EmptyState(viewState.message, viewModel::load) }
                item { TodayExploreHeader() }
                item { TodayExploreRow("问问知识库", "基于已有资料获得回答", Icons.Outlined.ChatBubbleOutline, onOpenRag) }
                item { TodayExploreRow("采集新知识", "图片、链接或手动记录", Icons.Outlined.Add, onOpenCapture, MaterialTheme.colorScheme.tertiaryContainer) }
            }
            is LoadState.Failure -> item { EmptyState(viewState.message, viewModel::load) }
            is LoadState.Content -> {
                item { LearningHero(viewState.value.size, viewState.value.firstOrNull(), onReview = { onOpenReview(null) }, reviewEnabled = true) }
                item { TodayExploreHeader() }
                item { TodayExploreRow("问问知识库", "基于已有资料获得回答", Icons.Outlined.ChatBubbleOutline, onOpenRag) }
                item { TodayExploreRow("采集新知识", "图片、链接或手动记录", Icons.Outlined.Add, onOpenCapture, MaterialTheme.colorScheme.tertiaryContainer) }
            }
        }
    }
}

@Composable
private fun LearningHero(
    count: Int,
    nextCard: com.secondbrain.android.data.remote.ReviewCard?,
    onReview: () -> Unit,
    reviewEnabled: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box {
            Image(
                painter = painterResource(com.secondbrain.android.R.drawable.today_review_scene),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f), modifier = Modifier.matchParentSize()) {}
        Column(Modifier.padding(24.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("今日待复习", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if (reviewEnabled) "准备开始" else "暂无到期", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Bottom) {
                Text(count.toString(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
                Text(" 张待复习", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
            }
            LinearProgressIndicator(
                progress = { if (reviewEnabled) 0.6f else 0f },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp).height(6.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Text(if (reviewEnabled) "本周学习节奏正在累积" else "去问问你的知识库，继续探索。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            HorizontalDivider(Modifier.padding(top = 20.dp), color = MaterialTheme.colorScheme.outlineVariant)
            Text(if (nextCard != null) "下一张 · 1 / $count" else "暂时没有到期内容", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 18.dp))
            Text(nextCard?.nodeTitle ?: "从一次回顾开始", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 6.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(nextCard?.let { com.secondbrain.android.review.parseReviewPrompt(it.question).question }?.ifBlank { "进入复习，回顾这个知识点" } ?: "去整理新的知识，下一次复习会在这里出现。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Button(
                onClick = onReview,
                enabled = reviewEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp)
            ) { Text("开始复习") }
        }
        }
    }
}

@Composable
private fun TodayExploreHeader() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("继续探索", style = MaterialTheme.typography.titleLarge)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("让好奇心带来更多可能", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun TodayExploreRow(title: String, body: String, icon: ImageVector, onClick: () -> Unit, accent: Color = Color.Unspecified) {
    val tileColor = if (accent == Color.Unspecified) MaterialTheme.colorScheme.primaryContainer else accent
    val iconColor = if (accent == Color.Unspecified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 84.dp),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = tileColor, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(52.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconColor) }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = MaterialTheme.colorScheme.outline)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RagScreen(onBack: () -> Unit, viewModel: com.secondbrain.android.rag.RagViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("问问知识库", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { BackNavigation(onBack) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Text("回答基于你的知识库", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = state.question,
                            enabled = !state.asking,
                            onValueChange = viewModel::updateQuestion,
                            placeholder = { Text("继续追问，或输入新的问题") },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        Button(
                            onClick = if (state.asking) viewModel::stop else viewModel::ask,
                            enabled = state.asking || (state.ready && state.question.isNotBlank()),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier.size(56.dp)
                        ) {
                            if (state.asking) Text("×", style = MaterialTheme.typography.titleLarge)
                            else Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "发送")
                        }
                    }
                }
            }
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
            contentPadding = PaddingValues(start = 20.dp, top = 12.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text("基于个人空间 · 从已有资料中得到可追溯的回答", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.question.takeIf { it.isNotBlank() && (state.asking || state.answer.isNotBlank()) }?.let { question ->
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(0.88f),
                            shape = RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(question, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
            if (state.asking && state.answer.isBlank()) item { LoadingContent("正在检索相关知识…") }
            state.error?.let { error ->
                item {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text("这次问答没有完成", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 4.dp))
                            Text("已收到的内容会保留，可继续提问。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 10.dp))
                        }
                    }
                }
            }
            state.answer.takeIf { it.isNotBlank() }?.let { answer ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("回答", style = MaterialTheme.typography.titleLarge)
                        ReadingBody(answer)
                    }
                }
            }
            state.references?.let { references -> item { RagReferences(references) } }
        }
    }
}

@Composable
private fun CommunityScreen(
    padding: PaddingValues,
    onOpenPost: (com.secondbrain.android.data.remote.SquarePost) -> Unit,
    onOpenQuestion: (com.secondbrain.android.data.remote.CommunityQuestion) -> Unit,
    viewModel: CommunityViewModel = hiltViewModel()
) {
    val square by viewModel.square.collectAsState()
    val questions by viewModel.questions.collectAsState()
    CommunityContent(padding, square, questions, viewModel::loadSquare, viewModel::loadQuestions, onOpenPost, onOpenQuestion)
}

/** 切换时隔离两种列表的组合状态，避免旧条目被新类型的索引回调读取。 */
@Composable
internal fun CommunityContent(
    padding: PaddingValues,
    square: LoadState<List<com.secondbrain.android.data.remote.SquarePost>>,
    questions: LoadState<List<com.secondbrain.android.data.remote.CommunityQuestion>>,
    onRetrySquare: () -> Unit,
    onRetryQuestions: () -> Unit,
    onOpenPost: (com.secondbrain.android.data.remote.SquarePost) -> Unit,
    onOpenQuestion: (com.secondbrain.android.data.remote.CommunityQuestion) -> Unit
) {
    var questionMode by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val showingQuestions = questionMode
    val tabs: @Composable () -> Unit = {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(4.dp)) {
                CommunityTab("知识广场", !showingQuestions, Modifier.weight(1f)) { questionMode = false }
                CommunityTab("问答社区", showingQuestions, Modifier.weight(1f)) { questionMode = true }
            }
        }
    }
    androidx.compose.runtime.key(showingQuestions) {
        if (showingQuestions) {
            ContentScreen("社区", "把问题说清楚，和懂的人一起解开", padding, questions, onRetryQuestions, header = tabs) { content ->
                item { CommunitySectionHeader("正在讨论", onRetryQuestions) }
                items(content, key = { "question-${it.id}" }) { question ->
                    CommunityQuestionRow(question, onClick = { onOpenQuestion(question) })
                }
            }
        } else {
            ContentScreen("社区", "分享能被保存的理解", padding, square, onRetrySquare, header = tabs) { content ->
                item { CommunitySectionHeader("最新分享", onRetrySquare) }
                items(content, key = { "post-${it.postId}" }) { post ->
                    CommunityPostRow(post, onClick = { onOpenPost(post) })
                }
            }
        }
    }
}

@Composable
private fun CommunitySectionHeader(title: String, onRefresh: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onRefresh, modifier = Modifier.heightIn(min = 44.dp)) { Text("刷新") }
    }
}

@Composable
private fun CommunityQuestionRow(
    question: com.secondbrain.android.data.remote.CommunityQuestion,
    onClick: () -> Unit
) {
    val author = question.authorName?.takeIf { it.isNotBlank() } ?: "社区成员"
    val answered = question.answerCount > 0
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (answered) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        if (answered) "${question.answerCount} 个回答" else "待回答",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (answered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                Icon(Icons.Outlined.ChevronRight, contentDescription = "查看问题", tint = MaterialTheme.colorScheme.outline)
            }
            Text(question.title, style = MaterialTheme.typography.titleLarge, maxLines = 3, overflow = TextOverflow.Ellipsis)
            question.content?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("$author · ${if (answered) "已有回答" else "等待理解"}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onClick, modifier = Modifier.heightIn(min = 40.dp), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)) {
                    Text("查看问题")
                }
            }
            question.tags?.takeIf { it.isNotEmpty() }?.let { tags ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.take(3).forEach { tag ->
                        Text("# $tag", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun CommunityPostRow(
    post: com.secondbrain.android.data.remote.SquarePost,
    onClick: () -> Unit
) {
    val author = post.authorName?.takeIf { it.isNotBlank() } ?: "知识贡献者"
    val summary = post.recommendText?.takeIf { it.isNotBlank() }
        ?: post.nodeSummary?.takeIf { it.isNotBlank() }
        ?: "打开分享，查看完整知识内容。"
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(author.take(1), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(author, style = MaterialTheme.typography.labelLarge)
                    Text(
                        post.createdAt?.take(10) ?: "知识分享",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Text("分享", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            Text(post.nodeTitle ?: "知识分享", style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(summary, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            post.knowledgeNodes?.firstOrNull()?.let { node ->
                Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(36.dp)) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
                        }
                        Column(Modifier.weight(1f)) {
                            Text(node.title ?: "关联知识", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("打开查看原知识", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${post.commentCount} 条讨论 · ${post.likeCount} 人赞同", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.heightIn(min = 40.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text("查看讨论") }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun CommunityTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(0.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) { Text(label) }
    } else {
        TextButton(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) { Text(label) }
    }
}

@Composable
private fun ProfileScreen(
    padding: PaddingValues,
    onOpenCapture: () -> Unit,
    viewModel: WorkspaceViewModel = hiltViewModel()
) {
    var remindersOpen by remember { androidx.compose.runtime.mutableStateOf(false) }
    var draftsOpen by remember { androidx.compose.runtime.mutableStateOf(false) }
    BackHandler(enabled = remindersOpen || draftsOpen) { remindersOpen = false; draftsOpen = false }
    if (remindersOpen) {
        ReminderSettingsScreen(onBack = { remindersOpen = false })
        return
    }
    if (draftsOpen) {
        DraftRecoveryScreen(onBack = { draftsOpen = false }, onOpenCapture = onOpenCapture)
        return
    }
    WorkspaceProfile(padding, viewModel, onReminders = { remindersOpen = true }, onDrafts = { draftsOpen = true })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DraftRecoveryScreen(
    onBack: () -> Unit,
    onOpenCapture: () -> Unit,
    viewModel: com.secondbrain.android.capture.CaptureViewModel = hiltViewModel()
) {
    val drafts by viewModel.savedDrafts.collectAsState(initial = emptyList())
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TopAppBar(title = { Text("采集草稿") }, navigationIcon = { BackNavigation(onBack) }) }
        if (drafts.isEmpty()) {
            item { EmptyState("没有未确认的采集草稿。", onBack) }
        }
        items(drafts, key = { it.id }) { draft ->
            Card {
                Column(Modifier.padding(20.dp)) {
                    Text(draft.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        draft.content.take(100).ifBlank { "暂无识别正文" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(
                        onClick = { viewModel.restore(draft); onOpenCapture() },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) { Text("继续编辑") }
                    Button(
                        onClick = { viewModel.discard(draft.id) },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) { Text("删除草稿") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderSettingsScreen(onBack: () -> Unit, viewModel: com.secondbrain.android.reminder.ReminderViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load() }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TopAppBar(title = { Text("复习提醒") }, navigationIcon = { BackNavigation(onBack) }) }
        item {
            Text("让知识在合适的时间回来", style = MaterialTheme.typography.titleMedium)
            Text("打开一条知识，在详情中点击「安排复习提醒」，选择日期与时间。已设置的提醒会显示在这里。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            NotificationPermissionHint()
        }
        state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        if (state.reminders.isEmpty()) {
            item { Text("尚未设置指定提醒。") }
        }
        items(state.reminders, key = { it.nodeId }) { reminder ->
            Card {
                Column(Modifier.padding(20.dp)) {
                    Text("知识点 #" + reminder.nodeId, style = MaterialTheme.typography.titleMedium)
                    Text("计划时间：" + reminder.scheduledAt.replace('T', ' ').take(16), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    Button(onClick = { viewModel.cancel(context, reminder.nodeId) }, modifier = Modifier.padding(top = 12.dp)) { Text("取消提醒") }
                }
            }
        }
    }
}

@Composable
internal fun <T> ContentScreen(
    title: String,
    subtitle: String,
    padding: PaddingValues,
    state: LoadState<List<T>>,
    onRetry: () -> Unit,
    header: @Composable () -> Unit = {},
    rows: androidx.compose.foundation.lazy.LazyListScope.(List<T>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, top = 28.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        }
        item { header() }
        when (state) {
            LoadState.Loading -> item { LoadingContent("正在读取你的数据…") }
            is LoadState.Empty -> item { EmptyState(state.message, onRetry) }
            is LoadState.Failure -> item { EmptyState(state.message, onRetry) }
            is LoadState.Content -> rows(state.value)
        }
    }
}

@Composable
private fun EmptyState(message: String, retry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
            }
            Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
            OutlinedButton(onClick = retry, modifier = Modifier.padding(top = 16.dp).heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp)) { Text("重新读取") }
        }
    }
}

@Composable
private fun InsightCard(title: String, body: String, onClick: (() -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
    )
}

@Composable
internal fun KnowledgeRow(title: String, body: String, marker: String, suffix: String = "", onClick: (() -> Unit)? = null) {
    val content: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(marker, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                if (suffix.isNotBlank()) Text(suffix, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
            }
            if (onClick != null) Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        }
    }
    if (onClick == null) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), content = content)
    } else {
        Surface(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), content = content)
    }
}

@Composable
internal fun SettingsRow(title: String, body: String, icon: ImageVector, selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth().heightIn(min = 76.dp), shape = RoundedCornerShape(18.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Surface(color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp)) }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(if (selected) "当前使用" else body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            Icon(if (selected) Icons.Outlined.Check else Icons.Outlined.ChevronRight, contentDescription = if (selected) "当前空间" else null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
