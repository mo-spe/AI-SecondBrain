package com.secondbrain.android.rag

import okio.Buffer
import org.junit.Assert.*
import org.junit.Test

class RagStreamTest {
    @Test fun preservesSpacesAndMultilineDataAcrossCrLfFrames() {
        val events = mutableListOf<RagStreamEvent>()
        readRagEvents(Buffer().writeUtf8(": heartbeat\r\nevent: token\r\ndata:  indented \r\ndata: next\r\n\r\nevent: done\ndata:\n\n")) { events.add(it); true }
        assertEquals(listOf(RagStreamEvent("token", " indented \nnext"), RagStreamEvent("done", "")), events)
    }

    @Test fun resetsEventNameAndIgnoresIncompleteFrame() {
        val events = mutableListOf<RagStreamEvent>()
        readRagEvents(Buffer().writeUtf8("event: token\ndata: first\n\ndata: second\n\nevent: token\ndata: incomplete")) { events.add(it); true }
        assertEquals(listOf(RagStreamEvent("token", "first"), RagStreamEvent("message", "second")), events)
    }

    @Test fun consumerCanStopReadingAfterDone() {
        val events = mutableListOf<RagStreamEvent>()
        readRagEvents(Buffer().writeUtf8("event: done\ndata: ok\n\nevent: token\ndata: late\n\n")) { events.add(it); false }
        assertEquals(listOf(RagStreamEvent("done", "ok")), events)
    }
}
