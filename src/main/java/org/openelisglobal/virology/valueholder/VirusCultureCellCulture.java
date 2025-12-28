package org.openelisglobal.virology.valueholder;

import java.math.BigDecimal;
import java.sql.Timestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.validation.annotations.SafeHtml;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Process Step 3: Cell Culture
 * Track cell line, passage number, growth conditions
 */
@Entity
@Table(name = "virus_culture_cell_culture")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class VirusCultureCellCulture extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "virus_culture_cell_culture_seq")
    @SequenceGenerator(name = "virus_culture_cell_culture_seq", sequenceName = "virus_culture_cell_culture_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "culture_batch_id", nullable = false)
    @JsonIgnore
    private VirusCultureBatch cultureBatch;

    @NotNull
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "cell_line", nullable = false, length = 100)
    private String cellLine;

    @NotNull
    @Column(name = "passage_number", nullable = false)
    private Integer passageNumber;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "seeding_density", length = 50)
    private String seedingDensity;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "flask_type", length = 50)
    private String flaskType;

    @Column(name = "media_volume_ml", precision = 10, scale = 2)
    private BigDecimal mediaVolumeMl;

    @Column(name = "incubation_temperature_celsius", precision = 5, scale = 2)
    private BigDecimal incubationTemperatureCelsius;

    @Column(name = "co2_percentage", precision = 5, scale = 2)
    private BigDecimal co2Percentage;

    @Column(name = "humidity_percentage", precision = 5, scale = 2)
    private BigDecimal humidityPercentage;

    @Column(name = "confluence_percentage")
    private Integer confluencePercentage;

    @Column(name = "cell_count")
    private Long cellCount;

    @Column(name = "doubling_time_hours", precision = 6, scale = 2)
    private BigDecimal doublingTimeHours;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "morphology_observation", columnDefinition = "TEXT")
    private String morphologyObservation;

    @Column(name = "seeding_date")
    private Timestamp seedingDate;

    @Column(name = "harvest_date")
    private Timestamp harvestDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    @JsonIgnore
    private SystemUser technician;

    @Column(name = "created_date")
    private Timestamp createdDate;

    public VirusCultureCellCulture() {
        super();
    }

    public VirusCultureCellCulture(VirusCultureBatch cultureBatch, String cellLine, Integer passageNumber) {
        this();
        this.cultureBatch = cultureBatch;
        this.cellLine = cellLine;
        this.passageNumber = passageNumber;
        this.createdDate = new Timestamp(System.currentTimeMillis());
        this.seedingDate = new Timestamp(System.currentTimeMillis());
    }

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = new Timestamp(System.currentTimeMillis());
        }
        if (seedingDate == null) {
            seedingDate = new Timestamp(System.currentTimeMillis());
        }
    }

    @Override
    public Integer getId() { return id; }
    @Override
    public void setId(Integer id) { this.id = id; }

    public VirusCultureBatch getCultureBatch() { return cultureBatch; }
    public void setCultureBatch(VirusCultureBatch cultureBatch) { this.cultureBatch = cultureBatch; }

    public String getCellLine() { return cellLine; }
    public void setCellLine(String cellLine) { this.cellLine = cellLine; }

    public Integer getPassageNumber() { return passageNumber; }
    public void setPassageNumber(Integer passageNumber) { this.passageNumber = passageNumber; }

    public String getSeedingDensity() { return seedingDensity; }
    public void setSeedingDensity(String seedingDensity) { this.seedingDensity = seedingDensity; }

    public String getFlaskType() { return flaskType; }
    public void setFlaskType(String flaskType) { this.flaskType = flaskType; }

    public BigDecimal getMediaVolumeMl() { return mediaVolumeMl; }
    public void setMediaVolumeMl(BigDecimal mediaVolumeMl) { this.mediaVolumeMl = mediaVolumeMl; }

    public BigDecimal getIncubationTemperatureCelsius() { return incubationTemperatureCelsius; }
    public void setIncubationTemperatureCelsius(BigDecimal incubationTemperatureCelsius) { this.incubationTemperatureCelsius = incubationTemperatureCelsius; }

    public BigDecimal getCo2Percentage() { return co2Percentage; }
    public void setCo2Percentage(BigDecimal co2Percentage) { this.co2Percentage = co2Percentage; }

    public BigDecimal getHumidityPercentage() { return humidityPercentage; }
    public void setHumidityPercentage(BigDecimal humidityPercentage) { this.humidityPercentage = humidityPercentage; }

    public Integer getConfluencePercentage() { return confluencePercentage; }
    public void setConfluencePercentage(Integer confluencePercentage) { this.confluencePercentage = confluencePercentage; }

    public Long getCellCount() { return cellCount; }
    public void setCellCount(Long cellCount) { this.cellCount = cellCount; }

    public BigDecimal getDoublingTimeHours() { return doublingTimeHours; }
    public void setDoublingTimeHours(BigDecimal doublingTimeHours) { this.doublingTimeHours = doublingTimeHours; }

    public String getMorphologyObservation() { return morphologyObservation; }
    public void setMorphologyObservation(String morphologyObservation) { this.morphologyObservation = morphologyObservation; }

    public Timestamp getSeedingDate() { return seedingDate; }
    public void setSeedingDate(Timestamp seedingDate) { this.seedingDate = seedingDate; }

    public Timestamp getHarvestDate() { return harvestDate; }
    public void setHarvestDate(Timestamp harvestDate) { this.harvestDate = harvestDate; }

    public SystemUser getTechnician() { return technician; }
    public void setTechnician(SystemUser technician) { this.technician = technician; }

    public Timestamp getCreatedDate() { return createdDate; }
    public void setCreatedDate(Timestamp createdDate) { this.createdDate = createdDate; }

    public boolean isReadyForHarvest() { return confluencePercentage != null && confluencePercentage >= 80; }
    public boolean isHarvested() { return harvestDate != null; }
}