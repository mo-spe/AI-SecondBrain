package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 积分流水 VO.
 */
@Getter
@Setter
public class PointsLogVO {

    private Long id;
    private Integer points;
    private String type;
    private String description;
    private LocalDateTime createTime;
}
