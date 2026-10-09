package com.secondbrain.exception;

import org.apache.catalina.connector.ClientAbortException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** 验证已断开的连接不再被写入，同时保留正常请求的错误响应。 */
class GlobalExceptionHandlerTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new FailureController())
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    void clientDisconnectDoesNotWriteAnotherResponse() throws Exception {
        mvc.perform(get("/disconnect")).andExpect(content().string(""));
    }

    @Test
    void unrelatedIoFailureStillReturnsAnError() throws Exception {
        mvc.perform(get("/io-failure"))
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("系统异常，请联系管理员"));
    }

    /** 使用隔离接口模拟容器异常，避免测试依赖模型或数据库。 */
    @RestController
    static class FailureController {
        /**
         * 模拟响应写入期间客户端断开。
         *
         * @throws ClientAbortException 模拟的连接中断
         */
        @GetMapping("/disconnect")
        public void disconnect() throws ClientAbortException {
            throw new ClientAbortException(new IOException("Connection reset by peer"));
        }

        /**
         * 模拟与客户端连接无关的 I/O 错误。
         *
         * @throws IOException 模拟的存储读取失败
         */
        @GetMapping("/io-failure")
        public void ioFailure() throws IOException {
            throw new IOException("Storage unavailable");
        }
    }
}
