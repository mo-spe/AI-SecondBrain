package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 排行榜 VO.
 */
@Getter
@Setter
public class LeaderboardVO {

    /**
     * 排行榜条目列表
     */
    private List<LeaderboardEntryVO> entries;

    /**
     * 当前用户的排名信息（可能不在 entries 中）
     */
    private LeaderboardEntryVO currentUser;
}
