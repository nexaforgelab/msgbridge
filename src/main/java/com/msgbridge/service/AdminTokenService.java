package com.msgbridge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.msgbridge.config.MsgBridgeProperties;
import com.msgbridge.core.AdminRole;
import com.msgbridge.crypto.HmacSigner;
import com.msgbridge.security.AdminPrincipal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AdminTokenService {
    private final MsgBridgeProperties properties;
    private final HmacSigner hmacSigner;
    private final ObjectMapper objectMapper;

    public AdminTokenService(MsgBridgeProperties properties, HmacSigner hmacSigner, ObjectMapper objectMapper) {
        this.properties = properties;
        this.hmacSigner = hmacSigner;
        this.objectMapper = objectMapper;
    }

    public TokenIssue issue(String username, String displayName, AdminRole role) {
        Instant expiresAt = Instant.now().plusSeconds(properties.getSecurity().getAdminTokenTtlSeconds());
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "MB_ADMIN");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("name", displayName);
        payload.put("role", role.name());
        payload.put("exp", expiresAt.getEpochSecond());
        String signingInput = encode(header) + "." + encode(payload);
        String signature = hmacSigner.hmacSha256Hex(signingSecret(), signingInput);
        return new TokenIssue(signingInput + "." + signature, expiresAt);
    }

    public Optional<AdminPrincipal> verify(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        String signingInput = parts[0] + "." + parts[1];
        String expected = hmacSigner.hmacSha256Hex(signingSecret(), signingInput);
        if (!hmacSigner.constantTimeEquals(expected, parts[2])) {
            return Optional.empty();
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(
                    Base64.getUrlDecoder().decode(parts[1]),
                    new TypeReference<Map<String, Object>>() {
                    });
            long exp = Long.parseLong(String.valueOf(payload.get("exp")));
            if (Instant.now().getEpochSecond() > exp) {
                return Optional.empty();
            }
            return Optional.of(new AdminPrincipal(
                    String.valueOf(payload.get("sub")),
                    String.valueOf(payload.get("name")),
                    AdminRole.valueOf(String.valueOf(payload.get("role")))));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String encode(Object value) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception e) {
            throw new IllegalStateException("token encoding failed", e);
        }
    }

    private String signingSecret() {
        return properties.getSecurity().getMasterKey()
                + ":"
                + properties.getSecurity().getAdminKey()
                + ":admin-token";
    }

    public record TokenIssue(String token, Instant expiresAt) {
    }
}
