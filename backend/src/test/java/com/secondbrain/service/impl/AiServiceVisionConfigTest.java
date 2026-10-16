package com.secondbrain.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.secondbrain.entity.AiProvider;
import com.secondbrain.entity.UserAiConfig;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.AiProviderMapper;
import com.secondbrain.mapper.UserAiConfigMapper;
import com.secondbrain.mapper.UserAiProviderKeyMapper;
import com.secondbrain.service.ApiKeyEncryptionService;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.RETURNS_SELF;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/** 确保保存成功并不掩盖服务商、模型和密钥不匹配的问题。 */
class AiServiceVisionConfigTest {
    private final AiProviderMapper providerMapper = mock(AiProviderMapper.class);
    private final UserAiConfigMapper configMapper = mock(UserAiConfigMapper.class);
    private final UserAiProviderKeyMapper providerKeyMapper = mock(UserAiProviderKeyMapper.class);
    private final ApiKeyEncryptionService encryptionService = mock(ApiKeyEncryptionService.class);
    private final AiServiceImpl service = new AiServiceImpl(providerMapper, configMapper, providerKeyMapper,
            encryptionService, mock(RestTemplate.class), mock(RestTemplate.class));

    @Test
    void rejectsQwenModelOnAnotherProvider() {
        givenConfiguration("zhipu", "qwen3-vl-flash");

        assertThatThrownBy(() -> service.resolveVisionConfig(7L, "vision"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("通义千问");
    }

    @Test
    void acceptsMatchingVisionProviderAndModel() {
        givenConfiguration("qwen", "qwen3-vl-flash");

        assertThat(service.resolveVisionConfig(7L, "vision").getModelName()).isEqualTo("qwen3-vl-flash");
    }

    @Test
    void promptsToReplaceUnreadableSavedKey() {
        givenConfiguration("qwen", "qwen3-vl-flash");
        when(encryptionService.decrypt("encrypted-key")).thenThrow(new IllegalStateException("decrypt failed"));

        assertThatThrownBy(() -> service.resolveVisionConfig(7L, "vision"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重新输入");
    }

    @Test
    void sendsStructuredBudgetAndJsonModeWithoutChangingOrdinaryAnswers() throws IOException {
        AtomicReference<JsonNode> request = new AtomicReference<>();
        HttpServer server = completionServer("stop", "{}", request);
        try {
            givenConfiguration("qwen", "qwen3-vl-flash", endpoint(server));

            assertThat(service.generateJsonAnswer(7L, "vision", "只返回 JSON：{\"article\":\"正文\"}"))
                    .isEqualTo("{}");
            assertThat(request.get().path("max_tokens").asInt()).isEqualTo(8192);
            assertThat(request.get().path("response_format").path("type").asText()).isEqualTo("json_object");

            assertThat(service.generateAnswer(7L, "vision", "给出中文释义")).isEqualTo("{}");
            assertThat(request.get().path("max_tokens").asInt()).isEqualTo(2048);
            assertThat(request.get().path("response_format").isMissingNode()
                    || request.get().path("response_format").isNull()).isTrue();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void keepsUnknownProviderFormatParametersCompatible() throws IOException {
        AtomicReference<JsonNode> request = new AtomicReference<>();
        HttpServer server = completionServer("stop", "{}", request);
        try {
            givenConfiguration("zhipu", "glm-4v", endpoint(server));
            service.generateJsonAnswer(7L, "vision", "返回 JSON 对象");
            assertThat(request.get().path("max_tokens").asInt()).isEqualTo(4096);
            assertThat(request.get().path("response_format").isMissingNode()
                    || request.get().path("response_format").isNull()).isTrue();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsProviderTruncationEvenWhenReturnedContentLooksLikeJson() throws IOException {
        AtomicReference<JsonNode> request = new AtomicReference<>();
        HttpServer server = completionServer("length", "{\"article\":\"Incomplete ending.\"}", request);
        try {
            givenConfiguration("qwen", "qwen3-vl-flash", endpoint(server));
            assertThatThrownBy(() -> service.generateJsonAnswer(7L, "vision", "返回 JSON 对象"))
                    .isInstanceOf(BusinessException.class).hasMessageContaining("截断");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void identifiesBackendDnsFailureWithoutExposingTechnicalDetails() throws IOException {
        assertVisionIoFailure(new UnknownHostException("private-host secret-test-value"), "域名", 503);
    }

    @Test
    void identifiesBackendModelTimeout() throws IOException {
        assertVisionIoFailure(new SocketTimeoutException("secret-test-value"), "超时", 503);
    }

    @Test
    void preservesGenericConnectionErrorForOtherIoFailures() throws IOException {
        assertVisionIoFailure(new IOException("secret-test-value"), "视觉识别连接失败", 503);
    }

    @Test
    void doesNotReportMalformedProviderResponseAsConnectionFailure() throws IOException {
        AtomicReference<JsonNode> request = new AtomicReference<>();
        HttpServer server = completionServer("not-json secret-test-value", request);
        try {
            givenConfiguration("qwen", "qwen3-vl-flash", endpoint(server));
            assertThatThrownBy(() -> service.analyzeImages(7L, "vision", "提取候选词",
                    List.of(new byte[]{1}), List.of("image/png")))
                    .isInstanceOfSatisfying(BusinessException.class, failure -> {
                        assertThat(failure.getCode()).isEqualTo(502);
                        assertThat(failure.getMessage()).contains("响应格式无效")
                                .doesNotContain("连接失败", "secret-test-value");
                    });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void preservesSuccessfulVisionExtractionAndNormalizesVersionPath() throws IOException {
        AtomicReference<JsonNode> request = new AtomicReference<>();
        HttpServer server = completionServer("stop", "{\"words\":[\"art\"]}", request);
        try {
            givenConfiguration("qwen", "qwen3-vl-flash", endpoint(server) + "/v1/");
            assertThat(service.analyzeImages(7L, "vision", "提取候选词",
                    List.of(new byte[]{1}), List.of("image/png")))
                    .isEqualTo("{\"words\":[\"art\"]}");
            assertThat(request.get().path("messages").path(0).path("content").path(1)
                    .path("image_url").path("url").asText()).isEqualTo("data:image/png;base64,AQ==");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void reportsModelAvailabilityForProvider404WithoutExposingResponseBody() throws IOException {
        HttpServer server = completionServer("{\"error\":{\"code\":\"model_not_found\","
                + "\"message\":\"secret-test-value\"}}", new AtomicReference<>(), 404);
        try {
            givenConfiguration("qwen", "qwen3-vl-flash", endpoint(server));
            assertThatThrownBy(() -> service.analyzeImages(7L, "vision", "提取候选词",
                    List.of(new byte[]{1}), List.of("image/png")))
                    .isInstanceOfSatisfying(BusinessException.class, failure -> {
                        assertThat(failure.getCode()).isEqualTo(502);
                        assertThat(failure.getMessage()).contains("HTTP 404", "Key 无权访问", "地域")
                                .doesNotContain("secret-test-value");
                    });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void preserves404DiagnosisWhenGatewayReturnsHtmlInsteadOfJson() throws IOException {
        HttpServer server = completionServer("<html>secret-test-value</html>", new AtomicReference<>(), 404);
        try {
            givenConfiguration("qwen", "qwen3-vl-flash", endpoint(server));
            assertThatThrownBy(() -> service.analyzeImages(7L, "vision", "提取候选词",
                    List.of(new byte[]{1}), List.of("image/png")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("接口或模型不可用（HTTP 404）")
                    .hasMessageNotContaining("连接失败")
                    .hasMessageNotContaining("secret-test-value");
        } finally {
            server.stop(0);
        }
    }

    private void assertVisionIoFailure(IOException cause, String expectedMessage, int expectedCode) throws IOException {
        OkHttpClient client = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        when(client.newCall(any(Request.class))).thenReturn(call);
        when(call.execute()).thenThrow(cause);
        // 在传输边界注入故障，避免测试依赖真实 DNS 故障或等待网络超时。
        try (var builders = mockConstruction(OkHttpClient.Builder.class,
                withSettings().defaultAnswer(RETURNS_SELF),
                (builder, context) -> when(builder.build()).thenReturn(client))) {
            givenConfiguration("qwen", "qwen3-vl-flash");
            assertThatThrownBy(() -> service.analyzeImages(7L, "vision", "提取候选词",
                    List.of(new byte[]{1}), List.of("image/png")))
                    .isInstanceOfSatisfying(BusinessException.class, failure -> {
                        assertThat(failure.getCode()).isEqualTo(expectedCode);
                        assertThat(failure.getMessage()).contains(expectedMessage)
                                .doesNotContain("secret-test-value", "private-host");
                    });
            verify(call).execute();
        }
    }

    private HttpServer completionServer(String finishReason, String content,
                                        AtomicReference<JsonNode> request) throws IOException {
        ObjectMapper json = new ObjectMapper();
        String response = json.writeValueAsString(Map.of(
                "id", "test-completion", "object", "chat.completion", "created", 0,
                "model", "test-model", "choices", List.of(Map.of("index", 0,
                        "message", Map.of("role", "assistant", "content", content),
                        "finish_reason", finishReason))));
        return completionServer(response, request);
    }

    private HttpServer completionServer(String responseBody, AtomicReference<JsonNode> request) throws IOException {
        return completionServer(responseBody, request, 200);
    }

    private HttpServer completionServer(String responseBody, AtomicReference<JsonNode> request,
                                        int statusCode) throws IOException {
        ObjectMapper json = new ObjectMapper();
        // 使用本地 HTTP 替身验证 SDK 真正发送的参数，不能只验证业务层传入的方法参数。
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            request.set(json.readTree(exchange.getRequestBody()));
            byte[] response = responseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, response.length);
            try (var responseStream = exchange.getResponseBody()) {
                responseStream.write(response);
            }
        });
        server.start();
        return server;
    }

    private String endpoint(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void givenConfiguration(String providerCode, String modelName) {
        givenConfiguration(providerCode, modelName, "https://example.invalid/api/v1");
    }

    private void givenConfiguration(String providerCode, String modelName, String baseUrl) {
        UserAiConfig saved = new UserAiConfig();
        saved.setProviderId(3L);
        saved.setModelName(modelName);
        saved.setApiKey("encrypted-key");
        AiProvider provider = new AiProvider();
        provider.setId(3L);
        provider.setCode(providerCode);
        provider.setName(providerCode);
        provider.setApiType("openai_compatible");
        provider.setBaseUrl(baseUrl);
        provider.setIsEnabled(1);
        when(configMapper.selectOne(any())).thenReturn(saved);
        when(providerMapper.selectById(3L)).thenReturn(provider);
        when(encryptionService.decrypt("encrypted-key")).thenReturn("personal-key");
    }
}
