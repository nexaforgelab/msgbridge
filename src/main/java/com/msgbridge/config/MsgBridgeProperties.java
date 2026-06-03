package com.msgbridge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "msgbridge")
public class MsgBridgeProperties {

    private final Security security = new Security();
    private final Worker worker = new Worker();
    private final Seed seed = new Seed();

    public Security getSecurity() {
        return security;
    }

    public Worker getWorker() {
        return worker;
    }

    public Seed getSeed() {
        return seed;
    }

    public static class Security {
        private String masterKey = "change-me-dev-master-key";
        private String adminKey = "dev-admin-key";
        private long timestampSkewSeconds = 300;
        private String defaultAdminUsername = "admin";
        private String defaultAdminPassword = "admin123";
        private long adminTokenTtlSeconds = 28800;

        public String getMasterKey() {
            return masterKey;
        }

        public void setMasterKey(String masterKey) {
            this.masterKey = masterKey;
        }

        public String getAdminKey() {
            return adminKey;
        }

        public void setAdminKey(String adminKey) {
            this.adminKey = adminKey;
        }

        public long getTimestampSkewSeconds() {
            return timestampSkewSeconds;
        }

        public void setTimestampSkewSeconds(long timestampSkewSeconds) {
            this.timestampSkewSeconds = timestampSkewSeconds;
        }

        public String getDefaultAdminUsername() {
            return defaultAdminUsername;
        }

        public void setDefaultAdminUsername(String defaultAdminUsername) {
            this.defaultAdminUsername = defaultAdminUsername;
        }

        public String getDefaultAdminPassword() {
            return defaultAdminPassword;
        }

        public void setDefaultAdminPassword(String defaultAdminPassword) {
            this.defaultAdminPassword = defaultAdminPassword;
        }

        public long getAdminTokenTtlSeconds() {
            return adminTokenTtlSeconds;
        }

        public void setAdminTokenTtlSeconds(long adminTokenTtlSeconds) {
            this.adminTokenTtlSeconds = adminTokenTtlSeconds;
        }
    }

    public static class Worker {
        private boolean enabled = true;
        private int batchSize = 20;
        private long fixedDelayMs = 1000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public long getFixedDelayMs() {
            return fixedDelayMs;
        }

        public void setFixedDelayMs(long fixedDelayMs) {
            this.fixedDelayMs = fixedDelayMs;
        }
    }

    public static class Seed {
        private boolean demoEnabled = true;

        public boolean isDemoEnabled() {
            return demoEnabled;
        }

        public void setDemoEnabled(boolean demoEnabled) {
            this.demoEnabled = demoEnabled;
        }
    }
}
