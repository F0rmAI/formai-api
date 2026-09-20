package com.formai.iam.application.internal.outboundservices.hashing;

public interface HashingService {

    String hash(String rawPassword);

    boolean matches(String rawPassword, String hashedPassword);
}
