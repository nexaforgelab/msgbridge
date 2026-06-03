package com.msgbridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.msgbridge.core.BusinessException;
import com.msgbridge.core.TaskStatus;
import com.msgbridge.domain.MbMessageTask;
import com.msgbridge.dto.BulkMessageActionResponse;
import com.msgbridge.dto.MessageQueryRequest;
import com.msgbridge.dto.PurgePreviewResponse;
import com.msgbridge.repository.AuditLogRepository;
import com.msgbridge.repository.MessageTaskRepository;
import com.msgbridge.repository.SendLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MessageServiceTest {

    private final MessageTaskRepository messageTaskRepository = mock(MessageTaskRepository.class);
    private final SendLogRepository sendLogRepository = mock(SendLogRepository.class);
    private final MessageService messageService = new MessageService(
            messageTaskRepository,
            sendLogRepository,
            new JsonService(new ObjectMapper()),
            new AuditLogService(mock(AuditLogRepository.class), new JsonService(new ObjectMapper())),
            new MaskingService());

    @Test
    void rejectsInvalidAdminMessageStatus() {
        MessageQueryRequest query = new MessageQueryRequest(
                null, null, null, "BROKEN", null, null, null, null);

        assertThatThrownBy(() -> messageService.adminList(0, 10, query))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalid message status");
    }

    @Test
    void previewsPurgeImpactBeforeDeleting() {
        Instant before = Instant.now().minusSeconds(7200);
        when(messageTaskRepository.countByCreatedAtBefore(before)).thenReturn(3L);
        when(sendLogRepository.countByCreatedAtBefore(before)).thenReturn(7L);

        PurgePreviewResponse response = messageService.purgePreview(before);

        assertThat(response.before()).isEqualTo(before);
        assertThat(response.messages()).isEqualTo(3);
        assertThat(response.sendLogs()).isEqualTo(7);
    }

    @Test
    void bulkTerminateReportsPerMessageResult() {
        MbMessageTask task = new MbMessageTask();
        task.setMessageId("msg-1");
        task.setStatus(TaskStatus.PENDING);
        when(messageTaskRepository.findByMessageId("msg-1")).thenReturn(Optional.of(task));
        when(messageTaskRepository.findByMessageId("missing")).thenReturn(Optional.empty());

        BulkMessageActionResponse response = messageService.bulkTerminate(List.of("msg-1", "missing", "msg-1"));

        assertThat(response.requested()).isEqualTo(2);
        assertThat(response.succeeded()).isEqualTo(1);
        assertThat(response.failed()).isEqualTo(1);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.DEAD);
        assertThat(response.results()).extracting(BulkMessageActionResponse.ItemResult::messageId)
                .containsExactly("msg-1", "missing");
    }
}
