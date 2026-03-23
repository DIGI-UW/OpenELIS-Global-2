package org.openelisglobal.patient.merge.valueholder;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import java.sql.Timestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.type.JsonBinaryType;

@Entity
@Table(name = "patient_merge_audit")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class PatientMergeAudit extends BaseObject<Long> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "primary_patient_id", nullable = false)
    private Long primaryPatientId;

    @Column(name = "merged_patient_id", nullable = false)
    private Long mergedPatientId;

    @Column(name = "merge_date", nullable = false)
    private Timestamp mergeDate;

    @Column(name = "performed_by_user_id", nullable = false)
    private Long performedByUserId;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Type(type = "jsonb")
    @Column(name = "data_summary", columnDefinition = "jsonb")
    private JsonNode dataSummary;

    @Column(name = "sys_user_id")
    private Long sysUserId;

    public PatientMergeAudit() {
        super();
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getPrimaryPatientId() {
        return primaryPatientId;
    }

    public void setPrimaryPatientId(Long primaryPatientId) {
        this.primaryPatientId = primaryPatientId;
    }

    public Long getMergedPatientId() {
        return mergedPatientId;
    }

    public void setMergedPatientId(Long mergedPatientId) {
        this.mergedPatientId = mergedPatientId;
    }

    public Timestamp getMergeDate() {
        return mergeDate;
    }

    public void setMergeDate(Timestamp mergeDate) {
        this.mergeDate = mergeDate;
    }

    public Long getPerformedByUserId() {
        return performedByUserId;
    }

    public void setPerformedByUserId(Long performedByUserId) {
        this.performedByUserId = performedByUserId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public JsonNode getDataSummary() {
        return dataSummary;
    }

    public void setDataSummary(JsonNode dataSummary) {
        this.dataSummary = dataSummary;
    }

    @Override
    public String getSysUserId() {
        return sysUserId == null ? null : sysUserId.toString();
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId == null || sysUserId.isEmpty() ? null : Long.parseLong(sysUserId);
    }
}
