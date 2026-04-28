package org.openelisglobal.rbac.service;

/**
 * TR-06: Immutable audit logging for all RBAC-controlled actions.
 * Every access, create, update, validation, transfer, retrieval, disposal,
 * inventory transaction, and equipment action shall be time-stamped and user-attributed.
 */
public interface RbacAuditService {

    /** Log a successful action */
    void logAction(String systemUserId, String username, String module, String action,
            String recordId, String recordType, String departmentId, String projectId,
            String ipAddress, String details);

    /** Log a denied access attempt */
    void logDenied(String systemUserId, String username, String module, String action,
            String recordId, String recordType, String departmentId, String projectId,
            String ipAddress, String reason);
}
