package com.secondbrain.service;

import com.secondbrain.entity.User;

public interface UserService {

    User getUserById(Long userId);

    User getUserByUsername(String username);

    void updateUser(Long userId, String username, String email, String phone, String bio, String apiKey);

    void updatePassword(Long userId, String oldPassword, String newPassword);

    void updateLastLoginTime(Long userId);

    void updateAvatar(Long userId, String avatarUrl);
}
