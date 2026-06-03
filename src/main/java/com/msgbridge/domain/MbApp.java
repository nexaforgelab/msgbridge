package com.msgbridge.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "mb_app")
public class MbApp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "app_id", nullable = false, unique = true, length = 64)
    private String appId;

    @Column(name = "app_name", nullable = false, length = 128)
    private String appName;

    @Column(name = "app_secret_encrypted", nullable = false, length = 1024)
    private String appSecretEncrypted;

    @Column(name = "sign_type", length = 32)
    private String signType = "HMAC_SHA256";

    @Column(name = "ip_whitelist")
    private String ipWhitelist;

    @Column(name = "rate_limit_per_min")
    private Integer rateLimitPerMin = 600;

    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "owner_name", length = 64)
    private String ownerName;

    @Column(name = "owner_contact", length = 128)
    private String ownerContact;

    @Column(name = "remark", length = 512)
    private String remark;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppSecretEncrypted() {
        return appSecretEncrypted;
    }

    public void setAppSecretEncrypted(String appSecretEncrypted) {
        this.appSecretEncrypted = appSecretEncrypted;
    }

    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    public String getIpWhitelist() {
        return ipWhitelist;
    }

    public void setIpWhitelist(String ipWhitelist) {
        this.ipWhitelist = ipWhitelist;
    }

    public Integer getRateLimitPerMin() {
        return rateLimitPerMin;
    }

    public void setRateLimitPerMin(Integer rateLimitPerMin) {
        this.rateLimitPerMin = rateLimitPerMin;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getOwnerContact() {
        return ownerContact;
    }

    public void setOwnerContact(String ownerContact) {
        this.ownerContact = ownerContact;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
