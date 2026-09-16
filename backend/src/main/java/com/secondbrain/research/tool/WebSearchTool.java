package com.secondbrain.research.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 外部网页搜索工具.
 *
 * <p>三级搜索回退策略：
 * 1. DuckDuckGo Instant Answer API（结构化即时答案）
 * 2. DuckDuckGo HTML 搜索页（通用网页搜索结果）
 * 3. 空结果（由上层 Agent 决定是否使用 LLM 回退）</p>
 *
 * <p>支持通过 research.proxy.* 配置 HTTP 代理，
 * 用于 DuckDuckGo 等搜索引擎在国内网络环境下的访问。</p>
 *
 * @author AI
 */
@Component
public class WebSearchTool implements Tool {

    private static final Logger log = LoggerFactory.getLogger(WebSearchTool.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final OkHttpClient httpClient;

    public WebSearchTool(
            @Value("${research.proxy.enabled:false}") boolean proxyEnabled,
            @Value("${research.proxy.host:127.0.0.1}") String proxyHost,
            @Value("${research.proxy.port:7890}") int proxyPort,
            @Value("${research.proxy.type:HTTP}") String proxyType) {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true);

        if (proxyEnabled && !proxyHost.isBlank()) {
            Proxy.Type type = "SOCKS".equalsIgnoreCase(proxyType)
                    ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
            Proxy proxy = new Proxy(type, new InetSocketAddress(proxyHost, proxyPort));
            builder.proxy(proxy);
            log.info("web_search_proxy_configured type={} host={} port={}", proxyType, proxyHost, proxyPort);
        }

        this.httpClient = builder.build();
    }

    @Override
    public String getName() {
        return "web_search";
    }

