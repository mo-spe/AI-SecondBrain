package com.secondbrain.android.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.secondbrain.android.data.remote.CommunityQuestion
import com.secondbrain.android.data.remote.KnowledgeNode
import com.secondbrain.android.data.remote.ReviewCard
import com.secondbrain.android.data.remote.SecondBrainApi
import com.secondbrain.android.data.remote.SquarePost
import com.secondbrain.android.data.remote.Workspace
import com.secondbrain.android.data.remote.requireData
import com.secondbrain.android.data.repository.KnowledgeRepository
import com.secondbrain.android.data.session.SessionStore
import com.secondbrain.android.reminder.ReminderRecovery
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Content<T>(val value: T) : LoadState<T>
    data class Empty(val message: String) : LoadState<Nothing>
    data class Failure(val message: String) : LoadState<Nothing>
}

@HiltViewModel
class KnowledgeViewModel @Inject constructor(private val repository: KnowledgeRepository, sessionStore: SessionStore) : ViewModel() {
    private val _state = MutableStateFlow<LoadState<List<KnowledgeNode>>>(LoadState.Loading)
    val state: StateFlow<LoadState<List<KnowledgeNode>>> = _state

    suspend fun detail(id: Long): KnowledgeNode = repository.detail(id)

    private var loadJob: Job? = null
    private var moreJob: Job? = null
    private var page = 1
    private val _listing = MutableStateFlow(KnowledgeListing())
    val listing: StateFlow<KnowledgeListing> = _listing

    fun load(keyword: String? = _listing.value.keyword): Job {
        loadJob?.cancel()
        moreJob?.cancel()
        _listing.value = KnowledgeListing(keyword = keyword?.trim()?.takeIf { it.isNotEmpty() })
        _state.value = LoadState.Loading
        page = 1
        return viewModelScope.launch {
            val result = runCatching { repository.page(1, _listing.value.keyword) }
            ensureActive()
            result.fold(
                onSuccess = {
                    _listing.value = _listing.value.copy(total = it.total)
                    _state.value = if (it.records.isEmpty()) LoadState.Empty(if (_listing.value.keyword == null) "还没有知识点，试试从采集开始。" else "没有找到匹配的知识，试试其他关键词。") else LoadState.Content(it.records)
                },
                onFailure = { _state.value = LoadState.Failure(it.message ?: "知识加载失败") }
            )
        }.also { loadJob = it }
    }

    fun loadMore() {
        val nodes = (_state.value as? LoadState.Content)?.value ?: return
        if (_listing.value.loadingMore || nodes.size >= _listing.value.total) return
        _listing.value = _listing.value.copy(loadingMore = true, error = null)
        moreJob = viewModelScope.launch {
            val result = runCatching { repository.page(page + 1, _listing.value.keyword) }
            ensureActive()
            result.fold(onSuccess = {
                page++
                val merged = (nodes + it.records).distinctBy { node -> node.id }
                _state.value = LoadState.Content(merged)
                _listing.value = _listing.value.copy(total = if (it.records.isEmpty()) merged.size.toLong() else it.total, loadingMore = false)
            }, onFailure = { _listing.value = _listing.value.copy(loadingMore = false, error = it.message ?: "加载失败，请重试") })
        }
    }

    init { viewModelScope.launch { sessionStore.tokenFlow.distinctUntilChanged().collectLatest { load(null).join() } } }

}

@HiltViewModel
class TodayViewModel @Inject constructor(private val api: SecondBrainApi, sessionStore: SessionStore) : ViewModel() {
    private val _state = MutableStateFlow<LoadState<List<ReviewCard>>>(LoadState.Loading)
    val state: StateFlow<LoadState<List<ReviewCard>>> = _state
    private var loadJob: Job? = null
    fun load(): Job {
        loadJob?.cancel()
        return viewModelScope.launch {
            _state.value = LoadState.Loading
            val result = runCatching { api.todayReview().requireData() }
            ensureActive()
            result.fold(
                { _state.value = if (it.isEmpty()) LoadState.Empty("今天没有待复习卡片。") else LoadState.Content(it) },
                { _state.value = LoadState.Failure(it.message ?: "复习计划加载失败") }
            )
        }.also { loadJob = it }
    }
    init { viewModelScope.launch { sessionStore.tokenFlow.distinctUntilChanged().collectLatest { load().join() } } }
}

