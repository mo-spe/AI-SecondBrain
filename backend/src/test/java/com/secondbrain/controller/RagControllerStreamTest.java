package com.secondbrain.controller;

import com.secondbrain.dto.StreamEvent;
import com.secondbrain.mapper.KnowledgeNodeMapper;
import com.secondbrain.service.KnowledgeVectorService;
import com.secondbrain.service.RagService;
import com.secondbrain.service.impl.RagStreamingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RagControllerStreamTest {
    private RagStreamingServiceImpl streaming;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        streaming = mock(RagStreamingServiceImpl.class);
        mvc = MockMvcBuilders.standaloneSetup(new RagController(mock(RagService.class),
                mock(KnowledgeVectorService.class), mock(KnowledgeNodeMapper.class), streaming)).build();
    }

    @Test
    void sendsReferencesTokensAndOneTerminalEvent() throws Exception {
        doAnswer(invocation -> {
            Consumer<StreamEvent> consumer = invocation.getArgument(3);
            consumer.accept(StreamEvent.references("[]"));
            consumer.accept(StreamEvent.token("answer "));
            consumer.accept(StreamEvent.done());
            consumer.accept(StreamEvent.done());
            return null;
        }).when(streaming).streamAnswer(any(), eq(5L), isNull(), any());
        String body = response();
        assertThat(body).contains("event:references", "event:token", "data:answer ", "event:done");
        assertThat(body.split("event:done", -1)).hasSize(2);
    }

    @Test
    void linkageErrorCompletesStreamWithSafeErrorInsteadOfHanging() throws Exception {
        doThrow(new NoClassDefFoundError("com/secondbrain/controller/RagController$1"))
                .when(streaming).streamAnswer(any(), eq(5L), isNull(), any());
        assertThat(response()).contains("event:error", "问答服务暂时不可用").doesNotContain("NoClassDefFoundError");
    }

    @Test
    void incompleteServiceResponseProducesError() throws Exception {
        assertThat(response()).contains("event:error", "回答意外中断");
    }

    @Test
    void serviceErrorIsTerminalWithoutDuplicateDone() throws Exception {
        doAnswer(invocation -> {
            Consumer<StreamEvent> consumer = invocation.getArgument(3);
            consumer.accept(StreamEvent.error("请检查模型设置"));
            consumer.accept(StreamEvent.done());
            return null;
        }).when(streaming).streamAnswer(any(), eq(5L), isNull(), any());
        assertThat(response()).contains("event:error", "请检查模型设置").doesNotContain("event:done");
    }

    private String response() throws Exception {
        MvcResult result = mvc.perform(post("/rag/answer/stream").requestAttr("userId", 5L)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"question\":\"Redis\"}"))
                .andExpect(request().asyncStarted()).andReturn();
        result.getAsyncResult(3000);
        return mvc.perform(asyncDispatch(result)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
    }
}
