package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 举报帖子请求DTO.
 */
@Getter
@Setter
public class SquareReportRequest {

    /**
     * 举报原因（≤500字）
     */
    @NotBlank(message = "举报原因不能为空")
    @Size(max = 500, message = "举报原因不能超过500字")
    private String reason;
}
