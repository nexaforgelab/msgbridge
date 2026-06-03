package com.msgbridge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:msgbridge_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE",
        "msgbridge.worker.enabled=false"
})
class MsgBridgeApplicationTests {

    @Test
    void contextLoads() {
    }
}
