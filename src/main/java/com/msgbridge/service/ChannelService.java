package com.msgbridge.service;

import com.msgbridge.core.BusinessException;
import com.msgbridge.core.Constants;
import com.msgbridge.crypto.CryptoService;
import com.msgbridge.domain.MbChannel;
import com.msgbridge.domain.MbSendLog;
import com.msgbridge.dto.ChannelHealthResponse;
import com.msgbridge.dto.ChannelResponse;
import com.msgbridge.dto.CreateChannelRequest;
import com.msgbridge.repository.SendLogRepository;
import com.msgbridge.repository.ChannelRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ChannelService {
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "webhook_url", "webhookurl", "webhook",
            "secret", "sign_secret", "signsecret",
            "access_token", "accesstoken", "token",
            "robot_key", "robotkey",
            "corp_secret", "corpsecret",
            "app_secret", "appsecret");

    private final ChannelRepository channelRepository;
    private final CryptoService cryptoService;
    private final JsonService jsonService;
    private final AuditLogService auditLogService;
    private final SendLogRepository sendLogRepository;

    public ChannelService(
            ChannelRepository channelRepository,
            CryptoService cryptoService,
            JsonService jsonService,
            AuditLogService auditLogService,
            SendLogRepository sendLogRepository) {
        this.channelRepository = channelRepository;
        this.cryptoService = cryptoService;
        this.jsonService = jsonService;
        this.auditLogService = auditLogService;
        this.sendLogRepository = sendLogRepository;
    }

    @Transactional
    public ChannelResponse create(CreateChannelRequest request) {
        if (channelRepository.existsByChannelCode(request.channelCode())) {
            throw BusinessException.conflict("channelCode already exists");
        }
        MbChannel channel = new MbChannel();
        apply(channel, request);
        channelRepository.save(channel);
        auditLogService.record("CREATE_CHANNEL", "CHANNEL", request.channelCode());
        return toResponse(channel);
    }

    @Transactional
    public ChannelResponse update(String channelCode, CreateChannelRequest request) {
        MbChannel channel = channelRepository.findByChannelCode(channelCode)
                .orElseThrow(() -> BusinessException.notFound("channel not found"));
        apply(channel, request);
        auditLogService.record("UPDATE_CHANNEL", "CHANNEL", channelCode);
        return toResponse(channel);
    }

    @Transactional(readOnly = true)
    public ChannelResponse get(String channelCode) {
        return toResponse(channelRepository.findByChannelCode(channelCode)
                .orElseThrow(() -> BusinessException.notFound("channel not found")));
    }

    @Transactional(readOnly = true)
    public List<ChannelResponse> list() {
        return channelRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public MbChannel getEnabled(String channelCode) {
        MbChannel channel = channelRepository.findByChannelCode(channelCode)
                .orElseThrow(() -> BusinessException.notFound("channel not found: " + channelCode));
        if (channel.getStatus() == null || channel.getStatus() != Constants.STATUS_ENABLED) {
            throw BusinessException.badRequest("channel disabled: " + channelCode);
        }
        return channel;
    }

    @Transactional(readOnly = true)
    public List<MbChannel> findByCodes(Collection<String> channelCodes) {
        if (channelCodes == null || channelCodes.isEmpty()) {
            return List.of();
        }
        return channelRepository.findByChannelCodeIn(channelCodes);
    }

    @Transactional
    public ChannelResponse setStatus(String channelCode, int status) {
        MbChannel channel = channelRepository.findByChannelCode(channelCode)
                .orElseThrow(() -> BusinessException.notFound("channel not found"));
        channel.setStatus(status == Constants.STATUS_ENABLED ? Constants.STATUS_ENABLED : Constants.STATUS_DISABLED);
        auditLogService.record("SET_CHANNEL_STATUS", "CHANNEL", channelCode);
        return toResponse(channel);
    }

    @Transactional
    public void disable(String channelCode) {
        setStatus(channelCode, Constants.STATUS_DISABLED);
    }

    @Transactional(readOnly = true)
    public List<ChannelHealthResponse> health() {
        return channelRepository.findAll().stream().map(this::health).toList();
    }

    @Transactional(readOnly = true)
    public ChannelHealthResponse health(String channelCode) {
        return health(channelRepository.findByChannelCode(channelCode)
                .orElseThrow(() -> BusinessException.notFound("channel not found")));
    }

    public Map<String, Object> mergedConfig(MbChannel channel) {
        Map<String, Object> merged = new LinkedHashMap<>(jsonService.readMap(channel.getConfigJson()));
        String encrypted = channel.getSecretJsonEncrypted();
        if (StringUtils.hasText(encrypted)) {
            merged.putAll(jsonService.readMap(cryptoService.decrypt(encrypted)));
        }
        return merged;
    }

    public ChannelResponse toResponse(MbChannel channel) {
        Map<String, Object> config = jsonService.readMap(channel.getConfigJson());
        Map<String, Object> secrets = new LinkedHashMap<>();
        if (StringUtils.hasText(channel.getSecretJsonEncrypted())) {
            jsonService.readMap(cryptoService.decrypt(channel.getSecretJsonEncrypted()))
                    .keySet()
                    .forEach(key -> secrets.put(key, "******"));
        }
        return new ChannelResponse(
                channel.getId(),
                channel.getChannelCode(),
                channel.getChannelName(),
                channel.getChannelType(),
                config,
                secrets,
                channel.getStatus(),
                channel.getRemark(),
                channel.getCreatedAt(),
                channel.getUpdatedAt());
    }

    private void apply(MbChannel channel, CreateChannelRequest request) {
        Map<String, Object> config = new LinkedHashMap<>();
        Map<String, Object> secrets = new LinkedHashMap<>();
        if (request.config() != null) {
            request.config().forEach((key, value) -> {
                if (isSensitive(key)) {
                    secrets.put(key, value);
                } else {
                    config.put(key, value);
                }
            });
        }
        if (request.secrets() != null) {
            secrets.putAll(request.secrets());
        }
        if (channel.getId() != null && secrets.isEmpty() && StringUtils.hasText(channel.getSecretJsonEncrypted())) {
            secrets.putAll(jsonService.readMap(cryptoService.decrypt(channel.getSecretJsonEncrypted())));
        }
        channel.setChannelCode(request.channelCode());
        channel.setChannelName(request.channelName());
        channel.setChannelType(request.channelType());
        channel.setConfigJson(jsonService.toJson(config));
        channel.setSecretJsonEncrypted(secrets.isEmpty() ? null : cryptoService.encrypt(jsonService.toJson(secrets)));
        channel.setRemark(request.remark());
    }

    private boolean isSensitive(String key) {
        if (!StringUtils.hasText(key)) {
            return false;
        }
        return SENSITIVE_KEYS.contains(key.toLowerCase(Locale.ROOT).replace("-", "_"));
    }

    private ChannelHealthResponse health(MbChannel channel) {
        Map<String, Object> config = mergedConfig(channel);
        boolean configured = StringUtils.hasText(string(config, "webhook_url", "webhookUrl", "webhook"));
        boolean supported = switch (channel.getChannelType()) {
            case WE_COM_ROBOT, DINGTALK_ROBOT, FEISHU_ROBOT -> true;
            default -> false;
        };
        MbSendLog last = sendLogRepository.findFirstByChannelIdOrderByCreatedAtDesc(channel.getId()).orElse(null);
        String health;
        if (channel.getStatus() == null || channel.getStatus() != Constants.STATUS_ENABLED) {
            health = "DISABLED";
        } else if (!supported) {
            health = "UNSUPPORTED";
        } else if (!configured) {
            health = "MISCONFIGURED";
        } else if (last != null && last.getStatus() != null && "FAILED".equals(last.getStatus().name())) {
            health = "DEGRADED";
        } else {
            health = "OK";
        }
        return new ChannelHealthResponse(
                channel.getChannelCode(),
                channel.getChannelName(),
                channel.getChannelType(),
                channel.getStatus(),
                configured,
                supported,
                health,
                last == null || last.getStatus() == null ? null : last.getStatus().name(),
                last == null ? null : last.getPlatformCode(),
                last == null ? null : last.getPlatformMessage(),
                last == null ? null : last.getSentAt());
    }

    private String string(Map<String, Object> config, String... keys) {
        for (String key : keys) {
            Object value = config.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}
