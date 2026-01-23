package org.openelisglobal.virology.valueholder;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Timestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.validation.annotations.SafeHtml;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Process Step 9: Packaging
 * Track batch ID, packaging specs (vial type, fill volume, labeling)
 */
@Entity
@Table(name = "virus_culture_packaging")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class VirusCulturePackaging extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "virus_culture_packaging_seq")
    @SequenceGenerator(name = "virus_culture_packaging_seq", sequenceName = "virus_culture_packaging_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "culture_batch_id", nullable = false)
    @JsonIgnore
    private VirusCultureBatch cultureBatch;

    @NotNull
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "final_batch_id", nullable = false)
    private String finalBatchId;

    @NotNull
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "vial_type", nullable = false, length = 100)
    private String vialType;

    @Column(name = "vial_size_ml", precision = 6, scale = 2)
    private BigDecimal vialSizeMl;

    @Column(name = "fill_volume_ml", precision = 6, scale = 3)
    private BigDecimal fillVolumeMl;

    @Column(name = "vial_count")
    private Integer vialCount;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "cap_type", length = 100)
    private String capType;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "label_type", length = 100)
    private String labelType;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "label_content", columnDefinition = "TEXT")
    private String labelContent;

    @Column(name = "storage_temperature_celsius", precision = 6, scale = 2)
    private BigDecimal storageTemperatureCelsius;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "storage_location")
    private String storageLocation;

    @Column(name = "expiry_date")
    private Date expiryDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "package_integrity_check", length = 20)
    private String packageIntegrityCheck;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "final_titer_verification", length = 100)
    private String finalTiterVerification;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "sterility_test_result", length = 20)
    private String sterilityTestResult;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "endotoxin_test_result", length = 20)
    private String endotoxinTestResult;

    @Column(name = "packaging_date")
    private Timestamp packagingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "packaged_by")
    @JsonIgnore
    private SystemUser packagedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_checked_by")
    @JsonIgnore
    private SystemUser qcCheckedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "released_by")
    @JsonIgnore
    private SystemUser releasedBy;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Constructors and basic methods
    public VirusCulturePackaging() {
        super();
    }

    public VirusCulturePackaging(VirusCultureBatch cultureBatch, String finalBatchId, String vialType) {
        this();
        this.cultureBatch = cultureBatch;
        this.finalBatchId = finalBatchId;
        this.vialType = vialType;
        this.packagingDate = new Timestamp(System.currentTimeMillis());
        this.createdDate = new Timestamp(System.currentTimeMillis());
    }

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = new Timestamp(System.currentTimeMillis());
        }
        if (packagingDate == null) {
            packagingDate = new Timestamp(System.currentTimeMillis());
        }
    }

    @Override
    public Integer getId() { return id; }
    @Override
    public void setId(Integer id) { this.id = id; }

    // Key getters and setters
    public VirusCultureBatch getCultureBatch() { return cultureBatch; }
    public void setCultureBatch(VirusCultureBatch cultureBatch) { this.cultureBatch = cultureBatch; }

    public String getFinalBatchId() { return finalBatchId; }
    public void setFinalBatchId(String finalBatchId) { this.finalBatchId = finalBatchId; }

    public String getVialType() { return vialType; }
    public void setVialType(String vialType) { this.vialType = vialType; }

    public Integer getVialCount() { return vialCount; }
    public void setVialCount(Integer vialCount) { this.vialCount = vialCount; }

    public BigDecimal getFillVolumeMl() { return fillVolumeMl; }
    public void setFillVolumeMl(BigDecimal fillVolumeMl) { this.fillVolumeMl = fillVolumeMl; }

    public Date getExpiryDate() { return expiryDate; }
    public void setExpiryDate(Date expiryDate) { this.expiryDate = expiryDate; }

    public Timestamp getPackagingDate() { return packagingDate; }
    public void setPackagingDate(Timestamp packagingDate) { this.packagingDate = packagingDate; }

    public SystemUser getPackagedBy() { return packagedBy; }
    public void setPackagedBy(SystemUser packagedBy) { this.packagedBy = packagedBy; }

    public SystemUser getReleasedBy() { return releasedBy; }
    public void setReleasedBy(SystemUser releasedBy) { this.releasedBy = releasedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public boolean isReleased() { return releasedBy != null; }
    public boolean isExpired() { return expiryDate != null && expiryDate.before(new Date(System.currentTimeMillis())); }
}