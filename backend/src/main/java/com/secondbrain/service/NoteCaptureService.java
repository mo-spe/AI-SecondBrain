package com.secondbrain.service;

import com.secondbrain.dto.NoteCaptureRequest;

public interface NoteCaptureService {

    String captureMarkdownNote(NoteCaptureRequest request);
}
