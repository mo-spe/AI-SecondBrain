package com.secondbrain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 处理举报请求DTO.
 */
@Getter
@Setter
public class SquareHandleReportRequest {

    /**
     * 处理方式：ignore（忽略）或 remove（移除）
     */
    @NotBlank(message = "处理方式不能为空")
    private String action;

    /**
     * 处理备注（≤500字）
     */
    @Size(max = 500, message = "处理备注不能超过500字")
    private String handleNote;
}
