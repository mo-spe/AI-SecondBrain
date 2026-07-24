package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * 排行榜单条条目 VO.
 */
@Getter
@Setter
public class LeaderboardEntryVO {

    private Integer rank;
    private Long userId;
    private String username;
    private String avatar;
    private Long score;
    private Integer level;
}
