package org.openelisglobal.rbac.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * RbacAuditLog - immutable audit trail for all RBAC-controlled actions (TR-06).
 * Protected by database trigger preventing UPDATE/DELETE.
 */
@Entity
@Table(name = "rbac_audit_log", schema = "clinlims")
public class RbacAuditLog extends BaseObject<Long> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "action_timestamp", insertable = false, updatable = false)
    private Timestamp actionTimestamp;

    @Column(name = "system_user_id", nullable = false, length = 50)
    private String systemUserId;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    /** Module: SAMPLE, STORAGE, INVENTORY, EQUIPMENT, REPORTING, QC, DISPOSAL */
    @Column(name = "module", nullable = false, length = 50)
    private String module;

    /** Action: CREATE, READ, UPDATE, DELETE, VALIDATE, TRANSFER, RETRIEVE, DISPOSE, etc. */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "record_id", length = 100)
    private String recordId;

    @Column(name = "record_type", length = 100)
    private String recordType;

    @Column(name = "department_id", length = 255)
    private String departmentId;

    @Column(name = "project_id", length = 255)
    private String projectId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    /** SUCCESS or DENIED */
    @Column(name = "outcome", nullable = false, length = 20)
    private String outcome = "SUCCESS";

    @Override
    public Long getId() { return id; }

    @Override
    public void setId(Long id) { this.id = id; }

    public Timestamp getActionTimestamp() { return actionTimestamp; }
    public String getSystemUserId() { return systemUserId; }
    public void setSystemUserId(String systemUserId) { this.systemUserId = systemUserId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getRecordId() { return recordId; }
    public void setRecordId(String recordId) { this.recordId = recordId; }
    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }
    public String getDepartmentId() { return departmentId; }
    public void setDepartmentId(String departmentId) { this.departmentId = departmentId; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String outcome) { this.outcome = outcome; }
}