@HiltViewModel
class CommunityViewModel @Inject constructor(private val api: SecondBrainApi) : ViewModel() {
    private val _square = MutableStateFlow<LoadState<List<SquarePost>>>(LoadState.Loading)
    val square: StateFlow<LoadState<List<SquarePost>>> = _square
    private val _questions = MutableStateFlow<LoadState<List<CommunityQuestion>>>(LoadState.Loading)
    val questions: StateFlow<LoadState<List<CommunityQuestion>>> = _questions
    fun loadSquare() = viewModelScope.launch {
        _square.value = LoadState.Loading
        runCatching { api.square().requireData().records }.fold(
            { _square.value = if (it.isEmpty()) LoadState.Empty("暂时没有公开分享。") else LoadState.Content(it) },
            { _square.value = LoadState.Failure(it.message ?: "广场加载失败") }
        )
    }
    fun loadQuestions() = viewModelScope.launch {
        _questions.value = LoadState.Loading
        runCatching { api.questions().requireData().records }.fold(
            { _questions.value = if (it.isEmpty()) LoadState.Empty("暂时没有公开问题。") else LoadState.Content(it) },
            { _questions.value = LoadState.Failure(it.message ?: "问答加载失败") }
        )
    }
    suspend fun toggleLike(id: Long): Boolean = api.toggleLike(id).requireData().also { loadSquare() }
    suspend fun toggleBookmark(id: Long): Boolean = api.toggleBookmark(id).requireData().also { loadSquare() }
    suspend fun postDetail(id: Long): SquarePost = api.squareDetail(id).requireData()
    suspend fun questionDetail(id: Long): CommunityQuestion = api.questionDetail(id).requireData()
    init { loadSquare(); loadQuestions() }
}

@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val api: SecondBrainApi,
    private val sessionStore: SessionStore,
    @ApplicationContext private val applicationContext: Context
) : ViewModel() {
    private val _state = MutableStateFlow<LoadState<List<Workspace>>>(LoadState.Loading)
    val state: StateFlow<LoadState<List<Workspace>>> = _state
    private val _selection = MutableStateFlow(WorkspaceSelection())
    val selection: StateFlow<WorkspaceSelection> = _selection

    fun load() = viewModelScope.launch {
        _state.value = LoadState.Loading
        runCatching { api.workspaces().requireData() }.fold(
            { _state.value = LoadState.Content(it) },
            { _state.value = LoadState.Failure(it.message ?: "工作区加载失败") }
        )
    }

    fun select(id: Long) = switchTo(id)
    fun selectPersonal() = switchTo(null)

    private fun switchTo(id: Long?) {
        if (!_selection.value.ready || _selection.value.switching || _selection.value.activeId == id) return
        _selection.value = _selection.value.copy(switching = true, message = null)
        viewModelScope.launch {
            runCatching {
                val response = if (id == null) api.switchPersonalWorkspace() else api.switchWorkspace(id)
                val switched = response.requireData()
                check(switched.token.isNotBlank()) { "切换失败：服务没有返回有效凭证" }
                check(switched.workspaceId == id) { "切换失败：工作区信息不一致" }
                // 凭证与空间标识一起持久化，页面收到新会话后才重新读取数据。
                sessionStore.save(switched.token, id)
            }.onSuccess {
                _selection.value = WorkspaceSelection(activeId = id, ready = true, message = "已切换到${spaceName(id)}")
                ReminderRecovery.enqueue(applicationContext)
            }.onFailure {
                _selection.value = _selection.value.copy(switching = false, message = it.message ?: "切换失败，请重试")
            }
        }
    }

    private fun spaceName(id: Long?): String = if (id == null) "个人空间" else
        (_state.value as? LoadState.Content)?.value?.find { it.id == id }?.name ?: "协作工作区"

    init {
        load()
        viewModelScope.launch {
            sessionStore.workspaceFlow.distinctUntilChanged().collectLatest { id ->
                _selection.value = _selection.value.copy(activeId = id, ready = true)
            }
        }
    }
}

data class WorkspaceSelection(
    val activeId: Long? = null,
    val ready: Boolean = false,
    val switching: Boolean = false,
    val message: String? = null
)


data class KnowledgeListing(val keyword: String? = null, val total: Long = 0, val loadingMore: Boolean = false, val error: String? = null)
