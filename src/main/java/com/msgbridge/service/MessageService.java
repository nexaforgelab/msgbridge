package com.msgbridge.service;

import com.msgbridge.core.BusinessException;
import com.msgbridge.core.Constants;
import com.msgbridge.core.TaskStatus;
import com.msgbridge.domain.MbMessageTask;
import com.msgbridge.domain.MbSendLog;
import com.msgbridge.dto.MessageDetailResponse;
import com.msgbridge.dto.MessageOptionsDto;
import com.msgbridge.dto.MessageQueryRequest;
import com.msgbridge.dto.BulkMessageActionResponse;
import com.msgbridge.dto.PurgeResponse;
import com.msgbridge.dto.PurgePreviewResponse;
import com.msgbridge.dto.SendMessageRequest;
import com.msgbridge.dto.SendMessageResponse;
import com.msgbridge.repository.MessageTaskRepository;
import com.msgbridge.repository.SendLogRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class MessageService {
    private final MessageTaskRepository messageTaskRepository;
    private final SendLogRepository sendLogRepository;
    private final JsonService jsonService;
    private final AuditLogService auditLogService;
    private final MaskingService maskingService;

    public MessageService(
            MessageTaskRepository messageTaskRepository,
            SendLogRepository sendLogRepository,
            JsonService jsonService,
            AuditLogService auditLogService,
            MaskingService maskingService) {
        this.messageTaskRepository = messageTaskRepository;
        this.sendLogRepository = sendLogRepository;
        this.jsonService = jsonService;
        this.auditLogService = auditLogService;
        this.maskingService = maskingService;
    }

    @Transactional
    public SendMessageResponse accept(SendMessageRequest request, String authenticatedAppId) {
        if (!request.appId().equals(authenticatedAppId)) {
            throw BusinessException.forbidden("request appId does not match authenticated app");
        }
        MessageOptionsDto options = request.options() == null ? new MessageOptionsDto(true, 10, true, 3) : request.options();
        if (options.deduplicate() == null || Boolean.TRUE.equals(options.deduplicate())) {
            MbMessageTask existing = messageTaskRepository.findByAppIdAndRequestId(request.appId(), request.requestId())
                    .orElse(null);
            if (existing != null) {
                return new SendMessageResponse(
                        existing.getRequestId(), existing.getMessageId(), existing.getStatus().name(), true);
            }
        }

        MbMessageTask task = new MbMessageTask();
        task.setMessageId(newMessageId());
        task.setRequestId(request.requestId());
        task.setAppId(request.appId());
        task.setSceneCode(request.sceneCode());
        task.setRouteKey(request.routeKey());
        task.setPriority(StringUtils.hasText(request.priority()) ? request.priority() : "NORMAL");
        task.setMessageJson(jsonService.toJson(request));
        task.setStatus(TaskStatus.PENDING);
        task.setMaxRetryCount(options.effectiveMaxRetryCount());
        messageTaskRepository.save(task);
        return new SendMessageResponse(task.getRequestId(), task.getMessageId(), task.getStatus().name(), false);
    }

    @Transactional(readOnly = true)
    public MessageDetailResponse detailByMessageId(String messageId, String authenticatedAppId) {
        MbMessageTask task = messageTaskRepository.findByMessageId(messageId)
                .orElseThrow(() -> BusinessException.notFound("message not found"));
        if (!task.getAppId().equals(authenticatedAppId)) {
            throw BusinessException.forbidden("message belongs to another app");
        }
        return detail(task);
    }

    @Transactional(readOnly = true)
    public MessageDetailResponse detailByRequestId(String appId, String requestId, String authenticatedAppId) {
        if (!appId.equals(authenticatedAppId)) {
            throw BusinessException.forbidden("request appId does not match authenticated app");
        }
        MbMessageTask task = messageTaskRepository.findByAppIdAndRequestId(appId, requestId)
                .orElseThrow(() -> BusinessException.notFound("message not found"));
        return detail(task);
    }

    @Transactional(readOnly = true)
    public MessageDetailResponse adminDetail(String messageId) {
        return detail(messageTaskRepository.findByMessageId(messageId)
                .orElseThrow(() -> BusinessException.notFound("message not found")));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> adminList(int page, int size, MessageQueryRequest query) {
        return messageTaskRepository.findAll(spec(query), PageRequest.of(
                        Math.max(page, 0),
                        Math.max(1, Math.min(size, 100)),
                        Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(this::taskMap)
                .toList();
    }

    @Transactional(readOnly = true)
    public String exportCsv(MessageQueryRequest query) {
        List<MbMessageTask> tasks = messageTaskRepository.findAll(
                spec(query),
                PageRequest.of(0, 10_000, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        StringBuilder sb = new StringBuilder();
        sb.append("messageId,requestId,appId,sceneCode,routeKey,priority,status,retryCount,maxRetryCount,errorCode,errorMessage,createdAt,updatedAt\n");
        for (MbMessageTask task : tasks) {
            sb.append(csv(task.getMessageId())).append(',')
                    .append(csv(task.getRequestId())).append(',')
                    .append(csv(task.getAppId())).append(',')
                    .append(csv(task.getSceneCode())).append(',')
                    .append(csv(task.getRouteKey())).append(',')
                    .append(csv(task.getPriority())).append(',')
                    .append(csv(task.getStatus() == null ? null : task.getStatus().name())).append(',')
                    .append(csv(task.getRetryCount())).append(',')
                    .append(csv(task.getMaxRetryCount())).append(',')
                    .append(csv(task.getErrorCode())).append(',')
                    .append(csv(maskingService.mask(task.getErrorMessage()))).append(',')
                    .append(csv(task.getCreatedAt())).append(',')
                    .append(csv(task.getUpdatedAt())).append('\n');
        }
        return sb.toString();
    }

    @Transactional
    public PurgeResponse purgeBefore(Instant before) {
        validatePurgeCutoff(before);
        long deletedLogs = sendLogRepository.deleteByCreatedAtBefore(before);
        long deletedMessages = messageTaskRepository.deleteByCreatedAtBefore(before);
        auditLogService.record(null, "PURGE_MESSAGES", "MESSAGE", before.toString(), Map.of(
                "deletedMessages", deletedMessages,
                "deletedSendLogs", deletedLogs));
        return new PurgeResponse(deletedMessages, deletedLogs);
    }

    @Transactional(readOnly = true)
    public PurgePreviewResponse purgePreview(Instant before) {
        validatePurgeCutoff(before);
        return new PurgePreviewResponse(
                before,
                messageTaskRepository.countByCreatedAtBefore(before),
                sendLogRepository.countByCreatedAtBefore(before));
    }

    @Transactional
    public void markManualRetry(String messageId) {
        MbMessageTask task = messageTaskRepository.findByMessageId(messageId)
                .orElseThrow(() -> BusinessException.notFound("message not found"));
        resetForRetry(task);
        auditLogService.record("MANUAL_RETRY_MESSAGE", "MESSAGE", messageId);
    }

    @Transactional
    public void markManualRetry(String messageId, String authenticatedAppId) {
        MbMessageTask task = messageTaskRepository.findByMessageId(messageId)
                .orElseThrow(() -> BusinessException.notFound("message not found"));
        if (!task.getAppId().equals(authenticatedAppId)) {
            throw BusinessException.forbidden("message belongs to another app");
        }
        resetForRetry(task);
        auditLogService.record(authenticatedAppId, "APP_RETRY_MESSAGE", "MESSAGE", messageId, Map.of("appId", authenticatedAppId));
    }

    @Transactional
    public void terminate(String messageId) {
        MbMessageTask task = messageTaskRepository.findByMessageId(messageId)
                .orElseThrow(() -> BusinessException.notFound("message not found"));
        task.setStatus(TaskStatus.DEAD);
        task.setNextRetryAt(null);
        task.setErrorCode("MANUAL_TERMINATED");
        task.setErrorMessage("message was manually terminated");
        auditLogService.record("TERMINATE_MESSAGE", "MESSAGE", messageId);
    }

    @Transactional
    public BulkMessageActionResponse bulkRetry(List<String> messageIds) {
        return bulkAction(messageIds, "retry", this::markManualRetry);
    }

    @Transactional
    public BulkMessageActionResponse bulkTerminate(List<String> messageIds) {
        return bulkAction(messageIds, "terminate", this::terminate);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> dashboard() {
        Instant today = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long totalToday = messageTaskRepository.countByCreatedAtAfter(today);
        long logTotalToday = sendLogRepository.countByCreatedAtAfter(today);
        long successLogs = sendLogRepository.countByStatusAndCreatedAtAfter(com.msgbridge.core.SendStatus.SUCCESS, today);
        long failedLogs = sendLogRepository.countByStatusAndCreatedAtAfter(com.msgbridge.core.SendStatus.FAILED, today);
        double successRate = logTotalToday == 0 ? 1.0 : (successLogs * 1.0 / logTotalToday);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("todayMessages", totalToday);
        data.put("todaySendLogs", logTotalToday);
        data.put("successLogs", successLogs);
        data.put("failedLogs", failedLogs);
        data.put("successRate", successRate);
        data.put("pending", messageTaskRepository.countByStatus(TaskStatus.PENDING));
        data.put("retrying", messageTaskRepository.countByStatus(TaskStatus.RETRYING));
        data.put("dead", messageTaskRepository.countByStatus(TaskStatus.DEAD));
        data.put("channelShare", sendLogRepository.countByChannelTypeSince(today).stream()
                .collect(LinkedHashMap::new,
                        (map, item) -> map.put(item.getChannelType().name(), item.getCountValue()),
                        LinkedHashMap::putAll));
        return data;
    }

    private MessageDetailResponse detail(MbMessageTask task) {
        return new MessageDetailResponse(taskMap(task), sendLogRepository.findByMessageIdOrderByCreatedAtAsc(task.getMessageId())
                .stream()
                .map(this::logMap)
                .toList());
    }

    private Map<String, Object> taskMap(MbMessageTask task) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("messageId", task.getMessageId());
        map.put("requestId", task.getRequestId());
        map.put("appId", task.getAppId());
        map.put("sceneCode", task.getSceneCode());
        map.put("routeKey", task.getRouteKey());
        map.put("priority", task.getPriority());
        map.put("status", task.getStatus());
        map.put("retryCount", task.getRetryCount());
        map.put("maxRetryCount", task.getMaxRetryCount());
        map.put("nextRetryAt", task.getNextRetryAt());
        map.put("errorCode", task.getErrorCode());
        map.put("errorMessage", task.getErrorMessage());
        map.put("messageJson", maskingService.mask(task.getMessageJson()));
        map.put("createdAt", task.getCreatedAt());
        map.put("updatedAt", task.getUpdatedAt());
        return map;
    }

    private Specification<MbMessageTask> spec(MessageQueryRequest query) {
        TaskStatus status = query == null ? null : parseStatus(query.status());
        validateTimeRange(query);
        return (root, criteriaQuery, builder) -> {
            if (query == null) {
                return builder.conjunction();
            }
            var predicate = builder.conjunction();
            if (StringUtils.hasText(query.appId())) {
                predicate = builder.and(predicate, builder.equal(root.get("appId"), query.appId()));
            }
            if (StringUtils.hasText(query.sceneCode())) {
                predicate = builder.and(predicate, builder.equal(root.get("sceneCode"), query.sceneCode()));
            }
            if (StringUtils.hasText(query.routeKey())) {
                predicate = builder.and(predicate, builder.equal(root.get("routeKey"), query.routeKey()));
            }
            if (status != null) {
                predicate = builder.and(predicate, builder.equal(root.get("status"), status));
            }
            if (StringUtils.hasText(query.requestId())) {
                predicate = builder.and(predicate, builder.like(root.get("requestId"), "%" + query.requestId() + "%"));
            }
            if (StringUtils.hasText(query.messageId())) {
                predicate = builder.and(predicate, builder.like(root.get("messageId"), "%" + query.messageId() + "%"));
            }
            if (query.createdFrom() != null) {
                predicate = builder.and(predicate, builder.greaterThanOrEqualTo(root.get("createdAt"), query.createdFrom()));
            }
            if (query.createdTo() != null) {
                predicate = builder.and(predicate, builder.lessThanOrEqualTo(root.get("createdAt"), query.createdTo()));
            }
            return predicate;
        };
    }

    private void validateTimeRange(MessageQueryRequest query) {
        if (query == null || query.createdFrom() == null || query.createdTo() == null) {
            return;
        }
        if (query.createdFrom().isAfter(query.createdTo())) {
            throw BusinessException.badRequest("createdFrom must be earlier than or equal to createdTo");
        }
    }

    private void validatePurgeCutoff(Instant before) {
        if (before == null || before.isAfter(Instant.now().minusSeconds(3600))) {
            throw BusinessException.badRequest("purge cutoff must be at least one hour in the past");
        }
    }

    private TaskStatus parseStatus(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return TaskStatus.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw BusinessException.badRequest("invalid message status: " + value
                    + ", allowed: " + Arrays.toString(TaskStatus.values()));
        }
    }

    private BulkMessageActionResponse bulkAction(List<String> messageIds, String action, Consumer<String> handler) {
        if (messageIds == null || messageIds.isEmpty()) {
            throw BusinessException.badRequest("messageIds is required");
        }
        List<String> ids = messageIds.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .limit(100)
                .toList();
        if (ids.isEmpty()) {
            throw BusinessException.badRequest("messageIds is required");
        }

        List<BulkMessageActionResponse.ItemResult> results = ids.stream()
                .map(messageId -> {
                    try {
                        handler.accept(messageId);
                        return new BulkMessageActionResponse.ItemResult(messageId, true, action + " accepted");
                    } catch (BusinessException e) {
                        return new BulkMessageActionResponse.ItemResult(messageId, false, e.getMessage());
                    }
                })
                .toList();
        int succeeded = (int) results.stream().filter(BulkMessageActionResponse.ItemResult::success).count();
        return new BulkMessageActionResponse(ids.size(), succeeded, ids.size() - succeeded, results);
    }

    private Map<String, Object> logMap(MbSendLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("messageId", log.getMessageId());
        map.put("channelId", log.getChannelId());
        map.put("channelType", log.getChannelType());
        map.put("platformRequest", maskingService.mask(log.getPlatformRequest()));
        map.put("platformResponse", maskingService.mask(log.getPlatformResponse()));
        map.put("platformCode", log.getPlatformCode());
        map.put("platformMessage", maskingService.mask(log.getPlatformMessage()));
        map.put("status", log.getStatus());
        map.put("retryable", log.getRetryable());
        map.put("costMs", log.getCostMs());
        map.put("sentAt", log.getSentAt());
        map.put("createdAt", log.getCreatedAt());
        return map;
    }

    private void resetForRetry(MbMessageTask task) {
        task.setStatus(TaskStatus.RETRYING);
        task.setNextRetryAt(Instant.now());
        task.setRetryCount(0);
        task.setErrorCode(null);
        task.setErrorMessage(null);
    }

    private String newMessageId() {
        return "msg_" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now())
                + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private String csv(Object value) {
        if (value == null) {
            return "";
        }
        String text = String.valueOf(value);
        if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
            return "\"" + text.replace("\"", "\"\"") + "\"";
        }
        return text;
    }
}
