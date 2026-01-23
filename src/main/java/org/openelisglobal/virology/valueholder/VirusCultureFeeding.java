package org.openelisglobal.virology.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Process Step 8: Feeding Log feeding schedule, reagents used
 */
@Entity
@Table(name = "virus_culture_feeding")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class VirusCultureFeeding extends BaseObject<Integer> {

    public enum FeedingType {
        FULL_MEDIA_CHANGE, PARTIAL_MEDIA_CHANGE, SUPPLEMENT_ADDITION, GLUCOSE_FEEDING, MAINTENANCE_FEEDING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "virus_culture_feeding_seq")
    @SequenceGenerator(name = "virus_culture_feeding_seq", sequenceName = "virus_culture_feeding_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "culture_batch_id", nullable = false)
    @JsonIgnore
    private VirusCultureBatch cultureBatch;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "feeding_type", nullable = false, length = 50)
    private FeedingType feedingType;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "media_type")
    private String mediaType;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "media_lot_number", length = 100)
    private String mediaLotNumber;

    @Column(name = "volume_added_ml", precision = 10, scale = 2)
    private BigDecimal volumeAddedMl;

    @Column(name = "volume_removed_ml", precision = 10, scale = 2)
    private BigDecimal volumeRemovedMl;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "supplements_added", columnDefinition = "TEXT")
    private String supplementsAdded;

    @Column(name = "glucose_concentration_mg_dl", precision = 8, scale = 2)
    private BigDecimal glucoseConcentrationMgDl;

    @Column(name = "ph_before_feeding", precision = 4, scale = 2)
    private BigDecimal phBeforeFeeding;

    @Column(name = "ph_after_feeding", precision = 4, scale = 2)
    private BigDecimal phAfterFeeding;

    @Column(name = "cell_viability_percentage", precision = 5, scale = 2)
    private BigDecimal cellViabilityPercentage;

    @Column(name = "cell_density_cells_ml")
    private Long cellDensityCellsMl;

    @Column(name = "lactate_level_mmol_l", precision = 8, scale = 2)
    private BigDecimal lactateLevelMmolL;

    @Column(name = "feeding_interval_hours")
    private Integer feedingIntervalHours;

    @Column(name = "next_feeding_due")
    private Timestamp nextFeedingDue;

    @Column(name = "feeding_date")
    private Timestamp feedingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    @JsonIgnore
    private SystemUser performedBy;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Constructors and basic methods
    public VirusCultureFeeding() {
        super();
    }

    public VirusCultureFeeding(VirusCultureBatch cultureBatch, FeedingType feedingType) {
        this();
        this.cultureBatch = cultureBatch;
        this.feedingType = feedingType;
        this.feedingDate = new Timestamp(System.currentTimeMillis());
        this.createdDate = new Timestamp(System.currentTimeMillis());
    }

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = new Timestamp(System.currentTimeMillis());
        }
        if (feedingDate == null) {
            feedingDate = new Timestamp(System.currentTimeMillis());
        }
        // Calculate next feeding due based on interval
        if (feedingIntervalHours != null && feedingDate != null) {
            long nextFeedingMs = feedingDate.getTime() + (feedingIntervalHours * 60 * 60 * 1000L);
            nextFeedingDue = new Timestamp(nextFeedingMs);
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

    // Key getters and setters
    public VirusCultureBatch getCultureBatch() {
        return cultureBatch;
    }

    public void setCultureBatch(VirusCultureBatch cultureBatch) {
        this.cultureBatch = cultureBatch;
    }

    public FeedingType getFeedingType() {
        return feedingType;
    }

    public void setFeedingType(FeedingType feedingType) {
        this.feedingType = feedingType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public BigDecimal getVolumeAddedMl() {
        return volumeAddedMl;
    }

    public void setVolumeAddedMl(BigDecimal volumeAddedMl) {
        this.volumeAddedMl = volumeAddedMl;
    }

    public Timestamp getFeedingDate() {
        return feedingDate;
    }

    public void setFeedingDate(Timestamp feedingDate) {
        this.feedingDate = feedingDate;
    }

    public Timestamp getNextFeedingDue() {
        return nextFeedingDue;
    }

    public void setNextFeedingDue(Timestamp nextFeedingDue) {
        this.nextFeedingDue = nextFeedingDue;
    }

    public SystemUser getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(SystemUser performedBy) {
        this.performedBy = performedBy;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isFeedingDue() {
        return nextFeedingDue != null && nextFeedingDue.before(new Timestamp(System.currentTimeMillis()));
    }
}