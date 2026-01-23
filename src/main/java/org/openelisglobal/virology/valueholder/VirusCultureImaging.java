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
 * Process Step 6: Dark Room Imaging Store image data, analysis results (CPE
 * observation, fluorescence intensity)
 */
@Entity
@Table(name = "virus_culture_imaging")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class VirusCultureImaging extends BaseObject<Integer> {

    public enum ImagingType {
        BRIGHTFIELD, FLUORESCENCE, PHASE_CONTRAST, CONFOCAL, TIME_LAPSE, LIVE_DEAD_STAINING
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "virus_culture_imaging_seq")
    @SequenceGenerator(name = "virus_culture_imaging_seq", sequenceName = "virus_culture_imaging_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "culture_batch_id", nullable = false)
    @JsonIgnore
    private VirusCultureBatch cultureBatch;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "imaging_type", nullable = false, length = 50)
    private ImagingType imagingType;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "image_file_path", length = 500)
    private String imageFilePath;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "image_file_name")
    private String imageFileName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "magnification", length = 20)
    private String magnification;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "microscope_type", length = 100)
    private String microscopeType;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "filter_set", length = 100)
    private String filterSet;

    @Column(name = "exposure_time_ms")
    private Integer exposureTimeMs;

    @Column(name = "cpe_score")
    private Integer cpeScore; // Scale 0-4 (0=no CPE, 4=complete CPE)

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "cpe_description", columnDefinition = "TEXT")
    private String cpeDescription;

    @Column(name = "fluorescence_intensity", precision = 10, scale = 2)
    private BigDecimal fluorescenceIntensity;

    @Column(name = "fluorescence_area_percentage", precision = 5, scale = 2)
    private BigDecimal fluorescenceAreaPercentage;

    @Column(name = "cell_count_total")
    private Integer cellCountTotal;

    @Column(name = "cell_count_infected")
    private Integer cellCountInfected;

    @Column(name = "infection_percentage", precision = 5, scale = 2)
    private BigDecimal infectionPercentage;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "analysis_software", length = 100)
    private String analysisSoftware;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "analysis_parameters", columnDefinition = "TEXT")
    private String analysisParameters;

    @Column(name = "quality_score")
    private Integer qualityScore; // Scale 1-5 (1=poor, 5=excellent)

    @Column(name = "imaging_date")
    private Timestamp imagingDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "imaged_by")
    @JsonIgnore
    private SystemUser imagedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analyzed_by")
    @JsonIgnore
    private SystemUser analyzedBy;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Constructors
    public VirusCultureImaging() {
        super();
    }

    public VirusCultureImaging(VirusCultureBatch cultureBatch, ImagingType imagingType) {
        this();
        this.cultureBatch = cultureBatch;
        this.imagingType = imagingType;
        this.createdDate = new Timestamp(System.currentTimeMillis());
        this.imagingDate = new Timestamp(System.currentTimeMillis());
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = new Timestamp(System.currentTimeMillis());
        }
        if (imagingDate == null) {
            imagingDate = new Timestamp(System.currentTimeMillis());
        }
    }

    @PostLoad
    @PostUpdate
    protected void calculateInfectionPercentage() {
        if (cellCountTotal != null && cellCountTotal > 0 && cellCountInfected != null) {
            infectionPercentage = BigDecimal.valueOf(cellCountInfected).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(cellCountTotal), 2, BigDecimal.ROUND_HALF_UP);
        }
    }

    // Getters and setters
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

    @Transient
    public Integer getCultureBatchId() {
        return cultureBatch != null ? cultureBatch.getId() : null;
    }

    public ImagingType getImagingType() {
        return imagingType;
    }

    public void setImagingType(ImagingType imagingType) {
        this.imagingType = imagingType;
    }

    public String getImageFilePath() {
        return imageFilePath;
    }

    public void setImageFilePath(String imageFilePath) {
        this.imageFilePath = imageFilePath;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public String getMagnification() {
        return magnification;
    }

    public void setMagnification(String magnification) {
        this.magnification = magnification;
    }

    public String getMicroscopeType() {
        return microscopeType;
    }

    public void setMicroscopeType(String microscopeType) {
        this.microscopeType = microscopeType;
    }

    public String getFilterSet() {
        return filterSet;
    }

    public void setFilterSet(String filterSet) {
        this.filterSet = filterSet;
    }

    public Integer getExposureTimeMs() {
        return exposureTimeMs;
    }

    public void setExposureTimeMs(Integer exposureTimeMs) {
        this.exposureTimeMs = exposureTimeMs;
    }

    public Integer getCpeScore() {
        return cpeScore;
    }

    public void setCpeScore(Integer cpeScore) {
        this.cpeScore = cpeScore;
    }

    public String getCpeDescription() {
        return cpeDescription;
    }

    public void setCpeDescription(String cpeDescription) {
        this.cpeDescription = cpeDescription;
    }

    public BigDecimal getFluorescenceIntensity() {
        return fluorescenceIntensity;
    }

    public void setFluorescenceIntensity(BigDecimal fluorescenceIntensity) {
        this.fluorescenceIntensity = fluorescenceIntensity;
    }

    public BigDecimal getFluorescenceAreaPercentage() {
        return fluorescenceAreaPercentage;
    }

    public void setFluorescenceAreaPercentage(BigDecimal fluorescenceAreaPercentage) {
        this.fluorescenceAreaPercentage = fluorescenceAreaPercentage;
    }

    public Integer getCellCountTotal() {
        return cellCountTotal;
    }

    public void setCellCountTotal(Integer cellCountTotal) {
        this.cellCountTotal = cellCountTotal;
    }

    public Integer getCellCountInfected() {
        return cellCountInfected;
    }

    public void setCellCountInfected(Integer cellCountInfected) {
        this.cellCountInfected = cellCountInfected;
    }

    public BigDecimal getInfectionPercentage() {
        return infectionPercentage;
    }

    public void setInfectionPercentage(BigDecimal infectionPercentage) {
        this.infectionPercentage = infectionPercentage;
    }

    public String getAnalysisSoftware() {
        return analysisSoftware;
    }

    public void setAnalysisSoftware(String analysisSoftware) {
        this.analysisSoftware = analysisSoftware;
    }

    public String getAnalysisParameters() {
        return analysisParameters;
    }

    public void setAnalysisParameters(String analysisParameters) {
        this.analysisParameters = analysisParameters;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public Timestamp getImagingDate() {
        return imagingDate;
    }

    public void setImagingDate(Timestamp imagingDate) {
        this.imagingDate = imagingDate;
    }

    public SystemUser getImagedBy() {
        return imagedBy;
    }

    public void setImagedBy(SystemUser imagedBy) {
        this.imagedBy = imagedBy;
    }

    public SystemUser getAnalyzedBy() {
        return analyzedBy;
    }

    public void setAnalyzedBy(SystemUser analyzedBy) {
        this.analyzedBy = analyzedBy;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Utility methods
    public boolean hasCpe() {
        return cpeScore != null && cpeScore > 0;
    }

    public boolean hasSignificantCpe() {
        return cpeScore != null && cpeScore >= 3;
    }

    public boolean hasGoodQuality() {
        return qualityScore != null && qualityScore >= 3;
    }

    public boolean hasHighInfectionRate() {
        return infectionPercentage != null && infectionPercentage.compareTo(BigDecimal.valueOf(80)) >= 0;
    }

    public String getCpeLevel() {
        if (cpeScore == null)
            return "Not Assessed";
        switch (cpeScore) {
        case 0:
            return "No CPE";
        case 1:
            return "Minimal CPE (<25%)";
        case 2:
            return "Moderate CPE (25-50%)";
        case 3:
            return "Significant CPE (50-75%)";
        case 4:
            return "Complete CPE (>75%)";
        default:
            return "Invalid Score";
        }
    }

    @Override
    public String toString() {
        return "VirusCultureImaging{" + "id=" + id + ", imagingType=" + imagingType + ", cpeScore=" + cpeScore
                + ", infectionPercentage=" + infectionPercentage + ", qualityScore=" + qualityScore + '}';
    }
}