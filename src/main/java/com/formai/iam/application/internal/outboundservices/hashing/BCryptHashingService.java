package com.formai.iam.application.internal.outboundservices.hashing;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
public class BCryptHashingService implements HashingService {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(preHash(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, String hashedPassword) {
        return encoder.matches(preHash(rawPassword), hashedPassword);
    }

    // BCryptPasswordEncoder rejects inputs longer than 72 bytes, while the password
    // policy (User.MAX_PASSWORD_LENGTH) allows up to 128 characters. SHA-256 + Base64
    // always yields 44 bytes, so any password within the policy can be hashed.
    private String preHash(String rawPassword) {
        try {
            var digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", e);
        }
    }
}
