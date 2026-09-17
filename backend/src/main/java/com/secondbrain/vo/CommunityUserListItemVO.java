package com.secondbrain.vo;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 粉丝与关注列表中的公开用户摘要。 */
@Getter
@Setter
public class CommunityUserListItemVO {

    /** 用户ID。 */
    private Long userId;

    /** 公开昵称。 */
    private String username;

    /** 头像地址。 */
    private String avatar;

    /** 社区公开简介。 */
    private String introduction;

    /** 擅长领域。 */
    private List<String> expertiseTags;

    /** 当前访问者是否已关注。 */
    private Boolean isFollowing;

    /** 当前访问者与该用户间是否存在拉黑关系。 */
    private Boolean isBlocked;
}
