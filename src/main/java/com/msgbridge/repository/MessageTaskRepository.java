package com.msgbridge.repository;

import com.msgbridge.core.TaskStatus;
import com.msgbridge.domain.MbMessageTask;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageTaskRepository extends JpaRepository<MbMessageTask, Long>, JpaSpecificationExecutor<MbMessageTask> {
    Optional<MbMessageTask> findByMessageId(String messageId);

    Optional<MbMessageTask> findByAppIdAndRequestId(String appId, String requestId);

    long countByStatus(TaskStatus status);

    long countByCreatedAtAfter(Instant after);

    long countByCreatedAtBefore(Instant before);

    long deleteByCreatedAtBefore(Instant before);

    @Query("""
            select t from MbMessageTask t
            where t.status in :statuses
              and (t.nextRetryAt is null or t.nextRetryAt <= :now)
            order by t.createdAt asc
            """)
    Page<MbMessageTask> findDueTasks(
            @Param("statuses") Collection<TaskStatus> statuses,
            @Param("now") Instant now,
            Pageable pageable);
}
