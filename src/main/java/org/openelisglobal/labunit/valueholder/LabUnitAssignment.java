package org.openelisglobal.labunit.valueholder;

import java.time.LocalDateTime;

@jakarta.persistence.Entity
@jakarta.persistence.Table(name = "lab_unit_assignment")
@jakarta.persistence.IdClass(value = LabUnitAssignmentId.class)
public class LabUnitAssignment {
    @jakarta.persistence.Id
    @jakarta.persistence.Column(name = "id")
    private Long id;

    @jakarta.persistence.Id
    @jakarta.persistence.Column(name = "lab_unit_id")
    private String labUnitId;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "lab_unit_id", referencedColumnName = "id", insertable = false, updatable = false)
    private LabUnit labUnit;

    @jakarta.persistence.Column(name = "assignment_type", length = 20, nullable = false)
    private String assignmentType;

    @jakarta.persistence.Column(name = "assigned_item_id", length = 36, nullable = false)
    private String assignedItemId;

    @jakarta.persistence.Column(name = "assigned_date")
    private LocalDateTime assignedDate;

    @jakarta.persistence.Column(name = "sys_user_id", length = 36)
    private String sysUserId;

    public LabUnitAssignment() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LabUnit getLabUnit() {
        return labUnit;
    }

    public void setLabUnit(LabUnit labUnit) {
        this.labUnit = labUnit;
    }

    public String getLabUnitId() {
        return labUnitId;
    }

    public void setLabUnitId(String labUnitId) {
        this.labUnitId = labUnitId;
    }

    public String getAssignmentType() {
        return assignmentType;
    }

    public void setAssignmentType(String assignmentType) {
        this.assignmentType = assignmentType;
    }

    public String getAssignedItemId() {
        return assignedItemId;
    }

    public void setAssignedItemId(String assignedItemId) {
        this.assignedItemId = assignedItemId;
    }

    public LocalDateTime getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDateTime assignedDate) {
        this.assignedDate = assignedDate;
    }

    public String getSysUserId() {
        return sysUserId;
    }

    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }
}