package com.pullwise.api.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * JPA AttributeConverter that transparently encrypts/decrypts sensitive
 * string values using AES-256-GCM before persisting to the database.
 */
@Slf4j
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ENCRYPTED_PREFIX = "ENC:";

    private static SecretKeySpec getKey() {
        String keyStr = System.getenv("PULLWISE_ENCRYPTION_KEY");
        if (keyStr == null || keyStr.isBlank()) {
            keyStr = "pullwise-dev-encryption-key-32b"; // 32 bytes for AES-256
        }
        byte[] keyBytes = keyStr.getBytes();
        // Ensure exactly 32 bytes for AES-256
        byte[] key = new byte[32];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));
        return new SecretKeySpec(key, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return attribute;
        }

        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), parameterSpec);
            byte[] encrypted = cipher.doFinal(attribute.getBytes());

            // Prepend IV to ciphertext
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            log.error("Failed to encrypt value", e);
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return dbData;
        }

        // Support reading plain text values (migration period)
        if (!dbData.startsWith(ENCRYPTED_PREFIX)) {
            return dbData;
        }

        try {
            String base64Data = dbData.substring(ENCRYPTED_PREFIX.length());
            byte[] decoded = Base64.getDecoder().decode(base64Data);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, getKey(), parameterSpec);

            return new String(cipher.doFinal(encrypted));
        } catch (Exception e) {
            log.error("Failed to decrypt value", e);
            throw new IllegalStateException("Decryption failed", e);
        }
    }
}
