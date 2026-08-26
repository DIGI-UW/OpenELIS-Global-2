package org.openelisglobal.microbiology.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.converter.StringToIntegerConverter;

@Entity
@Table(name = "micro_case_order_detail", schema = "clinlims")
public class MicroCaseOrderDetail extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", length = 36)
    private String id = UUID.randomUUID().toString();

    @Column(name = "case_id", length = 36, unique = true)
    private String caseId;

    @Convert(converter = StringToIntegerConverter.class)
    @Column(name = "sample_id", unique = true)
    private String sampleId;

    @Column(name = "culture_method_id", length = 20)
    private String cultureMethodId;

    @Column(name = "patient_origin", length = 255)
    private String patientOrigin;

    @Column(name = "culture_purpose", length = 32)
    private String culturePurpose;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @Column(name = "number_of_sets")
    private Integer numberOfSets;

    @Column(name = "clinical_history")
    private String clinicalHistory;

    @Column(name = "antibiotic_exposure")
    private Boolean antibioticExposure;

    @Column(name = "critical_notification_preference")
    private Boolean criticalNotificationPreference;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt = new Timestamp(System.currentTimeMillis());

    @Column(name = "created_by", length = 20)
    private String createdBy;

    @Column(name = "updated_at")
    private Timestamp updatedAt;

    @Column(name = "updated_by", length = 20)
    private String updatedBy;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getCaseId() {
        return caseId;
    }

    public void setCaseId(String caseId) {
        this.caseId = caseId;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getCultureMethodId() {
        return cultureMethodId;
    }

    public void setCultureMethodId(String cultureMethodId) {
        this.cultureMethodId = cultureMethodId;
    }

    public String getPatientOrigin() {
        return patientOrigin;
    }

    public void setPatientOrigin(String patientOrigin) {
        this.patientOrigin = patientOrigin;
    }

    public String getCulturePurpose() {
        return culturePurpose;
    }

    public void setCulturePurpose(String culturePurpose) {
        this.culturePurpose = culturePurpose;
    }

    public LocalDate getAdmissionDate() {
        return admissionDate;
    }

    public void setAdmissionDate(LocalDate admissionDate) {
        this.admissionDate = admissionDate;
    }

    public Integer getNumberOfSets() {
        return numberOfSets;
    }

    public void setNumberOfSets(Integer numberOfSets) {
        this.numberOfSets = numberOfSets;
    }

    public String getClinicalHistory() {
        return clinicalHistory;
    }

    public void setClinicalHistory(String clinicalHistory) {
        this.clinicalHistory = clinicalHistory;
    }

    public Boolean getAntibioticExposure() {
        return antibioticExposure;
    }

    public void setAntibioticExposure(Boolean antibioticExposure) {
        this.antibioticExposure = antibioticExposure;
    }

    public Boolean getCriticalNotificationPreference() {
        return criticalNotificationPreference;
    }

    public void setCriticalNotificationPreference(Boolean criticalNotificationPreference) {
        this.criticalNotificationPreference = criticalNotificationPreference;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
