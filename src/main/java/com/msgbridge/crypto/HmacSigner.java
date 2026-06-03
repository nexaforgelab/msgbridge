package com.msgbridge.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class HmacSigner {

    public String hmacSha256Hex(String secret, String data) {
        return toHex(hmacSha256(secret.getBytes(StandardCharsets.UTF_8), data.getBytes(StandardCharsets.UTF_8)));
    }

    public String hmacSha256Base64(String secret, String data) {
        return Base64.getEncoder().encodeToString(
                hmacSha256(secret.getBytes(StandardCharsets.UTF_8), data.getBytes(StandardCharsets.UTF_8)));
    }

    public byte[] hmacSha256(byte[] secret, byte[] data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    public boolean constantTimeEquals(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8));
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
