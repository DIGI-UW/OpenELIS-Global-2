package org.openelisglobal.rbac.service;

import org.openelisglobal.rbac.dao.RbacAuditLogDAO;
import org.openelisglobal.rbac.valueholder.RbacAuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RbacAuditServiceImpl implements RbacAuditService {

    @Autowired
    private RbacAuditLogDAO rbacAuditLogDAO;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAction(String systemUserId, String username, String module, String action,
            String recordId, String recordType, String departmentId, String projectId,
            String ipAddress, String details) {
        persist(systemUserId, username, module, action, recordId, recordType,
                departmentId, projectId, ipAddress, details, "SUCCESS");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDenied(String systemUserId, String username, String module, String action,
            String recordId, String recordType, String departmentId, String projectId,
            String ipAddress, String reason) {
        persist(systemUserId, username, module, action, recordId, recordType,
                departmentId, projectId, ipAddress, reason, "DENIED");
    }

    private void persist(String systemUserId, String username, String module, String action,
            String recordId, String recordType, String departmentId, String projectId,
            String ipAddress, String details, String outcome) {
        RbacAuditLog log = new RbacAuditLog();
        log.setSystemUserId(systemUserId);
        log.setUsername(username != null ? username : "unknown");
        log.setModule(module);
        log.setAction(action);
        log.setRecordId(recordId);
        log.setRecordType(recordType);
        log.setDepartmentId(departmentId);
        log.setProjectId(projectId);
        log.setIpAddress(ipAddress);
        log.setDetails(details);
        log.setOutcome(outcome);
        rbacAuditLogDAO.insert(log);
    }
}
