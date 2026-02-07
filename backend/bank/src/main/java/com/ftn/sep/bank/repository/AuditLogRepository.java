package com.ftn.sep.bank.repository;

import com.ftn.sep.bank.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByActionAndTimestampBetween(String action, LocalDateTime start, LocalDateTime end);

    List<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId);
}
