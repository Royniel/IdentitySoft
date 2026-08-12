package com.identity.identitysoft.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.identity.identitysoft.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUsernameOrderByTimestampDesc(String username);
}