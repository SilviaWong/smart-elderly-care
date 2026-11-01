package com.example.elderlycare.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Slf4j
@Component
public class SecurityCryptoUtil {

    private final SecretKeySpec secretKeySpec;

    public SecurityCryptoUtil(
            @Value("${crypto.aes-key:ElderlyCareAesSecretKey32BytesLong!}") String aesKey) {
        byte[] keyBytes = aesKey.getBytes(StandardCharsets.UTF_8);
        byte[] fixedKey = new byte[16]; // 128-bit key for standard compatibility
        System.arraycopy(keyBytes, 0, fixedKey, 0, Math.min(keyBytes.length, fixedKey.length));
        this.secretKeySpec = new SecretKeySpec(fixedKey, "AES");
    }

    /**
     * 对称加密敏感信息（如身份证号码）
     */
    public String encrypt(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            log.error("数据加密失败: {}", e.getMessage());
            return plainText;
        }
    }

    /**
     * 对称解密敏感信息
     */
    public String decrypt(String cipherText) {
        if (!StringUtils.hasText(cipherText)) {
            return null;
        }
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("数据解密失败或非有效密文: {}", e.getMessage());
            return cipherText;
        }
    }

    /**
     * 生成不可逆 SHA-256 哈希索引，用于加密列的精确等值检索
     */
    public String sha256Hex(String plainText) {
        if (!StringUtils.hasText(plainText)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(plainText.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("计算 SHA-256 哈希失败: {}", e.getMessage());
            return null;
        }
    }
}
