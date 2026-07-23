package com.secondbrain.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/** HTTP响应缓冲包装类. <p>包装ClientHttpResponse并缓冲响应体以便多次读取</p> */
public class BufferedClientHttpResponse implements ClientHttpResponse {

    private final ClientHttpResponse delegate;
    private final byte[] body;

    /**
     * 构造缓冲响应对象.
     *
     * @param delegate 被包装的原始响应
     * @param body     响应体字符串
     */
    public BufferedClientHttpResponse(ClientHttpResponse delegate, String body) {
        this.delegate = delegate;
        this.body = body.getBytes();
    }

    /**
     * 获取响应状态码.
     *
     * @return HTTP 状态码
     * @throws IOException 读取异常
     */
    @Override
    public HttpStatusCode getStatusCode() throws IOException {
        return delegate.getStatusCode();
    }

    /**
     * 获取原始状态码数值.
     *
     * @return 原始状态码
     * @throws IOException 读取异常
     */
    @Override
    public int getRawStatusCode() throws IOException {
        return delegate.getRawStatusCode();
    }

    /**
     * 获取状态文本.
     *
     * @return 状态文本
     * @throws IOException 读取异常
     */
    @Override
    public String getStatusText() throws IOException {
        return delegate.getStatusText();
    }

    /**
     * 关闭响应.
     *
     * @return void
     */
    @Override
    public void close() {
        delegate.close();
    }

    /**
     * 获取缓冲的响应体输入流.
     *
     * @return 响应体输入流
     * @throws IOException 读取异常
     */
    @Override
    public InputStream getBody() throws IOException {
        return new ByteArrayInputStream(body);
    }

    /**
     * 获取响应头.
     *
     * @return HTTP 响应头
     */
    @Override
    public HttpHeaders getHeaders() {
        return delegate.getHeaders();
    }
}
