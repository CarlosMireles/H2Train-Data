package com.h2traindata.infrastructure.datalake;

import com.h2traindata.infrastructure.config.DatalakeProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.springframework.stereotype.Component;

@Component
public class SubjectIdResolver {

    private final DatalakeProperties datalakeProperties;

    public SubjectIdResolver(DatalakeProperties datalakeProperties) {
        this.datalakeProperties = datalakeProperties;
    }

    public String resolve(String providerId, String athleteId) {
        String input = datalakeProperties.getSubjectIdSalt() + ":" + providerId + ":" + athleteId;
        return "subj_" + sha256Hex(input).substring(0, 24);
    }

    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available in the runtime", exception);
        }
    }
}
