package com.msgbridge.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.msgbridge.core.Constants;
import com.msgbridge.domain.MbAuditLog;
import com.msgbridge.repository.AuditLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class AuditLogServiceTest {

    private final AuditLogRepository auditLogRepository = org.mockito.Mockito.mock(AuditLogRepository.class);
    private final AuditLogService auditLogService = new AuditLogService(
            auditLogRepository,
            new JsonService(new ObjectMapper()));

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void recordsCurrentAdminActorWhenActorIsNotExplicit() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(Constants.ADMIN_USERNAME_ATTR, "ops");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        auditLogService.record("SET_CHANNEL_STATUS", "CHANNEL", "wecom_sales");

        ArgumentCaptor<MbAuditLog> captor = ArgumentCaptor.forClass(MbAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActor()).isEqualTo("ops");
    }

    @Test
    void recordsSystemActorWithoutRequestContext() {
        auditLogService.record(null, "SCHEDULED_TASK", "WORKER", "poll", null);

        ArgumentCaptor<MbAuditLog> captor = ArgumentCaptor.forClass(MbAuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getActor()).isEqualTo("system");
    }
}
