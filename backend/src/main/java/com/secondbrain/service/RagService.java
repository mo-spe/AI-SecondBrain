package com.secondbrain.service;

import com.secondbrain.dto.RagRequest;
import com.secondbrain.dto.RagResponse;

/**
 * RAG问答服务接口.
 * <p>基于知识库内容进行问答</p>
 */
public interface RagService {

    /**
     * 回答用户问题.
     *
     * @param request 问答请求
     * @param userId 用户ID
     * @return 问答响应
     */
    RagResponse answer(RagRequest request, Long userId);

    /**
     * 回答用户问题（带API Key）.
     *
     * @param request 问答请求
     * @param userId 用户ID
     * @param userApiKey 用户API Key
     * @return 问答响应
     */
    RagResponse answer(RagRequest request, Long userId, String userApiKey);
}
