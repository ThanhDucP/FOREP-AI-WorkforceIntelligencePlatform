package com.aiworkforce.core.service;

import com.aiworkforce.core.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Service
public class TokenProtectionService {

    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();
    private final SecretKeySpec keySpec;

    public TokenProtectionService(@Value("${app.security.token-protection.key:dev-change-me-token-protection-key}") String key) {
        this.keySpec = new SecretKeySpec(deriveKey(key), "AES");
    }

    public String protect(String raw) {
        if (raw == null || raw.isBlank() || raw.startsWith(PREFIX)) {
            return raw;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(raw.getBytes(StandardCharsets.UTF_8));
            return PREFIX
                    + Base64.getEncoder().encodeToString(iv)
                    + ":"
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new BusinessException("Unable to protect integration token");
        }
    }

    public String unprotect(String protectedValue) {
        if (protectedValue == null || protectedValue.isBlank()) {
            return protectedValue;
        }
        if (!protectedValue.startsWith(PREFIX)) {
            return protectedValue;
        }
        try {
            String payload = protectedValue.substring(PREFIX.length());
            String[] parts = payload.split(":", 2);
            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] encrypted = Base64.getDecoder().decode(parts[1]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new BusinessException("Unable to unprotect integration token. Check security.token-protection.key.");
        }
    }

    private byte[] deriveKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return Arrays.copyOf(digest.digest(key.getBytes(StandardCharsets.UTF_8)), 32);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to initialize token protection key", e);
        }
    }
}