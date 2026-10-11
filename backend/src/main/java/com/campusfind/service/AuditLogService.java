package com.campusfind.service;

import com.campusfind.dto.response.AuditLogDto;
import com.campusfind.entity.AuditLog;
import com.campusfind.entity.User;
import com.campusfind.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog logAction(User admin, String action, String targetType, Long targetId, String details) {
        AuditLog auditLog = AuditLog.builder()
                .admin(admin)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .details(details)
                .build();

        log.info("ADMIN AUDIT [adminId={}, action={}, targetType={}, targetId={}]",
                admin != null ? admin.getId() : null, action, targetType, targetId);

        return auditLogRepository.save(auditLog);
    }

    @Transactional(readOnly = true)
    public Page<AuditLogDto> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(log -> AuditLogDto.builder()
                        .id(log.getId())
                        .adminId(log.getAdmin().getId())
                        .adminEmail(log.getAdmin().getEmail())
                        .action(log.getAction())
                        .targetType(log.getTargetType())
                        .targetId(log.getTargetId())
                        .details(log.getDetails())
                        .createdAt(log.getCreatedAt())
                        .build());
    }
}
