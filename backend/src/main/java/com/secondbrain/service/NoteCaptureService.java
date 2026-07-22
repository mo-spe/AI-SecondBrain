package com.secondbrain.service;

import com.secondbrain.dto.NoteCaptureRequest;

/**
 * 笔记采集服务接口.
 * <p>提供笔记内容的采集和处理功能</p>
 */
public interface NoteCaptureService {

    /**
     * 采集Markdown笔记.
     *
     * @param request 笔记请求
     * @return 处理结果
     */
    String captureMarkdownNote(NoteCaptureRequest request);
}
