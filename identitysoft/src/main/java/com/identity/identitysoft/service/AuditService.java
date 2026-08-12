package com.identity.identitysoft.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.identity.identitysoft.entity.AuditLog;
import com.identity.identitysoft.repository.AuditLogRepository;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(String username, String action) {
        AuditLog entry = AuditLog.builder()
                .username(username)
                .action(action)
                .build();
        auditLogRepository.save(entry);
    }

    public List<AuditLog> getLogsForUser(String username) {
        return auditLogRepository.findByUsernameOrderByTimestampDesc(username);
    }
}