package com.secondbrain.service;

/**
 * API Key 加解密服务.
 * <p>使用AES-256-GCM对用户API Key进行加密存储和解密读取</p>
 */
public interface ApiKeyEncryptionService {

    /**
     * 加密明文API Key.
     *
     * @param plainText 明文
     * @return Base64编码的密文（含IV）
     */
    String encrypt(String plainText);

    /**
     * 解密密文API Key.
     *
     * @param cipherText Base64编码的密文（含IV）
     * @return 明文
     */
    String decrypt(String cipherText);
}
