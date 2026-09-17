package com.secondbrain.android.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import com.secondbrain.android.data.remote.*
import com.secondbrain.android.data.repository.KnowledgeRepository
import com.secondbrain.android.data.session.SessionStore
import com.secondbrain.android.review.ReviewViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.lang.reflect.Proxy

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureStateTest {
    private val dispatcher = StandardTestDispatcher()
    private val owners = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { owners.clear(); Dispatchers.resetMain() }

    @Test fun submissionKeepsFeedbackUntilNextAndRejectsDuplicateTaps() = runTest(dispatcher) {
        var submissions = 0
        val api = fakeApi { name -> when (name) {
            "todayReview" -> ApiResult(200, "ok", listOf(ReviewCard(1, question = "题目一"), ReviewCard(2, question = "题目二")))
            "submitReview" -> { submissions++; ApiResult(200, "ok", ReviewResult(true, "B", "解析内容")) }
            else -> error(name)
        } }
        val vm = ReviewViewModel(api).also { owners.put("review", it) }
        vm.load(); advanceUntilIdle()
        vm.updateAnswer("B"); vm.submit(); vm.submit(); advanceUntilIdle()
        assertEquals(1, submissions)
        assertEquals(0, vm.state.value.position)
        assertEquals("解析内容", vm.state.value.result?.explanation)
        vm.updateAnswer("A")
        assertEquals("B", vm.state.value.answer)
        vm.next()
        assertEquals(1, vm.state.value.position)
        assertNull(vm.state.value.result)
        assertEquals("", vm.state.value.answer)
    }

    @Test fun failedSubmitPreservesInputAndCanBeRetried() = runTest(dispatcher) {
        var attempts = 0
        val api = fakeApi { name -> when (name) {
            "todayReview" -> ApiResult(200, "ok", listOf(ReviewCard(1, question = "说明原因")))
            "submitReview" -> if (++attempts == 1) error("连接中断") else ApiResult(200, "ok", ReviewResult(false, "参考答案", "原因"))
            else -> error(name)
        } }
        val vm = ReviewViewModel(api).also { owners.put("review", it) }
        vm.load(); advanceUntilIdle(); vm.updateAnswer("我的答案"); vm.submit(); advanceUntilIdle()
        assertEquals("我的答案", vm.state.value.answer)
        assertEquals("连接中断", vm.state.value.message)
        assertNull(vm.state.value.result)
        vm.next()
        assertEquals(0, vm.state.value.position)
        vm.submit(); advanceUntilIdle()
        assertEquals(2, attempts)
        assertNotNull(vm.state.value.result)
    }

    @Test fun sessionChangeRefreshesAlreadyLoadedKnowledgeAndReview() = runTest(dispatcher) {
        val preferences = MutableStateFlow<Preferences>(emptyPreferences())
        val store = object : DataStore<Preferences> {
            override val data = preferences
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = transform(preferences.value).also { preferences.value = it }
        }
        val session = SessionStore(store)
        var scopeName = "个人"
        session.save("personal")
        val api = fakeApi { name -> when (name) {
            "knowledge" -> ApiResult(200, "ok", KnowledgePage(listOf(KnowledgeNode(1, scopeName))))
            "todayReview" -> ApiResult(200, "ok", listOf(ReviewCard(1, question = scopeName)))
            else -> error(name)
        } }
        val knowledge = KnowledgeViewModel(KnowledgeRepository(api), session).also { owners.put("knowledge", it) }
        val today = TodayViewModel(api, session).also { owners.put("today", it) }
        advanceUntilIdle()
        assertEquals("个人", (knowledge.state.value as LoadState.Content).value.single().title)
        scopeName = "团队"
        session.save("team", 5); advanceUntilIdle()
        assertEquals("团队", (knowledge.state.value as LoadState.Content).value.single().title)
        assertEquals("团队", (today.state.value as LoadState.Content).value.single().question)
        assertEquals(5L, session.workspaceId())
        scopeName = "个人"
        session.save("personal-return"); advanceUntilIdle()
        assertNull(session.workspaceId())
        assertEquals("个人", (knowledge.state.value as LoadState.Content).value.single().title)
        assertEquals("个人", (today.state.value as LoadState.Content).value.single().question)
    }

    @Test fun answerFailureRetainsDraftThenSuccessAppearsAfterReopening() = runTest(dispatcher) {
        var question = CommunityQuestion(1, "如何理解锁粒度？", answers = emptyList())
        var attempts = 0
        val api = fakeApi { name -> when (name) {
            "questionDetail" -> ApiResult(200, "ok", question)
            "answerQuestion" -> {
                if (++attempts == 1) error("连接失败")
                val answer = CommunityAnswer(9, "测试成员", "锁粒度需要结合共享数据范围来确定。")
                question = question.copy(answers = listOf(answer), answerCount = 1)
                ApiResult(200, "ok", answer)
            }
            else -> error(name)
        } }
        val vm = QuestionViewModel(api).also { owners.put("question", it) }
        vm.load(1); advanceUntilIdle()
        vm.edit("锁粒度需要结合共享数据范围来确定。")
        vm.submit(); vm.submit(); advanceUntilIdle()
        assertEquals(1, attempts)
        assertEquals("连接失败", vm.state.value.error)
        assertTrue(vm.state.value.draft.isNotBlank())
        vm.submit(); vm.submit(); advanceUntilIdle()
        assertEquals(2, attempts)
        assertTrue(vm.state.value.published)
        assertEquals("", vm.state.value.draft)
        assertEquals(9L, vm.state.value.question?.answers?.single()?.id)
        vm.load(1); advanceUntilIdle()
        assertEquals(1, vm.state.value.question?.answerCount)
        assertEquals(9L, vm.state.value.question?.answers?.single()?.id)
    }

    @Test fun answerRejectsWhitespaceAndLoadFailureCanRetry() = runTest(dispatcher) {
        var loads = 0
        val api = fakeApi { name -> when (name) {
            "questionDetail" -> if (++loads == 1) error("暂时离线") else ApiResult(200, "ok", CommunityQuestion(1, "问题"))
            else -> error("不应提交：$name")
        } }
        val vm = QuestionViewModel(api).also { owners.put("question", it) }
        vm.load(1); advanceUntilIdle()
        assertEquals("暂时离线", vm.state.value.error)
        vm.load(1); advanceUntilIdle()
        vm.edit("          "); vm.submit(); advanceUntilIdle()
        assertFalse(vm.state.value.submitting)
        assertFalse(vm.state.value.published)
    }

    @Test fun reviewStartsAtSelectedCardAndRecompositionKeepsAnswer() = runTest(dispatcher) {
        val api = fakeApi { ApiResult(200, "ok", listOf(ReviewCard(1, question = "第一题"), ReviewCard(2, question = "第二题"))) }
        val vm = ReviewViewModel(api).also { owners.put("review", it) }
        vm.loadFrom(2); advanceUntilIdle()
        assertEquals(2L, vm.state.value.cards.first().id)
        vm.updateAnswer("我的答案")
        vm.ensureLoaded(); advanceUntilIdle()
        assertEquals("我的答案", vm.state.value.answer)
        assertEquals(2, vm.state.value.cards.size)
    }

    @Test fun knowledgeSearchAndPaginationKeepResultsAndPreventDuplicateLoads() = runTest(dispatcher) {
        val preferences = MutableStateFlow<Preferences>(emptyPreferences())
        val store = object : DataStore<Preferences> {
            override val data = preferences
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences) = transform(preferences.value).also { preferences.value = it }
        }
        var requests = 0
        var lastKeyword: String? = null
        val api = Proxy.newProxyInstance(SecondBrainApi::class.java.classLoader, arrayOf(SecondBrainApi::class.java)) { _, method, args ->
            check(method.name == "knowledge")
            requests++
            lastKeyword = args[2] as String?
            val page = args[0] as Int
            ApiResult(200, "ok", KnowledgePage(listOf(KnowledgeNode(page.toLong(), "第 $page 条")), 2))
        } as SecondBrainApi
        val vm = KnowledgeViewModel(KnowledgeRepository(api), SessionStore(store)).also { owners.put("knowledge", it) }
        advanceUntilIdle()
        vm.load(" Redis "); advanceUntilIdle()
        assertEquals("Redis", lastKeyword)
        vm.loadMore(); vm.loadMore(); advanceUntilIdle()
        assertEquals(3, requests)
        assertEquals(listOf(1L, 2L), (vm.state.value as LoadState.Content).value.map { it.id })
        vm.loadMore(); advanceUntilIdle()
        assertEquals(3, requests)
        vm.load(null); advanceUntilIdle()
        assertNull(lastKeyword)
        assertEquals(1, (vm.state.value as LoadState.Content).value.size)
    }

    private fun fakeApi(call: (String) -> Any): SecondBrainApi = Proxy.newProxyInstance(
        SecondBrainApi::class.java.classLoader, arrayOf(SecondBrainApi::class.java)
    ) { _, method, _ -> call(method.name) } as SecondBrainApi
}
