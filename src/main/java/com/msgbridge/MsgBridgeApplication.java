package com.msgbridge;

import com.msgbridge.config.MsgBridgeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableConfigurationProperties(MsgBridgeProperties.class)
public class MsgBridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsgBridgeApplication.class, args);
    }
}
