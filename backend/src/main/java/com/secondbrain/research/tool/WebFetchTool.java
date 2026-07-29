package com.secondbrain.research.tool;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 网页内容抓取工具.
 *
 * <p>使用 OkHttp + Jsoup 抓取网页正文内容。
 * 包含 SSRF 防护（阻止内网 IP）和超时控制。
 * 支持通过 research.proxy.* 配置 HTTP 代理。</p>
 *
 * @author AI
 */
@Component
public class WebFetchTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(WebFetchTool.class);

    private final OkHttpClient httpClient;

    public WebFetchTool(
            @Value("${research.proxy.enabled:false}") boolean proxyEnabled,
            @Value("${research.proxy.host:127.0.0.1}") String proxyHost,
            @Value("${research.proxy.port:7890}") int proxyPort,
            @Value("${research.proxy.type:HTTP}") String proxyType) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true);

        if (proxyEnabled && !proxyHost.isBlank()) {
            Proxy.Type type = "SOCKS".equalsIgnoreCase(proxyType)
                    ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
            Proxy proxy = new Proxy(type, new InetSocketAddress(proxyHost, proxyPort));
            builder.proxy(proxy);
            log.info("web_fetch_proxy_configured type={} host={} port={}", proxyType, proxyHost, proxyPort);
        }

        this.httpClient = builder.build();
    }

    @Override
    public String getName() {
        return "web_fetch";
    }

    @Override
    public String getDescription() {
        return "抓取指定 URL 的网页内容，提取正文文本";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "url", Map.of("type", "string", "description", "要抓取的网页 URL"),
                "extractMode", Map.of("type", "string",
                        "enum", List.of("full", "snippet"),
                        "description", "提取模式：full（全文）或 snippet（摘要），默认 full")
        ));
        schema.put("required", List.of("url"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String url = (String) params.get("url");
        if (url == null || url.isBlank()) {
            return ToolResult.failure("INVALID_PARAM", "url 不能为空", false);
        }

        // SSRF 防护：阻止内网地址
        if (isPrivateUrl(url)) {
            return ToolResult.failure("SSRF_BLOCKED", "不允许访问内网地址: " + url, false);
        }

        long startMs = System.currentTimeMillis();

        try {
            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent",
                            "Mozilla/5.0 (compatible; AI-Research-Agent/1.0)")
                    .header("Accept",
                            "text/html,application/xhtml+xml,text/plain")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    return ToolResult.failure("FETCH_FAILED",
                            "HTTP " + response.code() + ": " + response.message(), false);
                }

                String contentType = response.header("Content-Type", "");
                if (!contentType.contains("text/html") && !contentType.contains("text/plain")) {
                    return ToolResult.failure("UNSUPPORTED_TYPE",
                            "不支持的内容类型: " + contentType, false);
                }

                String html = response.body().string();
                String text = extractText(html);

                // 截断过长内容
                if (text.length() > 50000) {
                    text = text.substring(0, 50000) + "...[内容已截断]";
                }

                long duration = System.currentTimeMillis() - startMs;
                ToolResult result = ToolResult.success(text);
                result.setDurationMs(duration);
                result.getMetadata().put("url", url);
                result.getMetadata().put("contentLength", text.length());
                result.getMetadata().put("contentType", contentType);
                return result;
            }
        } catch (IOException e) {
            log.error("web_fetch_failed url={}", url, e);
            return ToolResult.failure("FETCH_ERROR", e.getMessage(), url.contains("://"));
        }
    }

    /**
     * 使用 Jsoup 从 HTML 提取正文文本.
     */
    private String extractText(String html) {
        Document doc = Jsoup.parse(html);

        // 移除脚本和样式
        doc.select("script, style, nav, footer, header, aside, .sidebar, .nav, .menu, .advertisement")
                .remove();

        String title = doc.title();
        String body = doc.body() != null ? doc.body().text() : "";

        // 清理多余空白
        body = body.replaceAll("\\s+", " ").trim();

        StringBuilder sb = new StringBuilder();
        if (title != null && !title.isBlank()) {
            sb.append("标题: ").append(title).append("\n\n");
        }
        sb.append(body);
        return sb.toString();
    }

    /**
     * 检测是否为内网地址（SSRF 防护）.
     */
    private boolean isPrivateUrl(String url) {
        try {
            java.net.URI uri = java.net.URI.create(url);
            String host = uri.getHost();
            if (host == null) return true;

            // localhost
            if (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("0.0.0.0")) {
                return true;
            }

            // 内网地址段
            if (host.startsWith("10.") || host.startsWith("172.16.")
                    || host.startsWith("192.168.")) {
                return true;
            }

            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
