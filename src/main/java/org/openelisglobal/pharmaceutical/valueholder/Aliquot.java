package org.openelisglobal.pharmaceutical.valueholder;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Aliquot entity - Child portions of a PharmaceuticalSample.
 * Tracks volume/weight, freeze-thaw cycles, and storage assignment.
 */
@Entity
@Table(name = "PHARMA_ALIQUOT")
@DynamicUpdate
public class Aliquot extends BaseObject<Integer> {

    public enum AliquotStatus {
        AVAILABLE, RESERVED, IN_USE, CONSUMED, DISPOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "pharma_aliquot_seq")
    @SequenceGenerator(name = "pharma_aliquot_seq", sequenceName = "pharma_aliquot_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @Column(name = "FHIR_UUID", columnDefinition = "uuid")
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_SAMPLE_ID", nullable = false)
    private PharmaceuticalSample parentSample;

    @Column(name = "ALIQUOT_CODE", length = 20, nullable = false)
    private String aliquotCode;

    @Column(name = "VOLUME_WEIGHT", precision = 10, scale = 4)
    private BigDecimal volumeWeight;

    @Column(name = "UNITS", length = 20)
    private String units;

    @Column(name = "FREEZE_THAW_COUNT", nullable = false)
    private Integer freezeThawCount = 0;

    @Column(name = "FREEZE_THAW_LIMIT")
    private Integer freezeThawLimit;

    @Column(name = "LABEL_DATA", length = 500)
    private String labelData;

    @Column(name = "BARCODE", length = 100)
    private String barcode;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", length = 20, nullable = false)
    private AliquotStatus status = AliquotStatus.AVAILABLE;

    @Column(name = "STORAGE_LOCATION_ID")
    private Integer storageLocationId;

    @Column(name = "STORAGE_LOCATION_TYPE", length = 20)
    private String storageLocationType;

    @Column(name = "STORAGE_POSITION", length = 20)
    private String storagePosition;

    @Column(name = "CREATED_AT")
    private Timestamp createdAt;

    @Column(name = "NOTES", length = 1000)
    private String notes;

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

    public PharmaceuticalSample getParentSample() {
        return parentSample;
    }

    public void setParentSample(PharmaceuticalSample parentSample) {
        this.parentSample = parentSample;
    }

    public String getAliquotCode() {
        return aliquotCode;
    }

    public void setAliquotCode(String aliquotCode) {
        this.aliquotCode = aliquotCode;
    }

    public BigDecimal getVolumeWeight() {
        return volumeWeight;
    }

    public void setVolumeWeight(BigDecimal volumeWeight) {
        this.volumeWeight = volumeWeight;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public Integer getFreezeThawCount() {
        return freezeThawCount;
    }

    public void setFreezeThawCount(Integer freezeThawCount) {
        this.freezeThawCount = freezeThawCount;
    }

    public Integer getFreezeThawLimit() {
        return freezeThawLimit;
    }

    public void setFreezeThawLimit(Integer freezeThawLimit) {
        this.freezeThawLimit = freezeThawLimit;
    }

    public String getLabelData() {
        return labelData;
    }

    public void setLabelData(String labelData) {
        this.labelData = labelData;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public AliquotStatus getStatus() {
        return status;
    }

    public void setStatus(AliquotStatus status) {
        this.status = status;
    }

    public Integer getStorageLocationId() {
        return storageLocationId;
    }

    public void setStorageLocationId(Integer storageLocationId) {
        this.storageLocationId = storageLocationId;
    }

    public String getStorageLocationType() {
        return storageLocationType;
    }

    public void setStorageLocationType(String storageLocationType) {
        this.storageLocationType = storageLocationType;
    }

    public String getStoragePosition() {
        return storagePosition;
    }

    public void setStoragePosition(String storagePosition) {
        this.storagePosition = storagePosition;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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
        if (createdAt == null) {
            createdAt = new Timestamp(System.currentTimeMillis());
        }
    }

    /**
     * Increments freeze-thaw count and checks if limit exceeded.
     * @return true if limit exceeded after increment
     */
    public boolean incrementFreezeThaw() {
        this.freezeThawCount++;
        return freezeThawLimit != null && freezeThawCount > freezeThawLimit;
    }

    /**
     * Checks if freeze-thaw limit is exceeded.
     * @return true if limit exceeded
     */
    public boolean isFreezeThawLimitExceeded() {
        return freezeThawLimit != null && freezeThawCount >= freezeThawLimit;
    }
}
