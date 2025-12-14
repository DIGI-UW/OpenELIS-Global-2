package org.openelisglobal.pharmaceutical.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * PharmaceuticalSample entity - Core sample registration for pharmaceutical laboratory.
 * Captures metadata for pharmaceutical, herbal, biological, and environmental specimens.
 */
@Entity
@Table(name = "PHARMACEUTICAL_SAMPLE")
@DynamicUpdate
public class PharmaceuticalSample extends BaseObject<Integer> {

    public enum SampleStatus {
        REGISTERED, QC_PASSED, QC_FAILED, PROCESSING, STORED, IN_ASSAY, COMPLETED, DISPOSED
    }

    public enum LabType {
        PHARMACEUTICAL, BIOLOGICAL, MICROBIOLOGICAL, ENVIRONMENTAL, HERBAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pharma_sample_seq")
    @SequenceGenerator(name = "pharma_sample_seq", sequenceName = "pharma_sample_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "FHIR_UUID", columnDefinition = "uuid")
    private UUID fhirUuid;

    @Column(name = "UNIQUE_SAMPLE_ID", length = 50, nullable = false, unique = true)
    private String uniqueSampleId;

    @Column(name = "SAMPLE_NAME", length = 255, nullable = false)
    private String sampleName;

    @Column(name = "IUPAC_NAME", length = 500)
    private String iupacName;

    @Column(name = "GRADE_SPEC", length = 100)
    private String gradeSpec;

    @Column(name = "LOT_BATCH", length = 100)
    private String lotBatch;

    @Column(name = "MANUFACTURE_DATE")
    private Timestamp manufactureDate;

    @Column(name = "EXPIRY_RETEST_DATE")
    private Timestamp expiryRetestDate;

    @Column(name = "STORAGE_CONDITION", length = 100)
    private String storageCondition;

    @Column(name = "OWNER_REQUESTER", length = 255)
    private String ownerRequester;

    @Column(name = "CHAIN_OF_CUSTODY_REF", length = 100)
    private String chainOfCustodyRef;

    @Column(name = "CLINICAL_TRIAL_ID", length = 100)
    private String clinicalTrialId;

    @Column(name = "PATIENT_ID", length = 100)
    private String patientId;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20, nullable = false)
    private SampleStatus status = SampleStatus.REGISTERED;

    @Enumerated(EnumType.STRING)
    @Column(name = "LAB_TYPE", length = 20, nullable = false)
    private LabType labType = LabType.PHARMACEUTICAL;

    @Column(name = "BARCODE", length = 100)
    private String barcode;

    @Column(name = "QR_CODE", length = 500)
    private String qrCode;

    @Column(name = "NOTES", length = 2000)
    private String notes;

    @Column(name = "REGISTERED_AT")
    private Timestamp registeredAt;

    @Column(name = "SYS_USER_ID", nullable = false, length = 36)
    private String sysUserIdValue;

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getUniqueSampleId() {
        return uniqueSampleId;
    }

    public void setUniqueSampleId(String uniqueSampleId) {
        this.uniqueSampleId = uniqueSampleId;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public String getIupacName() {
        return iupacName;
    }

    public void setIupacName(String iupacName) {
        this.iupacName = iupacName;
    }

    public String getGradeSpec() {
        return gradeSpec;
    }

    public void setGradeSpec(String gradeSpec) {
        this.gradeSpec = gradeSpec;
    }

    public String getLotBatch() {
        return lotBatch;
    }

    public void setLotBatch(String lotBatch) {
        this.lotBatch = lotBatch;
    }

    public Timestamp getManufactureDate() {
        return manufactureDate;
    }

    public void setManufactureDate(Timestamp manufactureDate) {
        this.manufactureDate = manufactureDate;
    }

    public Timestamp getExpiryRetestDate() {
        return expiryRetestDate;
    }

    public void setExpiryRetestDate(Timestamp expiryRetestDate) {
        this.expiryRetestDate = expiryRetestDate;
    }

    public String getStorageCondition() {
        return storageCondition;
    }

    public void setStorageCondition(String storageCondition) {
        this.storageCondition = storageCondition;
    }

    public String getOwnerRequester() {
        return ownerRequester;
    }

    public void setOwnerRequester(String ownerRequester) {
        this.ownerRequester = ownerRequester;
    }

    public String getChainOfCustodyRef() {
        return chainOfCustodyRef;
    }

    public void setChainOfCustodyRef(String chainOfCustodyRef) {
        this.chainOfCustodyRef = chainOfCustodyRef;
    }

    public String getClinicalTrialId() {
        return clinicalTrialId;
    }

    public void setClinicalTrialId(String clinicalTrialId) {
        this.clinicalTrialId = clinicalTrialId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public SampleStatus getStatus() {
        return status;
    }

    public void setStatus(SampleStatus status) {
        this.status = status;
    }

    public LabType getLabType() {
        return labType;
    }

    public void setLabType(LabType labType) {
        this.labType = labType;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Timestamp registeredAt) {
        this.registeredAt = registeredAt;
    }

    @Override
    public String getSysUserId() {
        return sysUserIdValue;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserIdValue = sysUserId;
    }

    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
        if (registeredAt == null) {
            registeredAt = new Timestamp(System.currentTimeMillis());
        }
    }
}
