package com.lin.webtemplate.service.service;

import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.stereotype.Service;

/**
 * 功能：管理员密码哈希与校验服务（PBKDF2）。
 *
 * @author linyi
 * @since 2026-02-19
 */
@Service
public class AdminPasswordHashService {

    private static final String PREFIX = "PBKDF2";

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";

    private static final int ITERATIONS = 65536;

    private static final int KEY_LENGTH = 256;

    private static final int SALT_BYTES = 16;

    public String hash(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] digest = pbkdf2(rawPassword, salt, ITERATIONS, KEY_LENGTH);
        return PREFIX + "$" + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(digest);
    }

    public boolean matches(String rawPassword,
                           String encodedHash) {
        if (encodedHash == null || encodedHash.isBlank()) {
            return false;
        }
        String[] parts = encodedHash.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }
        int iterations;
        try {
            iterations = Integer.parseInt(parts[1]);
        } catch (NumberFormatException ex) {
            return false;
        }
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expected = Base64.getDecoder().decode(parts[3]);
        byte[] actual = pbkdf2(rawPassword, salt, iterations, expected.length * 8);
        return constantTimeEquals(expected, actual);
    }

    private static byte[] pbkdf2(String rawPassword,
                                 byte[] salt,
                                 int iterations,
                                 int keyLength) {
        try {
            PBEKeySpec spec = new PBEKeySpec(rawPassword.toCharArray(), salt, iterations, keyLength);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("password hash failed", ex);
        }
    }

    private static boolean constantTimeEquals(byte[] expected,
                                              byte[] actual) {
        if (expected.length != actual.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < expected.length; i++) {
            result |= expected[i] ^ actual[i];
        }
        return result == 0;
    }
}
