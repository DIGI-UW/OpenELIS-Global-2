package org.openelisglobal.biorepository.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.sql.Date;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Biorepository extension record for SampleItem. Contains ONLY fields specific
 * to biorepository sample management that don't exist in the core
 * Sample/SampleItem models.
 *
 * Core sample data (barcode, type, quantity, receipt date, source) is stored in
 * Sample and SampleItem entities for global OpenELIS visibility.
 *
 * This entity stores biorepository-specific metadata: - Biosafety
 * classification (WHO Laboratory Biosafety Manual) - Ethics/consent
 * documentation references - Material Transfer Agreement tracking - Storage
 * temperature requirements - Retention policy and expiry - Preservation medium
 *
 * Part of ISO 20387:2018 compliant sample tracking.
 */
@Entity
@Table(name = "bio_sample", schema = "clinlims")
public class BioSample extends BaseObject<Integer> {

    /**
     * Biosafety level classification per WHO Laboratory Biosafety Manual.
     */
    public enum BiosafetyLevel {
        BSL_1("BSL-1"), BSL_2("BSL-2"), BSL_3("BSL-3"), BSL_4("BSL-4");

        private final String displayValue;

        BiosafetyLevel(String displayValue) {
            this.displayValue = displayValue;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        public static BiosafetyLevel fromString(String value) {
            for (BiosafetyLevel level : values()) {
                if (level.displayValue.equals(value) || level.name().equals(value)) {
                    return level;
                }
            }
            throw new IllegalArgumentException("Unknown biosafety level: " + value);
        }
    }

    /**
     * Workflow status for biorepository samples tracking their progression through
     * the storage lifecycle.
     */
    public enum WorkflowStatus {
        /** Sample registered at Intake (Stage 1) */
        REGISTERED("Registered"),
        /** Sample advanced to Storage Assignment queue (pending Stage 2) */
        PENDING_STORAGE("Pending Storage"),
        /** Sample has been assigned to a storage location (Stage 2 complete) */
        STORED("Stored"),
        /** Sample retrieved from storage for use/analysis */
        IN_USE("In Use"),
        /** Sample has been disposed/destroyed */
        DISPOSED("Disposed");

        private final String displayValue;

        WorkflowStatus(String displayValue) {
            this.displayValue = displayValue;
        }

        public String getDisplayValue() {
            return displayValue;
        }