    @Override
    public String getDescription() {
        return "执行外部网页搜索，返回相关网页的标题、URL 和摘要";
    }

    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "query", Map.of("type", "string", "description", "搜索查询字符串"),
                "maxResults", Map.of("type", "integer", "description", "最大结果数，默认 10")
        ));
        schema.put("required", List.of("query"));
        return schema;
    }

    @Override
    public ToolResult execute(Map<String, Object> params) {
        String query = (String) params.get("query");
        if (query == null || query.isBlank()) {
            return ToolResult.failure("INVALID_PARAM", "query 不能为空", false);
        }

        int maxResults = params.get("maxResults") instanceof Number
                ? ((Number) params.get("maxResults")).intValue() : 10;

        long startMs = System.currentTimeMillis();

        try {
            // 三级回退：Instant Answer → HTML 搜索 → 空结果
            List<Map<String, String>> results = searchDuckDuckGoInstant(query, maxResults);

            if (results.isEmpty()) {
                log.info("ddg_instant_empty_trying_html query={}", query);
                results = searchDuckDuckGoHtml(query, maxResults);
            }

            long duration = System.currentTimeMillis() - startMs;

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < results.size(); i++) {
                Map<String, String> r = results.get(i);
                sb.append("[").append(i + 1).append("] ").append(r.get("title")).append("\n");
                sb.append("URL: ").append(r.get("url")).append("\n");
                sb.append("摘要: ").append(r.get("snippet")).append("\n\n");
            }

            ToolResult result = ToolResult.success(sb.toString());
            result.setDurationMs(duration);
            result.getMetadata().put("totalResults", results.size());
            result.getMetadata().put("query", query);
            result.getMetadata().put("rawResults", (java.io.Serializable) new ArrayList<>(results));
            return result;
        } catch (Exception e) {
            log.error("web_search_failed query={}", query, e);
            return ToolResult.failure("SEARCH_ERROR", e.getMessage(), true);
        }
    }

    /**
     * DuckDuckGo Instant Answer API（即时答案，仅对特定查询有效）.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, String>> searchDuckDuckGoInstant(String query, int maxResults)
            throws IOException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = "https://api.duckduckgo.com/?q=" + encodedQuery + "&format=json&no_html=1&skip_disambig=1";

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.warn("ddg_instant_http_error status={}", response.code());
                return List.of();
            }

            String body = response.body().string();
            Map<String, Object> json = objectMapper.readValue(body, Map.class);

            List<Map<String, String>> results = new ArrayList<>();

            // Abstract（即时答案文本）
            String abstractText = (String) json.get("AbstractText");
            String abstractUrl = (String) json.get("AbstractURL");
            if (abstractText != null && !abstractText.isBlank()) {
                Map<String, String> r = new HashMap<>();
                r.put("title", (String) json.getOrDefault("Heading", query));
                r.put("url", abstractUrl != null ? abstractUrl : "");
                r.put("snippet", abstractText);
                results.add(r);
            }

            // RelatedTopics（相关主题）
            List<Map<String, Object>> relatedTopics = (List<Map<String, Object>>)
                    json.get("RelatedTopics");
            if (relatedTopics != null) {
                for (Map<String, Object> topic : relatedTopics) {
                    if (results.size() >= maxResults) break;
                    String text = (String) topic.get("Text");
                    String firstUrl = (String) topic.get("FirstURL");
                    if (text != null && firstUrl != null && !firstUrl.isBlank()) {
                        Map<String, String> r = new HashMap<>();
                        r.put("title", text.length() > 100 ? text.substring(0, 100) + "..." : text);
                        r.put("url", firstUrl);
                        r.put("snippet", text);
                        results.add(r);
                    }
                }
            }

            // Answer（直接回答）
            Object answer = json.get("Answer");
            if (answer != null && !answer.toString().isBlank()) {
                Map<String, String> r = new HashMap<>();
                r.put("title", (String) json.getOrDefault("Heading", query));
                r.put("url", abstractUrl != null ? abstractUrl : "");
                r.put("snippet", answer.toString());
                results.add(r);
            }

            return results;
        }
    }

    /**
     * DuckDuckGo HTML 搜索页（通用网页搜索，解析 HTML 结果）.
     *
     * <p>当 Instant Answer API 无结果时，回退到 DuckDuckGo 的 HTML 搜索页。
     * 该页面对任意关键词都能返回搜索结果列表，解析其中的标题、URL 和摘要。</p>
     */
    private List<Map<String, String>> searchDuckDuckGoHtml(String query, int maxResults) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://html.duckduckgo.com/html/?q=" + encodedQuery;

            Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful() || response.body() == null) {
                    log.warn("ddg_html_http_error status={}", response.code());
                    return List.of();
                }

                String html = response.body().string();
                return parseDuckDuckGoHtml(html, maxResults);
            }
        } catch (Exception e) {
            log.warn("ddg_html_search_failed query={}", query, e);
            return List.of();
        }
    }

    /**
     * 解析 DuckDuckGo HTML 搜索结果页面.
     *
     * <p>DuckDuckGo HTML 页面结构：
     * <pre>
     *   <a class="result__a" href="URL">标题</a>
     *   <a class="result__snippet" href="URL">摘要文本</a>
     * </pre>
     * 同时提取 result__url 中的真实链接。</p>
     */
    private List<Map<String, String>> parseDuckDuckGoHtml(String html, int maxResults) {
        List<Map<String, String>> results = new ArrayList<>();

        // 提取所有 result__a 链接（标题 + URL）
        Pattern linkPattern = Pattern.compile(
                "<a[^>]*class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
                Pattern.DOTALL);
        Matcher linkMatcher = linkPattern.matcher(html);

        // 提取所有 result__snippet（摘要）
        Pattern snippetPattern = Pattern.compile(
                "<a[^>]*class=\"result__snippet\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
                Pattern.DOTALL);
        Matcher snippetMatcher = snippetPattern.matcher(html);

        // 提取所有 result__url（真实 URL）
        Pattern urlPattern = Pattern.compile(
                "<a[^>]*class=\"result__url\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
                Pattern.DOTALL);
        Matcher urlMatcher = urlPattern.matcher(html);

        while (linkMatcher.find() && results.size() < maxResults) {
            String rawUrl = linkMatcher.group(1);
            String title = stripHtmlTags(linkMatcher.group(2)).trim();

            // DuckDuckGo HTML 的 URL 是重定向链接，需要解析真实 URL
            String realUrl = resolveDuckDuckGoUrl(rawUrl);
            if (realUrl == null || realUrl.isBlank()) {
                realUrl = rawUrl;
            }

            Map<String, String> result = new HashMap<>();
            result.put("title", title);
            result.put("url", realUrl);

            // 匹配对应的摘要
            if (snippetMatcher.find()) {
                String snippet = stripHtmlTags(snippetMatcher.group(2)).trim();
                if (snippet.length() > 300) {
                    snippet = snippet.substring(0, 300) + "...";
                }
                result.put("snippet", snippet);
            } else {
                result.put("snippet", "");
            }

            results.add(result);
        }

        log.info("ddg_html_parsed count={}", results.size());
        return results;
    }

    /**
     * 解析 DuckDuckGo HTML 重定向链接中的真实 URL.
     *
     * <p>DuckDuckGo HTML 页面的 result__a href 通常是：
     * {@code //duckduckgo.com/l/?uddg=REAL_URL}，
     * 需要提取 uddg 参数中的真实 URL 并仅做一次 URLDecode。</p>
     */
    private String resolveDuckDuckGoUrl(String rawUrl) {
        if (rawUrl == null) return null;

        // 直接是 http(s) 链接
        if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) {
            return rawUrl;
        }

        // 以 // 开头的协议相对链接，补全协议
        if (rawUrl.startsWith("//")) {
            // 包含 uddg 参数的 DuckDuckGo 重定向
            int uddgIdx = rawUrl.indexOf("uddg=");
            if (uddgIdx >= 0) {
                String encoded = rawUrl.substring(uddgIdx + 5);
                int ampIdx = encoded.indexOf('&');
                if (ampIdx > 0) {
                    encoded = encoded.substring(0, ampIdx);
                }
                try {
                    // 仅做一次 decode，还原真实 URL
                    return java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    return encoded;
                }
            }
            // 非重定向型协议相对链接，补全 https:
            return "https:" + rawUrl;
        }

        // 无前缀但包含 uddg 参数
        int uddgIdx = rawUrl.indexOf("uddg=");
        if (uddgIdx >= 0) {
            String encoded = rawUrl.substring(uddgIdx + 5);
            int ampIdx = encoded.indexOf('&');
            if (ampIdx > 0) {
                encoded = encoded.substring(0, ampIdx);
            }
            try {
                return java.net.URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            } catch (Exception e) {
                return encoded;
            }
        }

        return rawUrl;
    }

    /**
     * 去除 HTML 标签.
     */
    private String stripHtmlTags(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]*>", "").replace("&nbsp;", " ")
                .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                .replaceAll("\\s+", " ").trim();
    }
}
