package com.msgbridge.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HmacSignerTest {

    @Test
    void signsCanonicalPayloadAsLowerHex() {
        HmacSigner signer = new HmacSigner();

        String signature = signer.hmacSha256Hex("key", "The quick brown fox jumps over the lazy dog");

        assertThat(signature).isEqualTo("f7bc83f430538424b13298e6aa6fb143ef4d59a14946175997479dbc2d1a3cd8");
    }
}
