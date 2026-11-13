package org.openelisglobal.notification.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.delivery.service.DeliveryOutboxService;
import org.openelisglobal.notification.dao.NotificationLogDAO;
import org.openelisglobal.notification.valueholder.NotificationLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationLogServiceImpl extends AuditableBaseObjectServiceImpl<NotificationLog, Long>
        implements NotificationLogService {

    @Autowired
    private NotificationLogDAO baseDAO;

    @Autowired
    @Lazy
    private DeliveryOutboxService outbox;

    public NotificationLogServiceImpl() {
        super(NotificationLog.class);
        // This *is* the audit trail; no need to audit-trail an audit log.
        this.auditTrailLog = false;
    }

    @Override
    protected BaseDAO<NotificationLog, Long> getBaseObjectDAO() {
        return baseDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationLog> findPage(Optional<String> eventCode, Optional<String> status, int page, int size) {
        int safePage = Math.max(1, page);
        int safeSize = Math.max(1, Math.min(size, 200));
        return baseDAO.findPage(eventCode, status, safePage, safeSize);
    }

    @Override
    @Transactional(readOnly = true)
    public long countMatching(Optional<String> eventCode, Optional<String> status) {
        return baseDAO.countMatching(eventCode, status);
    }

    @Override
    public NotificationLog resend(Long sourceLogId, String sysUserId) {
        return outbox.resend(sourceLogId, sysUserId);
    }
}
