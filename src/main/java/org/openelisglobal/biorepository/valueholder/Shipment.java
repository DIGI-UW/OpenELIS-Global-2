package org.openelisglobal.biorepository.valueholder;

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
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Entity representing an incoming shipment to the biorepository. Tracks
 * delivery information, packaging condition, and associated samples. Part of
 * the ISO 20387:2018 compliant sample intake workflow.
 */
@Entity
@Table(name = "shipment", schema = "clinlims")
public class Shipment extends BaseObject<Integer> {

    /**
     * Status of the shipment processing.
     */
    public enum ShipmentStatus {
        RECEIVED, // Initial state when shipment is logged
        PROCESSING, // Samples are being registered
        COMPLETED // All samples registered and verified
    }

    /**
     * Status of documentation verification for the shipment.
     */
    public enum DocumentationStatus {
        PENDING, // Verification not yet started
        VERIFIED, // All documentation checks passed
        QUARANTINE // Verification failed, shipment quarantined
    }

    /**
     * Condition of the packaging on arrival.
     */
    public enum PackagingCondition {
        INTACT, // Packaging in good condition
        DAMAGED // Packaging shows signs of damage
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shipment_generator")
    @SequenceGenerator(name = "shipment_generator", sequenceName = "shipment_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @NotBlank(message = "Delivery reference is required")
    @Size(max = 100)
    @Column(name = "delivery_reference", nullable = false, length = 100)
    private String deliveryReference;

    @NotBlank(message = "Sender name is required")
    @Size(max = 255)
    @Column(name = "sender_name", nullable = false, length = 255)
    private String senderName;

    @Size(max = 255)
    @Column(name = "sender_organization", length = 255)
    private String senderOrganization;

    @NotNull(message = "Receiver is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private SystemUser receiver;

    @NotNull(message = "Reception timestamp is required")
    @Column(name = "reception_timestamp", nullable = false)
    private Timestamp receptionTimestamp;

    @NotNull(message = "Packaging condition is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "packaging_condition", nullable = false, length = 20)
    private PackagingCondition packagingCondition;

    @Column(name = "packaging_condition_notes", columnDefinition = "TEXT")
    private String packagingConditionNotes;

    @Size(max = 500)
    @Column(name = "packaging_photo_path", length = 500)
    private String packagingPhotoPath;

    @Column(name = "transport_temperature", precision = 5, scale = 2)
    private BigDecimal transportTemperature;

    @Column(name = "expected_sample_count")
    private Integer expectedSampleCount;

    @Column(name = "actual_sample_count")
    private Integer actualSampleCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private ShipmentStatus status = ShipmentStatus.RECEIVED;

    @NotNull(message = "Documentation status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "documentation_status", nullable = false, length = 20)
    private DocumentationStatus documentationStatus = DocumentationStatus.PENDING;

    @Column(name = "sys_user_id", nullable = false, length = 36)
    private String sysUserId;

    @OneToMany(mappedBy = "shipment", fetch = FetchType.LAZY)
    private List<BioSample> samples = new ArrayList<>();

    // Default constructor required by JPA
    public Shipment() {
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

    public String getDeliveryReference() {
        return deliveryReference;
    }

    public void setDeliveryReference(String deliveryReference) {
        this.deliveryReference = deliveryReference;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderOrganization() {
        return senderOrganization;
    }

    public void setSenderOrganization(String senderOrganization) {
        this.senderOrganization = senderOrganization;
    }

    public SystemUser getReceiver() {
        return receiver;
    }

    public void setReceiver(SystemUser receiver) {
        this.receiver = receiver;
    }

    public Timestamp getReceptionTimestamp() {
        return receptionTimestamp;
    }

    public void setReceptionTimestamp(Timestamp receptionTimestamp) {
        this.receptionTimestamp = receptionTimestamp;
    }

    public PackagingCondition getPackagingCondition() {
        return packagingCondition;
    }

    public void setPackagingCondition(PackagingCondition packagingCondition) {
        this.packagingCondition = packagingCondition;
    }

    public String getPackagingConditionNotes() {
        return packagingConditionNotes;
    }

    public void setPackagingConditionNotes(String packagingConditionNotes) {
        this.packagingConditionNotes = packagingConditionNotes;
    }

    public String getPackagingPhotoPath() {
        return packagingPhotoPath;
    }

    public void setPackagingPhotoPath(String packagingPhotoPath) {
        this.packagingPhotoPath = packagingPhotoPath;
    }

    public BigDecimal getTransportTemperature() {
        return transportTemperature;
    }

    public void setTransportTemperature(BigDecimal transportTemperature) {
        this.transportTemperature = transportTemperature;
    }

    public Integer getExpectedSampleCount() {
        return expectedSampleCount;
    }

    public void setExpectedSampleCount(Integer expectedSampleCount) {
        this.expectedSampleCount = expectedSampleCount;
    }

    public Integer getActualSampleCount() {
        return actualSampleCount;
    }

    public void setActualSampleCount(Integer actualSampleCount) {
        this.actualSampleCount = actualSampleCount;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }

    public DocumentationStatus getDocumentationStatus() {
        return documentationStatus;
    }

    public void setDocumentationStatus(DocumentationStatus documentationStatus) {
        this.documentationStatus = documentationStatus;
    }

    public List<BioSample> getSamples() {
        return samples;
    }

    public void setSamples(List<BioSample> samples) {
        this.samples = samples;
    }

    /**
     * Checks if packaging was damaged on arrival.
     * 
     * @return true if packaging condition is DAMAGED
     */
    public boolean isPackagingDamaged() {
        return PackagingCondition.DAMAGED.equals(packagingCondition);
    }

    /**
     * Checks if all expected samples have been registered.
     * 
     * @return true if actual count equals expected count
     */
    public boolean isComplete() {
        return expectedSampleCount != null && actualSampleCount != null
                && expectedSampleCount.equals(actualSampleCount);
    }
}
