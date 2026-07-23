package com.secondbrain.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** 工作区成员响应DTO. <p>用于返回工作区成员信息</p> */
@Getter
@Setter
public class MemberResponse {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户头像URL
     */
    private String avatar;

    /**
     * 在工作区中的角色
     */
    private String role;

    /**
     * 加入时间
     */
    private LocalDateTime joinedTime;
}
