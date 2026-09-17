package com.secondbrain.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.secondbrain.dto.LoginResponseDTO;
import com.secondbrain.entity.User;
import com.secondbrain.entity.UserOAuthIdentity;
import com.secondbrain.exception.BusinessException;
import com.secondbrain.mapper.UserMapper;
import com.secondbrain.mapper.UserOAuthIdentityMapper;
import com.secondbrain.service.WechatAuthService;
import com.secondbrain.util.JwtUtil;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 微信小程序登录服务实现。
 *
 * 微信 openid 只作为独立身份映射的服务端关联键保存，session_key 永不落库，避免将平台会话凭证变成长期敏感数据。
 *
 * @author AI
 */
@Service
public class WechatAuthServiceImpl implements WechatAuthService {

    private static final String PROVIDER = "wechat";
    private static final String SESSION_URL = "https://api.weixin.qq.com/sns/jscode2session";

    private final UserMapper userMapper;
    private final UserOAuthIdentityMapper identityMapper;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Value("${wechat.app-id:}")
    private String appId;

    @Value("${wechat.app-secret:}")
    private String appSecret;

    public WechatAuthServiceImpl(UserMapper userMapper,
                                UserOAuthIdentityMapper identityMapper,
                                JwtUtil jwtUtil,
                                @Qualifier("oauthRestTemplate") RestTemplate restTemplate) {
        this.userMapper = userMapper;
        this.identityMapper = identityMapper;
        this.jwtUtil = jwtUtil;
        this.restTemplate = restTemplate;
    }

    /**
     * 向微信换取 openid，并将其绑定到现有或新建的本地账户。
     *
     * @param code 微信一次性登录凭证
     * @return JWT 和用户信息
     */
    @Override
    @Transactional
    public LoginResponseDTO login(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(400, "微信登录凭证不能为空");
        }
        if (!StringUtils.hasText(appId) || !StringUtils.hasText(appSecret)) {
            throw new BusinessException(503, "微信登录尚未配置，请联系管理员");
        }
        WechatSessionResponse session = exchangeCode(code);
        if (session == null || (session.errcode != null && session.errcode != 0)
                || !StringUtils.hasText(session.openid)) {
            throw new BusinessException(401, "微信登录凭证无效或已过期");
        }

        User user = findUserByOpenId(session.openid);
        if (user == null) {
            user = createUser(session.openid);
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }
        return toLoginResponse(user);
    }

    private WechatSessionResponse exchangeCode(String code) {
        URI uri = UriComponentsBuilder.fromUriString(SESSION_URL)
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUri();
        try {
            ResponseEntity<WechatSessionResponse> response = restTemplate.getForEntity(uri, WechatSessionResponse.class);
            return response.getBody();
        } catch (RuntimeException ex) {
            throw new BusinessException(502, "微信登录服务暂时不可用", ex);
        }
    }

    private User findUserByOpenId(String openid) {
        UserOAuthIdentity identity = identityMapper.selectOne(new LambdaQueryWrapper<UserOAuthIdentity>()
                .eq(UserOAuthIdentity::getProvider, PROVIDER)
                .eq(UserOAuthIdentity::getProviderUserId, openid));
        return identity == null ? null : userMapper.selectById(identity.getUserId());
    }

    private User createUser(String openid) {
        User user = new User();
        user.setUsername("wx_" + hashOpenId(openid).substring(0, 16));
        // 账号密码登录与微信登录共用用户表；随机密码只用于满足旧表约束，用户无法通过它反向登录。
        user.setPassword(passwordEncoder.encode(hashOpenId(openid)));
        user.setRole("USER");
        user.setStatus(1);
        userMapper.insert(user);

        UserOAuthIdentity identity = new UserOAuthIdentity();
        identity.setUserId(user.getId());
        identity.setProvider(PROVIDER);
        identity.setProviderUserId(openid);
        try {
            identityMapper.insert(identity);
        } catch (DuplicateKeyException ex) {
            // 并发首次登录时以已落库映射为准，避免一个微信身份创建两个账户。
            User existing = findUserByOpenId(openid);
            if (existing != null) {
                return existing;
            }
            throw ex;
        }
        return user;
    }

    private String hashOpenId(String openid) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(openid.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 不可用", ex);
        }
    }

    private LoginResponseDTO toLoginResponse(User user) {
        LoginResponseDTO response = new LoginResponseDTO();
        response.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole(), null));

        LoginResponseDTO.UserInfo userInfo = new LoginResponseDTO.UserInfo();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setEmail(user.getEmail());
        userInfo.setPhone(user.getPhone());
        userInfo.setBio(user.getBio());
        userInfo.setRole(user.getRole());
        response.setUserInfo(userInfo);
        return response;
    }

    @Getter
    @Setter
    private static class WechatSessionResponse {
        /** 微信错误码，成功时为空或为0。 */
        private Integer errcode;
        /** 微信错误信息，仅用于服务端诊断，不回传给客户端。 */
        private String errmsg;
        /** 微信用户 openid。 */
        private String openid;
    }
}
