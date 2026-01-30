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
import jakarta.persistence.UniqueConstraint;
import java.sql.Date;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.tb.valueholder.TbEnums.CultureMethod;
import org.openelisglobal.tb.valueholder.TbEnums.CultureResult;
import org.openelisglobal.tb.valueholder.TbEnums.GrowthObservation;

/**
 * Weekly culture reading for TB samples. Tracks growth observations for up to 8
 * weeks of culture incubation.
 */
@Entity
@Table(name = "tb_culture_reading", uniqueConstraints = @UniqueConstraint(columnNames = { "sample_item_id",
        "week_number" }))
public class TbCultureReading extends BaseObject<Integer> {

    /** Maximum number of weeks for culture incubation */
    public static final int MAX_WEEKS = 8;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tb_culture_reading_generator")
    @SequenceGenerator(name = "tb_culture_reading_generator", sequenceName = "tb_culture_reading_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", nullable = false)
    private SampleItem sampleItem;

    @Column(name = "week_number", nullable = false)
    private Integer weekNumber;

    @Column(name = "reading_date", nullable = false)
    private Timestamp readingDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "culture_method", nullable = false, length = 20)
    private CultureMethod cultureMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "growth_observation", nullable = false, length = 50)
    private GrowthObservation growthObservation;

    // For BOTH method, track individual media observations
    @Enumerated(EnumType.STRING)
    @Column(name = "lj_observation", length = 50)
    private GrowthObservation ljObservation;

    @Enumerated(EnumType.STRING)
    @Column(name = "mgit_observation", length = 50)
    private GrowthObservation mgitObservation;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "read_by")
    private SystemUser readBy;

    @Column(name = "is_delayed", nullable = false)
    private Boolean isDelayed = false;

    // ====== Stage 4: Inoculation & Final Result Fields ======

    @Column(name = "inoculation_date")
    private Date inoculationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_batch_id")
    private TbMediaPreparation mediaBatch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_processing_id")
    private TbSampleProcessing sampleProcessing;

    @Enumerated(EnumType.STRING)
    @Column(name = "culture_result", length = 20)
    private CultureResult cultureResult;

    @Column(name = "positive_week")
    private Integer positiveWeek;

    @Column(name = "final_result_date")
    private Date finalResultDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inoculated_by")
    private SystemUser inoculatedBy;

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

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        if (weekNumber != null && (weekNumber < 1 || weekNumber > MAX_WEEKS)) {
            throw new IllegalArgumentException("Week number must be between 1 and " + MAX_WEEKS);
        }
        this.weekNumber = weekNumber;
    }

    public Timestamp getReadingDate() {
        return readingDate;
    }

    public void setReadingDate(Timestamp readingDate) {
        this.readingDate = readingDate;
    }

    public CultureMethod getCultureMethod() {
        return cultureMethod;
    }

    public void setCultureMethod(CultureMethod cultureMethod) {
        this.cultureMethod = cultureMethod;
    }

    public GrowthObservation getGrowthObservation() {
        return growthObservation;
    }

    public void setGrowthObservation(GrowthObservation growthObservation) {
        this.growthObservation = growthObservation;
    }

    public GrowthObservation getLjObservation() {
        return ljObservation;
    }

    public void setLjObservation(GrowthObservation ljObservation) {
        this.ljObservation = ljObservation;
    }

    public GrowthObservation getMgitObservation() {
        return mgitObservation;
    }

    public void setMgitObservation(GrowthObservation mgitObservation) {
        this.mgitObservation = mgitObservation;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public SystemUser getReadBy() {
        return readBy;
    }

    public void setReadBy(SystemUser readBy) {
        this.readBy = readBy;
    }

    public Boolean getIsDelayed() {
        return isDelayed;
    }

    public void setIsDelayed(Boolean isDelayed) {
        this.isDelayed = isDelayed != null ? isDelayed : false;
    }

    /**
     * Returns true if this reading shows growth detected.
     */
    public boolean isGrowthDetected() {
        return growthObservation == GrowthObservation.GROWTH_DETECTED;
    }

    /**
     * Returns true if this reading indicates contamination.
     */
    public boolean isContaminated() {
        return growthObservation == GrowthObservation.CONTAMINATED;
    }

    /**
     * Returns true if this is the final week (Week 8) reading with no growth.
     */
    public boolean isFinalNegative() {
        return weekNumber != null && weekNumber == MAX_WEEKS && growthObservation == GrowthObservation.NO_GROWTH;
    }

    // ====== Stage 4: New Field Getters and Setters ======

    public Date getInoculationDate() {
        return inoculationDate;
    }

    public void setInoculationDate(Date inoculationDate) {
        this.inoculationDate = inoculationDate;
    }

    public TbMediaPreparation getMediaBatch() {
        return mediaBatch;
    }

    public void setMediaBatch(TbMediaPreparation mediaBatch) {
        this.mediaBatch = mediaBatch;
    }

    public TbSampleProcessing getSampleProcessing() {
        return sampleProcessing;
    }

    public void setSampleProcessing(TbSampleProcessing sampleProcessing) {
        this.sampleProcessing = sampleProcessing;
    }

    public CultureResult getCultureResult() {
        return cultureResult;
    }

    public void setCultureResult(CultureResult cultureResult) {
        this.cultureResult = cultureResult;
    }

    public Integer getPositiveWeek() {
        return positiveWeek;
    }

    public void setPositiveWeek(Integer positiveWeek) {
        if (positiveWeek != null && (positiveWeek < 1 || positiveWeek > MAX_WEEKS)) {
            throw new IllegalArgumentException("Positive week must be between 1 and " + MAX_WEEKS);
        }
        this.positiveWeek = positiveWeek;
    }

    public Date getFinalResultDate() {
        return finalResultDate;
    }

    public void setFinalResultDate(Date finalResultDate) {
        this.finalResultDate = finalResultDate;
    }

    public SystemUser getInoculatedBy() {
        return inoculatedBy;
    }

    public void setInoculatedBy(SystemUser inoculatedBy) {
        this.inoculatedBy = inoculatedBy;
    }

    /**
     * Returns true if the sample has been inoculated to media.
     */
    public boolean isInoculated() {
        return inoculationDate != null && mediaBatch != null;
    }

    /**
     * Returns true if a final culture result has been determined.
     */
    public boolean hasFinalResult() {
        return cultureResult != null;
    }

    /**
     * Marks this culture as positive at the current week.
     */
    public void markPositive(int week) {
        this.cultureResult = CultureResult.POSITIVE;
        this.positiveWeek = week;
        this.finalResultDate = new Date(System.currentTimeMillis());
    }

    /**
     * Marks this culture as negative (no growth after 8 weeks).
     */
    public void markNegative() {
        this.cultureResult = CultureResult.NEGATIVE;
        this.positiveWeek = null;
        this.finalResultDate = new Date(System.currentTimeMillis());
    }

    /**
     * Marks this culture as contaminated.
     */
    public void markContaminated() {
        this.cultureResult = CultureResult.CONTAMINATED;
        this.positiveWeek = null;
        this.finalResultDate = new Date(System.currentTimeMillis());
    }
}
