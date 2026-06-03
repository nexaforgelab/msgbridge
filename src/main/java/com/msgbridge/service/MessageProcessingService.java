package com.msgbridge.service;

import com.msgbridge.adapter.AdapterRegistry;
import com.msgbridge.adapter.MessageChannelAdapter;
import com.msgbridge.adapter.SendContext;
import com.msgbridge.adapter.SendResult;
import com.msgbridge.core.BusinessException;
import com.msgbridge.core.ChannelType;
import com.msgbridge.core.Constants;
import com.msgbridge.core.SendStatus;
import com.msgbridge.core.TaskStatus;
import com.msgbridge.domain.MbChannel;
import com.msgbridge.domain.MbMessageTask;
import com.msgbridge.domain.MbSendLog;
import com.msgbridge.domain.MbTemplate;
import com.msgbridge.dto.MessageOptionsDto;
import com.msgbridge.dto.SendMessageRequest;
import com.msgbridge.repository.MessageTaskRepository;
import com.msgbridge.repository.SendLogRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageProcessingService {
    private final MessageTaskRepository messageTaskRepository;
    private final SendLogRepository sendLogRepository;
    private final JsonService jsonService;
    private final RouteService routeService;
    private final ChannelService channelService;
    private final TemplateService templateService;
    private final TemplateRenderer templateRenderer;
    private final AdapterRegistry adapterRegistry;
    private final MaskingService maskingService;

    public MessageProcessingService(
            MessageTaskRepository messageTaskRepository,
            SendLogRepository sendLogRepository,
            JsonService jsonService,
            RouteService routeService,
            ChannelService channelService,
            TemplateService templateService,
            TemplateRenderer templateRenderer,
            AdapterRegistry adapterRegistry,
            MaskingService maskingService) {
        this.messageTaskRepository = messageTaskRepository;
        this.sendLogRepository = sendLogRepository;
        this.jsonService = jsonService;
        this.routeService = routeService;
        this.channelService = channelService;
        this.templateService = templateService;
        this.templateRenderer = templateRenderer;
        this.adapterRegistry = adapterRegistry;
        this.maskingService = maskingService;
    }

    @Transactional
    public void process(Long taskId) {
        MbMessageTask task = messageTaskRepository.findById(taskId)
                .orElseThrow(() -> BusinessException.notFound("task not found"));
        if (task.getStatus() != TaskStatus.PENDING && task.getStatus() != TaskStatus.RETRYING) {
            return;
        }
        task.setStatus(TaskStatus.SENDING);
        task.setErrorCode(null);
        task.setErrorMessage(null);
        messageTaskRepository.flush();

        try {
            ProcessOutcome outcome = send(task);
            if (outcome.total == 0) {
                fail(task, "NO_ROUTE", "no enabled target channel matched", false);
            } else if (outcome.success == outcome.total) {
                task.setStatus(TaskStatus.SUCCESS);
                task.setNextRetryAt(null);
            } else if (outcome.success > 0) {
                task.setStatus(TaskStatus.PARTIAL_SUCCESS);
                task.setErrorCode("PARTIAL_FAILED");
                task.setErrorMessage(outcome.lastError);
            } else {
                fail(task, "SEND_FAILED", outcome.lastError, outcome.retryable);
            }
        } catch (RuntimeException e) {
            fail(task, "PROCESS_ERROR", e.getMessage(), true);
        }
    }

    private ProcessOutcome send(MbMessageTask task) {
        SendMessageRequest request = jsonService.fromJson(task.getMessageJson(), SendMessageRequest.class);
        Map<String, Object> variables = templateRenderer.variables(request.message());
        RouteService.RouteResult route = routeService.resolve(request.sceneCode(), request.receiver(), variables);
        if (route.channelCodes().isEmpty()) {
            return new ProcessOutcome(0, 0, false, "no route matched");
        }

        Map<String, MbChannel> channels = channelService.findByCodes(route.channelCodes()).stream()
                .collect(Collectors.toMap(MbChannel::getChannelCode, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        List<SendResult> results = new ArrayList<>();
        for (String channelCode : route.channelCodes()) {
            MbChannel channel = channels.get(channelCode);
            if (channel == null) {
                results.add(SendResult.failure(null, -1L, "CHANNEL_NOT_FOUND", "channel not found: " + channelCode, false));
                continue;
            }
            if (channel.getStatus() == null || channel.getStatus() != Constants.STATUS_ENABLED) {
                results.add(SendResult.failure(channel.getChannelType(), channel.getId(), "CHANNEL_DISABLED", "channel disabled", false));
                continue;
            }
            MbTemplate template = templateService.latest(request.sceneCode(), channel.getChannelType()).orElse(null);
            TemplateRenderer.PreparedMessage prepared = templateRenderer.render(template, request.message());
            MessageChannelAdapter adapter = adapterRegistry.get(channel.getChannelType());
            if (!adapter.supports(prepared.type())) {
                results.add(SendResult.failure(channel.getChannelType(), channel.getId(), "UNSUPPORTED_MSG_TYPE", "message type not supported", false));
                continue;
            }
            Map<String, Object> config = channelService.mergedConfig(channel);
            results.add(adapter.send(new SendContext(channel, config, prepared, timeoutSeconds(request.options()))));
        }

        int success = 0;
        boolean retryable = false;
        String lastError = null;
        for (SendResult result : results) {
            saveLog(task.getMessageId(), result);
            if (result.success()) {
                success++;
            } else {
                retryable = retryable || result.retryable();
                lastError = result.platformMessage();
            }
        }
        return new ProcessOutcome(results.size(), success, retryable, lastError);
    }

    private void saveLog(String messageId, SendResult result) {
        MbSendLog log = new MbSendLog();
        log.setMessageId(messageId);
        log.setChannelId(result.channelId() == null ? -1L : result.channelId());
        log.setChannelType(result.platform() == null ? ChannelType.UNKNOWN : result.platform());
        log.setPlatformRequest(maskingService.mask(result.requestJson()));
        log.setPlatformResponse(maskingService.mask(result.rawResponse()));
        log.setPlatformCode(result.platformCode());
        log.setPlatformMessage(maskingService.mask(result.platformMessage()));
        log.setStatus(result.success() ? SendStatus.SUCCESS : SendStatus.FAILED);
        log.setRetryable(result.retryable() ? 1 : 0);
        log.setCostMs(result.costMs());
        log.setSentAt(Instant.now());
        sendLogRepository.save(log);
    }

    private void fail(MbMessageTask task, String code, String message, boolean retryable) {
        task.setErrorCode(code);
        task.setErrorMessage(message);
        int retryCount = task.getRetryCount() == null ? 0 : task.getRetryCount();
        int maxRetryCount = task.getMaxRetryCount() == null ? 3 : task.getMaxRetryCount();
        if (retryable && retryCount < maxRetryCount) {
            task.setRetryCount(retryCount + 1);
            task.setStatus(TaskStatus.RETRYING);
            task.setNextRetryAt(Instant.now().plusSeconds(backoffSeconds(retryCount + 1)));
        } else if (retryable && maxRetryCount > 0) {
            task.setStatus(TaskStatus.DEAD);
            task.setNextRetryAt(null);
        } else {
            task.setStatus(TaskStatus.FAILED);
            task.setNextRetryAt(null);
        }
    }

    private long backoffSeconds(int retryCount) {
        return Math.min(300, (long) Math.pow(2, Math.max(0, retryCount - 1)) * 30);
    }

    private int timeoutSeconds(MessageOptionsDto options) {
        if (options == null || options.timeoutSeconds() == null) {
            return 10;
        }
        return Math.max(1, Math.min(options.timeoutSeconds(), 60));
    }

    private record ProcessOutcome(int total, int success, boolean retryable, String lastError) {
    }
}
