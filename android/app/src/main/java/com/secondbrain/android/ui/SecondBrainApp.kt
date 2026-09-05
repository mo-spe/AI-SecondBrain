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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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
        Destination("今日", Icons.Outlined.AutoAwesome),
        Destination("知识", Icons.Outlined.Lightbulb),
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
            ReviewSessionScreen(onBack = { fullScreen = null })
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
                tonalElevation = 0.dp
            ) {
                destinations.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = { selected = index },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
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
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
            cameraError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
        } else {
            var title by remember(draft.id) { androidx.compose.runtime.mutableStateOf(draft.title) }
            var content by remember(draft.id) { androidx.compose.runtime.mutableStateOf(draft.content) }
            OutlinedTextField(title, { title = it; viewModel.update(title, content) }, label = { Text("知识标题") }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
            OutlinedTextField(content, { content = it; viewModel.update(title, content) }, label = { Text("识别正文，可编辑") }, modifier = Modifier.fillMaxWidth().height(280.dp).padding(top = 12.dp))
            Button(onClick = viewModel::saveConfirmed, enabled = !state.saving && title.isNotBlank() && content.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
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
    onOpenReview: (Long?) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load() }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("今日", style = MaterialTheme.typography.headlineMedium)
            Text("从一次回顾开始", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
        }
        when (val viewState = state) {
            LoadState.Loading -> item { LearningHero(0, onReview = {}, reviewEnabled = false, onAsk = onOpenRag) }
            is LoadState.Empty -> item {
                LearningHero(0, onReview = {}, reviewEnabled = false, onAsk = onOpenRag)
                EmptyState(viewState.message, viewModel::load)
            }
            is LoadState.Failure -> item { EmptyState(viewState.message, viewModel::load) }
            is LoadState.Content -> {
                item { LearningHero(viewState.value.size, onReview = { onOpenReview(null) }, reviewEnabled = true, onAsk = onOpenRag) }
                item { SectionLabel("等待回顾") }
                items(viewState.value, key = { it.id }) { card ->
                    KnowledgeRow(
                        title = card.nodeTitle ?: "未命名知识点",
                        body = com.secondbrain.android.review.parseReviewPrompt(card.question).question.ifBlank { "进入复习，回顾这个知识点" },
                        marker = "复",
                        onClick = { onOpenReview(card.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LearningHero(count: Int, onReview: () -> Unit, reviewEnabled: Boolean, onAsk: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Column(Modifier.padding(24.dp)) {
            Text("今日待复习", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primaryContainer)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                Text(count.toString(), style = MaterialTheme.typography.displaySmall)
                Text(" 张待复习", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp, bottom = 7.dp))
            }
            Text(
                if (reviewEnabled) "从一张卡片开始，把记忆重新接上。" else "暂时没有到期内容，去问问你的知识库。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.padding(top = 14.dp)
            )
            Button(
                onClick = onReview,
                enabled = reviewEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp).height(52.dp)
            ) { Text("开始复习") }
            TextButton(onClick = onAsk, modifier = Modifier.align(Alignment.End).padding(top = 8.dp)) {
                Text("向知识库提问", color = MaterialTheme.colorScheme.primaryContainer)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RagScreen(onBack: () -> Unit, viewModel: com.secondbrain.android.rag.RagViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { TopAppBar(title = { Text("知识问答") }, navigationIcon = { BackNavigation(onBack) }) }
        item {
            Text(
                "基于当前工作区的知识内容回答；回答会附带可追溯的引用。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            OutlinedTextField(
                value = state.question,
                onValueChange = viewModel::updateQuestion,
                label = { Text("你想了解什么？") },
                placeholder = { Text("例如：Redis 的持久化机制有什么差别？") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Button(
                onClick = viewModel::ask,
                enabled = !state.asking,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) { Text(if (state.asking) "正在检索知识…" else "开始问答") }
        }
        state.error?.let { error ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("这次问答没有完成", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 4.dp))
                        Text("检查服务端模型配置或稍后重试。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
        }
        state.answer.takeIf { it.isNotBlank() }?.let { answer -> item { InsightCard("回答", answer) } }
        state.references?.let { references -> item { InsightCard("引用知识", references) } }
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
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(4.dp)) {
                CommunityTab("知识广场", !showingQuestions, Modifier.weight(1f)) { questionMode = false }
                CommunityTab("问答社区", showingQuestions, Modifier.weight(1f)) { questionMode = true }
            }
        }
    }
    androidx.compose.runtime.key(showingQuestions) {
        if (showingQuestions) {
            ContentScreen("社区", "发现值得保存的知识与观点", padding, questions, onRetryQuestions, header = tabs) { content ->
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { SectionLabel("正在讨论"); TextButton(onClick = onRetryQuestions) { Text("刷新") } } }
                items(content, key = { "question-${it.id}" }) { question ->
                    KnowledgeRow(
                        title = question.title,
                        body = question.content ?: "提问者暂未补充描述",
                        marker = "问",
                        suffix = "${question.authorName ?: "社区成员"} · ${question.answerCount} 个回答",
                        onClick = { onOpenQuestion(question) }
                    )
                }
            }
        } else {
            ContentScreen("社区", "发现值得保存的知识与观点", padding, square, onRetrySquare, header = tabs) { content ->
                item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { SectionLabel("最新分享"); TextButton(onClick = onRetrySquare) { Text("刷新") } } }
                items(content, key = { "post-${it.postId}" }) { post ->
                    KnowledgeRow(
                        title = post.nodeTitle ?: "知识分享",
                        body = post.recommendText ?: post.nodeSummary ?: "阅读分享内容",
                        marker = (post.authorName ?: "知").take(1),
                        suffix = "${post.authorName ?: "知识贡献者"} · ${post.likeCount} 赞 · ${post.commentCount} 条讨论",
                        onClick = { onOpenPost(post) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityTab(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier.height(48.dp),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(0.dp)
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
    var nodeIdText by remember { androidx.compose.runtime.mutableStateOf("") }
    var timeText by remember { androidx.compose.runtime.mutableStateOf("") }
    androidx.compose.runtime.LaunchedEffect(Unit) { viewModel.load() }
    LazyColumn(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TopAppBar(title = { Text("复习提醒") }, navigationIcon = { BackNavigation(onBack) }) }
        item {
            Text("设置或更新提醒", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(nodeIdText, { nodeIdText = it }, label = { Text("知识点 ID") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true)
            OutlinedTextField(timeText, { timeText = it }, label = { Text("提醒时间，如 2026-09-06T09:00:00") }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), singleLine = true)
            Button(
                onClick = { nodeIdText.toLongOrNull()?.let { viewModel.save(context, it, timeText) } },
                enabled = nodeIdText.toLongOrNull() != null && timeText.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) { Text("保存提醒") }
        }
        state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
        if (state.reminders.isEmpty()) {
            item { Text("尚未设置指定提醒。") }
        }
        items(state.reminders, key = { it.nodeId }) { reminder ->
            Card {
                Column(Modifier.padding(20.dp)) {
                    Text("知识点 #" + reminder.nodeId, style = MaterialTheme.typography.titleMedium)
                    Text("计划时间：" + reminder.scheduledAt, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
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
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
        }
        item { header() }
        when (state) {
            LoadState.Loading -> item { Text("正在读取你的数据…") }
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Text(message, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 12.dp))
            OutlinedButton(onClick = retry, modifier = Modifier.padding(top = 16.dp)) { Text("重新读取") }
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
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(marker, style = MaterialTheme.typography.labelLarge) }
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                if (suffix.isNotBlank()) Text(suffix, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary, modifier = Modifier.padding(top = 10.dp))
            }
            if (onClick != null) Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
        }
    }
    if (onClick == null) {
        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface, content = content)
    } else {
        Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surface, content = content)
    }
}

@Composable
internal fun SettingsRow(title: String, body: String, icon: ImageVector, selected: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surface,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(if (selected) "当前使用" else body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
            }
            Icon(if (selected) Icons.Outlined.Check else Icons.Outlined.ChevronRight, contentDescription = if (selected) "当前空间" else null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}