        public static WorkflowStatus fromString(String value) {
            if (value == null) {
                return null;
            }
            for (WorkflowStatus status : values()) {
                if (status.displayValue.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                    return status;
                }
            }
            return null;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bio_sample_generator")
    @SequenceGenerator(name = "bio_sample_generator", sequenceName = "bio_sample_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    /**
     * Link to core OpenELIS SampleItem. This is the primary sample record that
     * provides global visibility across OpenELIS modules.
     *
     * SampleItem contains: barcode (externalId), typeOfSample, sourceOfSample,
     * quantity, collectionDate, statusId
     *
     * SampleItem.sample contains: accessionNumber, receivedTimestamp, sysUserId
     */
    @NotNull(message = "Sample item link is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private SampleItem sampleItem;

    /**
     * Link to the shipment this sample arrived in.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Shipment shipment;

    // ========================================
    // Biorepository-specific fields
    // (Not available in Sample/SampleItem)
    // ========================================

    /**
     * Biosafety level classification (BSL-1 through BSL-4). Required for all
     * biorepository samples.
     */
    @NotNull(message = "Biosafety level is required")
    @Convert(converter = BiosafetyLevelConverter.class)
    @Column(name = "biosafety_level", nullable = false, length = 10)
    private BiosafetyLevel biosafetyLevel;

    /**
     * Ethics/IRB approval reference number. Required for human samples per ethics
     * requirements.
     */
    @Size(max = 100)
    @Column(name = "ethics_approval_ref", length = 100)
    private String ethicsApprovalRef;

    /**
     * Material Transfer Agreement reference. Required for samples from external
     * sources.
     */
    @Size(max = 100)
    @Column(name = "mta_reference", length = 100)
    private String mtaReference;

    /**
     * Informed consent record identifier. Required for human samples.
     */
    @Size(max = 100)
    @Column(name = "consent_id", length = 100)
    private String consentId;

    /**
     * Principal investigator responsible for this sample.
     */
    @Size(max = 255)
    @Column(name = "principal_investigator", length = 255)
    private String principalInvestigator;

    /**
     * Origin laboratory where sample was collected/created.
     */
    @Size(max = 255)
    @Column(name = "origin_lab", length = 255)
    private String originLab;

    /**
     * Project or study identifier this sample belongs to.
     */
    @Size(max = 100)
    @Column(name = "project_id", length = 100)
    private String projectId;

    // ========================================
    // Storage requirements
    // ========================================

    /**
     * Minimum required storage temperature in Celsius.
     */
    @Column(name = "required_temp_min", precision = 5, scale = 2)
    private BigDecimal requiredTempMin;

    /**
     * Maximum required storage temperature in Celsius.
     */
    @Column(name = "required_temp_max", precision = 5, scale = 2)
    private BigDecimal requiredTempMax;

    /**
     * Preservation medium (e.g., "EDTA", "Heparin", "Cryoprotectant").
     */
    @Size(max = 100)
    @Column(name = "preservation_medium", length = 100)
    private String preservationMedium;

    /**
     * Special handling instructions (e.g., "Light sensitive", "Biohazard").
     */
    @Column(name = "special_handling", columnDefinition = "TEXT")
    private String specialHandling;

    // ========================================
    // Retention policy
    // ========================================

    /**
     * Retention policy ID defining how long sample must be kept.
     */
    @Column(name = "retention_policy_id")
    private Integer retentionPolicyId;

    /**
     * Date when retention period expires and sample can be disposed.
     */
    @Column(name = "retention_expiry_date")
    private Date retentionExpiryDate;

    // ========================================
    // Workflow status
    // ========================================

    /**
     * Current workflow status tracking sample progression through the biorepository
     * lifecycle: Intake → Storage Assignment → Stored → In Use → Disposed.
     */
    @Column(name = "workflow_status", length = 20)
    @Convert(converter = WorkflowStatusConverter.class)
    private WorkflowStatus workflowStatus = WorkflowStatus.REGISTERED;

    // ========================================
    // Arrival condition (biorepository-specific)
    // ========================================

    /**
     * Condition of sample upon arrival (e.g., "Intact", "Thawed", "Hemolyzed").
     */
    @Size(max = 50)
    @Column(name = "arrival_condition", length = 50)
    private String arrivalCondition;

    /**
     * Additional notes about arrival condition.
     */
    @Column(name = "arrival_condition_notes", columnDefinition = "TEXT")
    private String arrivalConditionNotes;

    @Column(name = "sys_user_id", nullable = false, length = 36)
    private String sysUserId;

    // Default constructor required by JPA
    public BioSample() {
    }

    @Override
    public String getSysUserId() {
        return sysUserId;
    }

    @Override
    public void setSysUserId(String sysUserId) {
        this.sysUserId = sysUserId;
    }

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

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public BiosafetyLevel getBiosafetyLevel() {
        return biosafetyLevel;
    }

    public void setBiosafetyLevel(BiosafetyLevel biosafetyLevel) {
        this.biosafetyLevel = biosafetyLevel;
    }

    public String getEthicsApprovalRef() {
        return ethicsApprovalRef;
    }

    public void setEthicsApprovalRef(String ethicsApprovalRef) {
        this.ethicsApprovalRef = ethicsApprovalRef;
    }

    public String getMtaReference() {
        return mtaReference;
    }

    public void setMtaReference(String mtaReference) {
        this.mtaReference = mtaReference;
    }

    public String getConsentId() {
        return consentId;
    }

    public void setConsentId(String consentId) {
        this.consentId = consentId;
    }

    public String getPrincipalInvestigator() {
        return principalInvestigator;
    }

    public void setPrincipalInvestigator(String principalInvestigator) {
        this.principalInvestigator = principalInvestigator;
    }

    public String getOriginLab() {
        return originLab;
    }

    public void setOriginLab(String originLab) {
        this.originLab = originLab;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public BigDecimal getRequiredTempMin() {
        return requiredTempMin;
    }

    public void setRequiredTempMin(BigDecimal requiredTempMin) {
        this.requiredTempMin = requiredTempMin;
    }

    public BigDecimal getRequiredTempMax() {
        return requiredTempMax;
    }

    public void setRequiredTempMax(BigDecimal requiredTempMax) {
        this.requiredTempMax = requiredTempMax;
    }

    public String getPreservationMedium() {
        return preservationMedium;
    }

    public void setPreservationMedium(String preservationMedium) {
        this.preservationMedium = preservationMedium;
    }

    public String getSpecialHandling() {
        return specialHandling;
    }

    public void setSpecialHandling(String specialHandling) {
        this.specialHandling = specialHandling;
    }

    public Integer getRetentionPolicyId() {
        return retentionPolicyId;
    }

    public void setRetentionPolicyId(Integer retentionPolicyId) {
        this.retentionPolicyId = retentionPolicyId;
    }

    public Date getRetentionExpiryDate() {
        return retentionExpiryDate;
    }

    public void setRetentionExpiryDate(Date retentionExpiryDate) {
        this.retentionExpiryDate = retentionExpiryDate;
    }

    public WorkflowStatus getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(WorkflowStatus workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public String getArrivalCondition() {
        return arrivalCondition;
    }

    public void setArrivalCondition(String arrivalCondition) {
        this.arrivalCondition = arrivalCondition;
    }

    public String getArrivalConditionNotes() {
        return arrivalConditionNotes;
    }

    public void setArrivalConditionNotes(String arrivalConditionNotes) {
        this.arrivalConditionNotes = arrivalConditionNotes;
    }

    // ========================================
    // Convenience methods to access core sample data
    // These are marked @JsonIgnore to prevent lazy loading during serialization.
    // Use the DTO pattern (BioSampleListDTO) for API responses with enriched data.
    // ========================================

    /**
     * Gets the barcode/sample ID from the linked SampleItem.
     *
     * @return the external ID (barcode) from SampleItem
     */
    @JsonIgnore
    public String getBarcode() {
        return sampleItem != null ? sampleItem.getExternalId() : null;
    }

    /**
     * Gets the accession number from the linked Sample.
     *
     * @return the accession number
     */
    @JsonIgnore
    public String getAccessionNumber() {
        return sampleItem != null && sampleItem.getSample() != null ? sampleItem.getSample().getAccessionNumber()
                : null;
    }

    /**
     * Checks if sample is an aliquot (has a parent in SampleItem hierarchy).
     *
     * @return true if this is an aliquot
     */
    @JsonIgnore
    public boolean isAliquot() {
        return sampleItem != null && sampleItem.isAliquot();
    }
}
