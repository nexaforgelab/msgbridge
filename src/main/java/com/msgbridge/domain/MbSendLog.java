package com.msgbridge.domain;

import com.msgbridge.core.ChannelType;
import com.msgbridge.core.SendStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "mb_send_log")
public class MbSendLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", nullable = false, length = 64)
    private String messageId;

    @Column(name = "channel_id", nullable = false)
    private Long channelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", length = 64)
    private ChannelType channelType;

    @Lob
    @Column(name = "platform_request")
    private String platformRequest;

    @Lob
    @Column(name = "platform_response")
    private String platformResponse;

    @Column(name = "platform_code", length = 64)
    private String platformCode;

    @Column(name = "platform_message", length = 512)
    private String platformMessage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private SendStatus status;

    @Column(name = "retryable")
    private Integer retryable = 0;

    @Column(name = "cost_ms")
    private Integer costMs;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getMessageId() {
        return messageId;
    }

    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    public Long getChannelId() {
        return channelId;
    }

    public void setChannelId(Long channelId) {
        this.channelId = channelId;
    }

    public ChannelType getChannelType() {
        return channelType;
    }

    public void setChannelType(ChannelType channelType) {
        this.channelType = channelType;
    }

    public String getPlatformRequest() {
        return platformRequest;
    }

    public void setPlatformRequest(String platformRequest) {
        this.platformRequest = platformRequest;
    }

    public String getPlatformResponse() {
        return platformResponse;
    }

    public void setPlatformResponse(String platformResponse) {
        this.platformResponse = platformResponse;
    }

    public String getPlatformCode() {
        return platformCode;
    }

    public void setPlatformCode(String platformCode) {
        this.platformCode = platformCode;
    }

    public String getPlatformMessage() {
        return platformMessage;
    }

    public void setPlatformMessage(String platformMessage) {
        this.platformMessage = platformMessage;
    }

    public SendStatus getStatus() {
        return status;
    }

    public void setStatus(SendStatus status) {
        this.status = status;
    }

    public Integer getRetryable() {
        return retryable;
    }

    public void setRetryable(Integer retryable) {
        this.retryable = retryable;
    }

    public Integer getCostMs() {
        return costMs;
    }

    public void setCostMs(Integer costMs) {
        this.costMs = costMs;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
