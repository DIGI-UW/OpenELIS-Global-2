package org.openelisglobal.tb.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.hibernate.type.JsonBinaryType;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.tb.valueholder.TbEnums.SpecimenType;

/**
 * TB-specific sample registration metadata. Extends the base sample_item with
 * TB laboratory workflow requirements.
 */
@Entity
@Table(name = "tb_sample_registration")
@TypeDef(name = "jsonb", typeClass = JsonBinaryType.class)
public class TbSampleRegistration extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tb_sample_registration_generator")
    @SequenceGenerator(name = "tb_sample_registration_generator", sequenceName = "tb_sample_registration_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", nullable = false)
    private SampleItem sampleItem;

    @Column(name = "document_number", length = 100)
    private String documentNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "specimen_type", nullable = false, length = 50)
    private SpecimenType specimenType;

    @Column(name = "specimen_quality", length = 50)
    private String specimenQuality;

    @Column(name = "referring_facility", length = 255)
    private String referringFacility;

    @Column(name = "treatment_history", columnDefinition = "TEXT")
    private String treatmentHistory;

    @Column(name = "physician_phone", length = 50)
    private String physicianPhone;

    @Column(name = "patient_phone", length = 50)
    private String patientPhone;

    @Column(name = "consent_status", length = 50)
    private String consentStatus;

    @Type(type = "jsonb")
    @Column(name = "test_requested", columnDefinition = "jsonb")
    private String testRequested;

    @Column(name = "received_site", length = 255)
    private String receivedSite;

    @Column(name = "received_datetime")
    private Timestamp receivedDatetime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private SystemUser registeredBy;

    @Column(name = "date_created")
    private Timestamp dateCreated;

    // Getters and setters
    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
    }

    public String getSampleItemId() {
        return sampleItem != null ? sampleItem.getId() : null;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public SpecimenType getSpecimenType() {
        return specimenType;
    }

    public void setSpecimenType(SpecimenType specimenType) {
        this.specimenType = specimenType;
    }

    public String getSpecimenQuality() {
        return specimenQuality;
    }

    public void setSpecimenQuality(String specimenQuality) {
        this.specimenQuality = specimenQuality;
    }

    public String getReferringFacility() {
        return referringFacility;
    }

    public void setReferringFacility(String referringFacility) {
        this.referringFacility = referringFacility;
    }

    public String getTreatmentHistory() {
        return treatmentHistory;
    }

    public void setTreatmentHistory(String treatmentHistory) {
        this.treatmentHistory = treatmentHistory;
    }

    public String getPhysicianPhone() {
        return physicianPhone;
    }

    public void setPhysicianPhone(String physicianPhone) {
        this.physicianPhone = physicianPhone;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getConsentStatus() {
        return consentStatus;
    }

    public void setConsentStatus(String consentStatus) {
        this.consentStatus = consentStatus;
    }

    public String getTestRequested() {
        return testRequested;
    }

    public void setTestRequested(String testRequested) {
        this.testRequested = testRequested;
    }

    public String getReceivedSite() {
        return receivedSite;
    }

    public void setReceivedSite(String receivedSite) {
        this.receivedSite = receivedSite;
    }

    public Timestamp getReceivedDatetime() {
        return receivedDatetime;
    }

    public void setReceivedDatetime(Timestamp receivedDatetime) {
        this.receivedDatetime = receivedDatetime;
    }

    public SystemUser getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(SystemUser registeredBy) {
        this.registeredBy = registeredBy;
    }

    public Timestamp getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Timestamp dateCreated) {
        this.dateCreated = dateCreated;
    }
}
