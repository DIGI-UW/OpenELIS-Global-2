package org.openelisglobal.virology.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Process Step 1: Media Preparation Log materials used (type, lot number,
 * expiry)
 */
@Entity
@Table(name = "virus_culture_media_preparation")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class VirusCultureMediaPreparation extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "virus_culture_media_prep_seq")
    @SequenceGenerator(name = "virus_culture_media_prep_seq", sequenceName = "virus_culture_media_prep_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "culture_batch_id", nullable = false)
    @JsonIgnore
    private VirusCultureBatch cultureBatch;

    @NotNull
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "media_type", nullable = false)
    private String mediaType;

    @NotNull
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "lot_number", nullable = false, length = 100)
    private String lotNumber;

    @NotNull
    @Column(name = "expiry_date", nullable = false)
    private Date expiryDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "supplier")
    private String supplier;

    @Column(name = "volume_ml", precision = 10, scale = 2)
    private BigDecimal volumeMl;

    @Column(name = "preparation_date")
    private Timestamp preparationDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "equipment_used", columnDefinition = "TEXT")
    private String equipmentUsed;

    @Column(name = "ph_level", precision = 4, scale = 2)
    private BigDecimal phLevel;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "sterility_test_result", length = 20)
    private String sterilityTestResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prepared_by")
    @JsonIgnore
    private SystemUser preparedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_checked_by")
    @JsonIgnore
    private SystemUser qualityCheckedBy;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Constructors, getters, setters, utility methods
    public VirusCultureMediaPreparation() {
        super();
    }

    public VirusCultureMediaPreparation(VirusCultureBatch cultureBatch, String mediaType, String lotNumber,
            Date expiryDate) {
        this();
        this.cultureBatch = cultureBatch;
        this.mediaType = mediaType;
        this.lotNumber = lotNumber;
        this.expiryDate = expiryDate;
        this.preparationDate = new Timestamp(System.currentTimeMillis());
    }

    @PrePersist
    protected void onCreate() {
        if (preparationDate == null) {
            preparationDate = new Timestamp(System.currentTimeMillis());
        }
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public VirusCultureBatch getCultureBatch() {
        return cultureBatch;
    }

    public void setCultureBatch(VirusCultureBatch cultureBatch) {
        this.cultureBatch = cultureBatch;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getLotNumber() {
        return lotNumber;
    }

    public void setLotNumber(String lotNumber) {
        this.lotNumber = lotNumber;
    }

    public Date getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(Date expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getSupplier() {
        return supplier;
    }

    public void setSupplier(String supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getVolumeMl() {
        return volumeMl;
    }

    public void setVolumeMl(BigDecimal volumeMl) {
        this.volumeMl = volumeMl;
    }

    public Timestamp getPreparationDate() {
        return preparationDate;
    }

    public void setPreparationDate(Timestamp preparationDate) {
        this.preparationDate = preparationDate;
    }

    public String getEquipmentUsed() {
        return equipmentUsed;
    }

    public void setEquipmentUsed(String equipmentUsed) {
        this.equipmentUsed = equipmentUsed;
    }

    public BigDecimal getPhLevel() {
        return phLevel;
    }

    public void setPhLevel(BigDecimal phLevel) {
        this.phLevel = phLevel;
    }

    public String getSterilityTestResult() {
        return sterilityTestResult;
    }

    public void setSterilityTestResult(String sterilityTestResult) {
        this.sterilityTestResult = sterilityTestResult;
    }

    public SystemUser getPreparedBy() {
        return preparedBy;
    }

    public void setPreparedBy(SystemUser preparedBy) {
        this.preparedBy = preparedBy;
    }

    public SystemUser getQualityCheckedBy() {
        return qualityCheckedBy;
    }

    public void setQualityCheckedBy(SystemUser qualityCheckedBy) {
        this.qualityCheckedBy = qualityCheckedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.before(new Date(System.currentTimeMillis()));
    }

    public boolean isSterile() {
        return "PASS".equalsIgnoreCase(sterilityTestResult);
    }
}