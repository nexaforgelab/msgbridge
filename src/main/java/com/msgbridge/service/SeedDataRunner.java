package com.msgbridge.service;

import com.msgbridge.config.MsgBridgeProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class SeedDataRunner implements ApplicationRunner {
    private final MsgBridgeProperties properties;
    private final AppService appService;
    private final AdminUserService adminUserService;

    public SeedDataRunner(MsgBridgeProperties properties, AppService appService, AdminUserService adminUserService) {
        this.properties = properties;
        this.appService = appService;
        this.adminUserService = adminUserService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.getSeed().isDemoEnabled()) {
            appService.ensureDemoApp();
            adminUserService.ensureDefaultAdmin();
        }
    }
}
