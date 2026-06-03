package com.msgbridge.worker;

import com.msgbridge.config.MsgBridgeProperties;
import com.msgbridge.core.TaskStatus;
import com.msgbridge.domain.MbMessageTask;
import com.msgbridge.repository.MessageTaskRepository;
import com.msgbridge.service.MessageProcessingService;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MessageTaskWorker {
    private static final Logger log = LoggerFactory.getLogger(MessageTaskWorker.class);

    private final MsgBridgeProperties properties;
    private final MessageTaskRepository messageTaskRepository;
    private final MessageProcessingService messageProcessingService;

    public MessageTaskWorker(
            MsgBridgeProperties properties,
            MessageTaskRepository messageTaskRepository,
            MessageProcessingService messageProcessingService) {
        this.properties = properties;
        this.messageTaskRepository = messageTaskRepository;
        this.messageProcessingService = messageProcessingService;
    }

    @Scheduled(fixedDelayString = "${msgbridge.worker.fixed-delay-ms:1000}")
    public void poll() {
        if (!properties.getWorker().isEnabled()) {
            return;
        }
        List<MbMessageTask> tasks = messageTaskRepository.findDueTasks(
                        List.of(TaskStatus.PENDING, TaskStatus.RETRYING),
                        Instant.now(),
                        PageRequest.of(0, properties.getWorker().getBatchSize()))
                .getContent();
        for (MbMessageTask task : tasks) {
            try {
                messageProcessingService.process(task.getId());
            } catch (RuntimeException e) {
                log.warn("message task processing failed, messageId={}, reason={}", task.getMessageId(), e.getMessage());
            }
        }
    }
}
