package com.secondbrain.service;

import com.secondbrain.entity.User;

/**
 * 用户服务接口.
 * <p>提供用户信息查询、更新等功能</p>
 */
public interface UserService {

    /**
     * 根据ID查询用户.
     *
     * @param userId 用户ID
     * @return 用户信息
     */
    User getUserById(Long userId);

    /**
     * 根据用户名查询用户.
     *
     * @param username 用户名
     * @return 用户信息
     */
    User getUserByUsername(String username);

    /**
     * 更新用户信息.
     *
     * @param userId 用户ID
     * @param username 用户名
     * @param email 邮箱
     * @param phone 手机号
     * @param bio 个人简介
     * @param apiKey API Key
     */
    void updateUser(Long userId, String username, String email, String phone, String bio, String apiKey);

    /**
     * 更新密码.
     *
     * @param userId 用户ID
     * @param oldPassword 原密码
     * @param newPassword 新密码
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 更新最后登录时间.
     *
     * @param userId 用户ID
     */
    void updateLastLoginTime(Long userId);

    /**
     * 更新头像.
     *
     * @param userId 用户ID
     * @param avatarUrl 头像URL
     */
    void updateAvatar(Long userId, String avatarUrl);
}
