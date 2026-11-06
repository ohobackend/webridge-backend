package com.webridge.auth.password;

import org.springframework.security.crypto.password.PasswordEncoder;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class DjangoPbkdf2PasswordEncoder implements PasswordEncoder {

    private static final int DEFAULT_ITERATIONS = 1_000_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String PREFIX = "pbkdf2_sha256";
    private static final String SALT_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final SecureRandom random = new SecureRandom();

    @Override
    public String encode(CharSequence rawPassword) {
        String salt = randomSalt(22);
        return PREFIX + "$" + DEFAULT_ITERATIONS + "$" + salt + "$" + hash(rawPassword, salt, DEFAULT_ITERATIONS);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        // Reject unusable passwords (set_unusable_password starts with '!')
        if (encodedPassword.startsWith("!")) {
            return false;
        }

        String[] parts = encodedPassword.split("\\$");
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            String salt = parts[2];
            String expectedHash = parts[3];
            String computedHash = hash(rawPassword, salt, iterations);

            return MessageDigest.isEqual(
                computedHash.getBytes(StandardCharsets.US_ASCII),
                expectedHash.getBytes(StandardCharsets.US_ASCII)
            );
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String hash(CharSequence rawPassword, String salt, int iterations) {
        try {
            var spec = new PBEKeySpec(
                rawPassword.toString().toCharArray(),
                salt.getBytes(StandardCharsets.UTF_8),
                iterations,
                KEY_LENGTH_BITS
            );
            byte[] encoded = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(encoded);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to calculate PBKDF2 hash", e);
        }
    }

    private String randomSalt(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(SALT_CHARS.charAt(random.nextInt(SALT_CHARS.length())));
        }
        return sb.toString();
    }
}
