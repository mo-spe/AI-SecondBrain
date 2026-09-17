package com.secondbrain.android.ui

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.ViewModelStore
import com.secondbrain.android.capture.*
import com.secondbrain.android.data.remote.*
import com.secondbrain.android.data.repository.KnowledgeRepository
import com.secondbrain.android.data.session.SessionStore
import com.secondbrain.android.rag.*
import com.secondbrain.android.reminder.validateReminderTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.lang.reflect.Proxy
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class ExtendedFlowsTest {
    private val dispatcher = StandardTestDispatcher()
    private val owners = ViewModelStore()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { owners.clear(); Dispatchers.resetMain() }

    @Test fun editsSurviveRestoringAndDiscardDoesNotResurrectDraft() = runTest(dispatcher) {
        val dao = Drafts()
        val vm = CaptureViewModel(dao, KnowledgeRepository(api { error(it) }), session()).also { owners.put("capture", it) }
        advanceUntilIdle(); vm.createManual(); advanceUntilIdle()
        vm.update("标题", "初稿"); vm.update("最终标题", "最终正文"); advanceUntilIdle()
        assertTrue(vm.state.value.localSaved)
        val saved = dao.rows.value.single()
        val restored = CaptureViewModel(dao, KnowledgeRepository(api { error(it) }), session()).also { owners.put("restored", it) }
        restored.restore(saved)
        assertEquals("最终正文", restored.state.value.draft?.content)
        vm.update("编辑", "删除前编辑"); vm.discard(saved.id); vm.update("迟到编辑", "不应复活")
        advanceUntilIdle()
        assertTrue(dao.rows.value.isEmpty())
        assertNull(vm.state.value.draft)
    }

    @Test fun localWriteFailureKeepsInputAndCanRetry() = runTest(dispatcher) {
        val dao = Drafts()
        val vm = CaptureViewModel(dao, KnowledgeRepository(api { error(it) }), session()).also { owners.put("capture", it) }
        advanceUntilIdle(); vm.createManual(); advanceUntilIdle()
        dao.failSave = true
        vm.update("不能丢失", "完整正文"); advanceUntilIdle()
        assertFalse(vm.state.value.localSaved)
        assertEquals("完整正文", vm.state.value.draft?.content)
        dao.failSave = false
        vm.update("不能丢失", "完整正文"); advanceUntilIdle()
        assertTrue(vm.state.value.localSaved)
        assertEquals("完整正文", dao.rows.value.single().content)
    }

    @Test fun failedKnowledgeSaveRetainsDraftAndRetryRejectsDuplicateTaps() = runTest(dispatcher) {
        var calls = 0
        val dao = Drafts()
        val vm = CaptureViewModel(dao, KnowledgeRepository(api {
            if (++calls == 1) error("离线") else ApiResult(200, "ok", KnowledgeNode(9, "标题"))
        }), session()).also { owners.put("capture", it) }
        advanceUntilIdle(); vm.createManual(); advanceUntilIdle(); vm.update("标题", "正文")
        vm.saveConfirmed(); vm.saveConfirmed(); advanceUntilIdle()
        assertEquals(1, calls)
        assertEquals("正文", vm.state.value.draft?.content)
        assertEquals(1, dao.rows.value.size)
        vm.saveConfirmed(); vm.saveConfirmed(); advanceUntilIdle()
        assertEquals(2, calls)
        assertNull(vm.state.value.draft)
        assertTrue(dao.rows.value.isEmpty())
    }

    @Test fun switchingWorkspacePreventsPublishingDraftIntoAnotherSpace() = runTest(dispatcher) {
        val session = session()
        val vm = CaptureViewModel(Drafts(), KnowledgeRepository(api { error("不应请求接口") }), session).also { owners.put("capture", it) }
        advanceUntilIdle(); vm.createManual(); advanceUntilIdle(); vm.update("标题", "正文")
        session.save("team", 5); advanceUntilIdle(); vm.saveConfirmed(); advanceUntilIdle()
        assertTrue(vm.state.value.message.orEmpty().contains("所属空间"))
        assertNotNull(vm.state.value.draft)
        assertFalse(vm.state.value.saving)
    }

    @Test fun ragStopsAndSessionSwitchCancelsOldAnswer() = runTest(dispatcher) {
        var started = 0
        var cancelled = 0
        val source = RagAnswerSource { flow {
            started++
            try { emit(RagStreamEvent("token", "保留的文字 ")); awaitCancellation() }
            finally { cancelled++ }
        } }
        val session = session().also { it.save("personal") }
        val vm = RagViewModel(source, session).also { owners.put("rag", it) }
        advanceUntilIdle(); vm.updateQuestion("问题"); vm.ask(); vm.ask(); runCurrent()
        assertEquals(1, started)
        vm.updateQuestion("不能改动")
        assertEquals("问题", vm.state.value.question)
        vm.stop(); runCurrent()
        assertEquals(1, cancelled)
        assertEquals("保留的文字 ", vm.state.value.answer)
        assertFalse(vm.state.value.asking)
        vm.ask(); runCurrent(); session.save("team", 5); runCurrent()
        assertEquals(2, cancelled)
        assertEquals("", vm.state.value.answer)
        assertFalse(vm.state.value.asking)
        assertTrue(vm.state.value.ready)
    }

    @Test fun ragDistinguishesTruncatedStreamFromCompletedAnswer() = runTest(dispatcher) {
        var complete = false
        val vm = RagViewModel(RagAnswerSource { flow {
            emit(RagStreamEvent("token", "a "))
            emit(RagStreamEvent("token", "b"))
            if (complete) emit(RagStreamEvent("done", ""))
        } }, session().also { it.save("personal") }).also { owners.put("rag", it) }
        advanceUntilIdle(); vm.updateQuestion("问题"); vm.ask(); advanceUntilIdle()
        assertEquals("a b", vm.state.value.answer)
        assertTrue(vm.state.value.error.orEmpty().contains("提前结束"))
        complete = true; vm.ask(); advanceUntilIdle()
        assertNull(vm.state.value.error)
        assertFalse(vm.state.value.asking)
    }

    @Test fun ragFailureKeepsPartialAnswerAndDoesNotExposeUnknownEvents() = runTest(dispatcher) {
        val vm = RagViewModel(RagAnswerSource { flow {
            emit(RagStreamEvent("thinking", "内部过程"))
            emit(RagStreamEvent("token", "已收到的答案"))
            throw java.io.IOException("网络中断")
        } }, session().also { it.save("personal") }).also { owners.put("rag", it) }
        advanceUntilIdle(); vm.updateQuestion("问题"); vm.ask(); advanceUntilIdle()
        assertEquals("已收到的答案", vm.state.value.answer)
        assertEquals("网络中断", vm.state.value.error)
        assertFalse(vm.state.value.asking)
    }

    @Test fun staleUnauthorizedResponseCannotClearNewSession() = runTest(dispatcher) {
        val session = session()
        session.save("old"); session.save("new", 5)
        session.clearIfTokenMatches("old"); session.clearIfTokenMatches(null)
        assertEquals("new", session.token())
        assertEquals(5L, session.workspaceId())
        session.clearIfTokenMatches("new")
        assertNull(session.token()); assertNull(session.workspaceId())
    }

    @Test fun reminderRejectsInvalidPastAndPresentTime() {
        val now = LocalDateTime.of(2026, 9, 11, 12, 0)
        assertNotNull(validateReminderTime(1, "not-a-date", now))
        assertNotNull(validateReminderTime(1, now.toString(), now))
        assertNotNull(validateReminderTime(1, now.minusMinutes(1).toString(), now))
        assertNotNull(validateReminderTime(0, now.plusDays(1).toString(), now))
        assertNull(validateReminderTime(1, now.plusMinutes(1).toString(), now))
    }

    private fun session(): SessionStore {
        val preferences = MutableStateFlow<Preferences>(emptyPreferences())
        return SessionStore(object : DataStore<Preferences> {
            override val data = preferences
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = transform(preferences.value).also { preferences.value = it }
        })
    }

    private fun api(call: (String) -> Any): SecondBrainApi = Proxy.newProxyInstance(
        SecondBrainApi::class.java.classLoader, arrayOf(SecondBrainApi::class.java)
    ) { _, method, _ -> call(method.name) } as SecondBrainApi

    private class Drafts : OcrDraftDao {
        val rows = MutableStateFlow<List<OcrDraft>>(emptyList())
        var failSave = false
        override fun observeAll(): Flow<List<OcrDraft>> = rows
        override suspend fun save(draft: OcrDraft): Long {
            if (failSave) error("磁盘不可用")
            val id = draft.id.takeIf { it != 0L } ?: 1L
            rows.value = rows.value.filterNot { it.id == id } + draft.copy(id = id)
            return id
        }
        override suspend fun delete(draftId: Long) { rows.value = rows.value.filterNot { it.id == draftId } }
    }
}
