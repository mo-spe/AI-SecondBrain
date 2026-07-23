package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

/** 报告请求DTO. <p>用于学习报告生成的请求参数封装</p> */
@Getter
@Setter
public class ReportRequest {

    /**
     * 报告主题
     */
    private String topic;

    /**
     * 学习天数
     */
    private Integer days;
}
