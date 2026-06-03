package com.msgbridge.service;

import com.msgbridge.config.MsgBridgeProperties;
import com.msgbridge.core.Constants;
import com.msgbridge.core.TaskStatus;
import com.msgbridge.dto.ChannelHealthResponse;
import com.msgbridge.repository.AdminUserRepository;
import com.msgbridge.repository.AppRepository;
import com.msgbridge.repository.ChannelRepository;
import com.msgbridge.repository.MessageTaskRepository;
import com.msgbridge.repository.RouteRuleRepository;
import com.msgbridge.repository.TemplateRepository;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class SystemStatusService {
    private final MsgBridgeProperties properties;
    private final AppRepository appRepository;
    private final ChannelRepository channelRepository;
    private final TemplateRepository templateRepository;
    private final RouteRuleRepository routeRuleRepository;
    private final MessageTaskRepository messageTaskRepository;
    private final AdminUserRepository adminUserRepository;
    private final ChannelService channelService;

    public SystemStatusService(
            MsgBridgeProperties properties,
            AppRepository appRepository,
            ChannelRepository channelRepository,
            TemplateRepository templateRepository,
            RouteRuleRepository routeRuleRepository,
            MessageTaskRepository messageTaskRepository,
            AdminUserRepository adminUserRepository,
            ChannelService channelService) {
        this.properties = properties;
        this.appRepository = appRepository;
        this.channelRepository = channelRepository;
        this.templateRepository = templateRepository;
        this.routeRuleRepository = routeRuleRepository;
        this.messageTaskRepository = messageTaskRepository;
        this.adminUserRepository = adminUserRepository;
        this.channelService = channelService;
    }

    public Map<String, Object> status() {
        List<ChannelHealthResponse> channelHealth = channelService.health();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("generatedAt", Instant.now());
        data.put("runtime", runtime());
        data.put("worker", worker());
        data.put("counts", counts(channelHealth));
        data.put("warnings", warnings(channelHealth));
        return data;
    }

    private Map<String, Object> runtime() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("javaVersion", System.getProperty("java.version"));
        data.put("os", System.getProperty("os.name") + " " + System.getProperty("os.version"));
        data.put("uptimeMs", ManagementFactory.getRuntimeMXBean().getUptime());
        data.put("processors", runtime.availableProcessors());
        data.put("maxMemoryMb", runtime.maxMemory() / 1024 / 1024);
        data.put("freeMemoryMb", runtime.freeMemory() / 1024 / 1024);
        return data;
    }

    private Map<String, Object> worker() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("enabled", properties.getWorker().isEnabled());
        data.put("batchSize", properties.getWorker().getBatchSize());
        data.put("fixedDelayMs", properties.getWorker().getFixedDelayMs());
        return data;
    }

    private Map<String, Object> counts(List<ChannelHealthResponse> channelHealth) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("apps", appRepository.count());
        data.put("channels", channelRepository.count());
        data.put("enabledChannels", channelHealth.stream()
                .filter(item -> item.status() != null && item.status() == Constants.STATUS_ENABLED)
                .count());
        data.put("misconfiguredChannels", channelHealth.stream()
                .filter(item -> "MISCONFIGURED".equals(item.health()))
                .count());
        data.put("templates", templateRepository.count());
        data.put("routes", routeRuleRepository.count());
        data.put("adminUsers", adminUserRepository.count());
        data.put("pendingMessages", messageTaskRepository.countByStatus(TaskStatus.PENDING));
        data.put("retryingMessages", messageTaskRepository.countByStatus(TaskStatus.RETRYING));
        data.put("deadMessages", messageTaskRepository.countByStatus(TaskStatus.DEAD));
        return data;
    }

    private List<String> warnings(List<ChannelHealthResponse> channelHealth) {
        java.util.ArrayList<String> warnings = new java.util.ArrayList<>();
        if ("change-me-dev-master-key".equals(properties.getSecurity().getMasterKey())) {
            warnings.add("MSGBRIDGE_MASTER_KEY is using the development default");
        }
        if ("dev-admin-key".equals(properties.getSecurity().getAdminKey())) {
            warnings.add("MSGBRIDGE_ADMIN_KEY is using the development default");
        }
        if ("admin123".equals(properties.getSecurity().getDefaultAdminPassword())) {
            warnings.add("default admin password is still admin123");
        }
        if (properties.getSeed().isDemoEnabled()) {
            warnings.add("demo seed is enabled");
        }
        if (!properties.getWorker().isEnabled()) {
            warnings.add("message worker is disabled");
        }
        if (channelHealth.stream().noneMatch(item -> item.status() != null && item.status() == Constants.STATUS_ENABLED)) {
            warnings.add("no enabled channel is configured");
        }
        channelHealth.stream()
                .filter(item -> !"OK".equals(item.health()) && !"DISABLED".equals(item.health()))
                .map(item -> "channel " + item.channelCode() + " health is " + item.health())
                .forEach(warnings::add);
        return warnings;
    }
}
