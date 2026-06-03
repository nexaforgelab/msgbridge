package com.msgbridge.repository;

import com.msgbridge.domain.MbAuditLog;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository extends JpaRepository<MbAuditLog, Long>, JpaSpecificationExecutor<MbAuditLog> {
    List<MbAuditLog> findByOrderByCreatedAtDesc(Pageable pageable);
}
