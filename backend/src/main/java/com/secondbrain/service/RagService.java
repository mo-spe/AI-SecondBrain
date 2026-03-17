package com.secondbrain.service;

import com.secondbrain.dto.RagRequest;
import com.secondbrain.dto.RagResponse;

public interface RagService {
    
    RagResponse answer(RagRequest request, Long userId);

    RagResponse answer(RagRequest request, Long userId, String userApiKey);
}
