package com.secondbrain.service.impl;

import com.secondbrain.service.ApiKeyEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 加解密服务实现.
 * <p>使用AES-256-GCM算法，密钥从 application.yml 的 ai.encryption.key 读取。
 * 密文格式为 Base64(IV[12字节] + Ciphertext)</p>
 */
@Service
public class ApiKeyEncryptionServiceImpl implements ApiKeyEncryptionService {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyEncryptionServiceImpl.class);

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int KEY_LENGTH = 32;

    private final byte[] encryptionKey;

    public ApiKeyEncryptionServiceImpl(
            @Value("${ai.encryption.key}") String configuredKey) {
        this.encryptionKey = normalizeKey(configuredKey.getBytes(StandardCharsets.UTF_8));
        log.info("加密密钥已加载，长度={}字节", this.encryptionKey.length);
    }

    /**
     * 将用户配置的密钥标准化为32字节.
     * <p>长度不足则补0，过长则截断</p>
     */
    private byte[] normalizeKey(byte[] keyBytes) {
        if (keyBytes.length == KEY_LENGTH) {
            return keyBytes;
        }
        byte[] normalized = new byte[KEY_LENGTH];
        System.arraycopy(keyBytes, 0, normalized, 0, Math.min(keyBytes.length, KEY_LENGTH));
        log.warn("ai.encryption.key 长度={}，不是32字节，已自动调整", keyBytes.length);
        return normalized;
    }

    @Override
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return null;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // IV(12) + Ciphertext
            byte[] combined = new byte[GCM_IV_LENGTH + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, GCM_IV_LENGTH);
            System.arraycopy(ciphertext, 0, combined, GCM_IV_LENGTH, ciphertext.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("API Key 加密失败", e);
            throw new RuntimeException("API Key 加密失败", e);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isEmpty()) {
            return null;
        }

        try {
            byte[] combined = Base64.getDecoder().decode(cipherText);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(combined, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey, ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plainBytes = cipher.doFinal(ciphertext);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("API Key 解密失败", e);
            throw new RuntimeException("API Key 解密失败", e);
        }
    }
}
