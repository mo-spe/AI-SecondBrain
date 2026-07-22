/**
 * HTTP请求日志拦截器
 *
 * 拦截RestTemplate发出的HTTP请求，记录请求和响应的详细信息，
 * 用于调试和监控外部API调用。
 */
package com.secondbrain.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    /**
     * 拦截HTTP请求并记录日志
     *
     * @param request   HTTP请求对象
     * @param body      请求体字节数组
     * @param execution 请求执行器
     * @return HTTP响应对象
     * @throws IOException 请求执行异常
     */
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        log.info("========== HTTP请求拦截开始 ==========");
        log.info("发送HTTP请求：{} {}", request.getMethod(), request.getURI());
        log.info("请求头：{}", request.getHeaders());
        log.info("请求体：{}", new String(body, StandardCharsets.UTF_8));

        ClientHttpResponse response;
        try {
            long startTime = System.currentTimeMillis();
            response = execution.execute(request, body);
            long endTime = System.currentTimeMillis();
            log.info("HTTP请求执行时间：{} ms", endTime - startTime);
        } catch (Exception e) {
            log.error("HTTP请求执行失败", e);
            throw e;
        }

        log.info("响应状态码：{}", response.getStatusCode());
        log.info("响应头：{}", response.getHeaders());

        try {
            String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
            log.info("响应体（前500字符）：{}", responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody);
            log.info("响应体长度：{}", responseBody.length());
            log.info("响应体开始字符：{}", responseBody.length() > 20 ? responseBody.substring(0, 20) : responseBody);
            log.info("========== HTTP请求拦截完成 ==========");

            return new BufferedClientHttpResponse(response, responseBody);
        } catch (Exception e) {
            log.error("读取响应体失败", e);
            throw e;
        }
    }
}
