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
 * Process Step 5: Virus Culture
 * Record virus strain, culture conditions (temp, CO₂, duration)
 */
@Entity
@Table(name = "virus_culture_virus_inoculation")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class VirusCultureVirusInoculation extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "virus_culture_virus_inoculation_seq")
    @SequenceGenerator(name = "virus_culture_virus_inoculation_seq", sequenceName = "virus_culture_virus_inoculation_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "culture_batch_id", nullable = false)
    @JsonIgnore
    private VirusCultureBatch cultureBatch;

    @NotNull
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "virus_strain", nullable = false)
    private String virusStrain;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "virus_stock_id", length = 100)
    private String virusStockId;

    @Column(name = "multiplicity_of_infection", precision = 8, scale = 4)
    private BigDecimal multiplicityOfInfection;

    @Column(name = "inoculum_volume_ml", precision = 10, scale = 3)
    private BigDecimal inoculumVolumeMl;

    @Column(name = "absorption_time_minutes")
    private Integer absorptionTimeMinutes;

    @Column(name = "infection_temperature_celsius", precision = 5, scale = 2)
    private BigDecimal infectionTemperatureCelsius;

    @Column(name = "co2_percentage", precision = 5, scale = 2)
    private BigDecimal co2Percentage;

    @Column(name = "infection_start_time")
    private Timestamp infectionStartTime;

    @Column(name = "infection_end_time")
    private Timestamp infectionEndTime;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @Column(name = "expected_cpe_time_hours")
    private Integer expectedCpeTimeHours;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "viral_titer_tcid50_ml", length = 50)
    private String viralTiterTcid50Ml;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "viral_titer_pfu_ml", length = 50)
    private String viralTiterPfuMl;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "harvest_criteria", columnDefinition = "TEXT")
    private String harvestCriteria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    @JsonIgnore
    private SystemUser performedBy;

    @Column(name = "inoculation_date")
    private Timestamp inoculationDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Constructors
    public VirusCultureVirusInoculation() {
        super();
    }

    public VirusCultureVirusInoculation(VirusCultureBatch cultureBatch, String virusStrain) {
        this();
        this.cultureBatch = cultureBatch;
        this.virusStrain = virusStrain;
        this.inoculationDate = new Timestamp(System.currentTimeMillis());
        this.infectionStartTime = new Timestamp(System.currentTimeMillis());
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (inoculationDate == null) {
            inoculationDate = new Timestamp(System.currentTimeMillis());
        }
        if (infectionStartTime == null) {
            infectionStartTime = new Timestamp(System.currentTimeMillis());
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Calculate duration if infection is completed
        if (infectionStartTime != null && infectionEndTime != null) {
            long durationMs = infectionEndTime.getTime() - infectionStartTime.getTime();
            durationHours = (int) (durationMs / (1000 * 60 * 60));
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

    public String getVirusStrain() {
        return virusStrain;
    }

    public void setVirusStrain(String virusStrain) {
        this.virusStrain = virusStrain;
    }

    public String getVirusStockId() {
        return virusStockId;
    }

    public void setVirusStockId(String virusStockId) {
        this.virusStockId = virusStockId;
    }

    public BigDecimal getMultiplicityOfInfection() {
        return multiplicityOfInfection;
    }

    public void setMultiplicityOfInfection(BigDecimal multiplicityOfInfection) {
        this.multiplicityOfInfection = multiplicityOfInfection;
    }

    public BigDecimal getInoculumVolumeMl() {
        return inoculumVolumeMl;
    }

    public void setInoculumVolumeMl(BigDecimal inoculumVolumeMl) {
        this.inoculumVolumeMl = inoculumVolumeMl;
    }

    public Integer getAbsorptionTimeMinutes() {
        return absorptionTimeMinutes;
    }

    public void setAbsorptionTimeMinutes(Integer absorptionTimeMinutes) {
        this.absorptionTimeMinutes = absorptionTimeMinutes;
    }

    public BigDecimal getInfectionTemperatureCelsius() {
        return infectionTemperatureCelsius;
    }

    public void setInfectionTemperatureCelsius(BigDecimal infectionTemperatureCelsius) {
        this.infectionTemperatureCelsius = infectionTemperatureCelsius;
    }

    public BigDecimal getCo2Percentage() {
        return co2Percentage;
    }

    public void setCo2Percentage(BigDecimal co2Percentage) {
        this.co2Percentage = co2Percentage;
    }

    public Timestamp getInfectionStartTime() {
        return infectionStartTime;
    }

    public void setInfectionStartTime(Timestamp infectionStartTime) {
        this.infectionStartTime = infectionStartTime;
    }

    public Timestamp getInfectionEndTime() {
        return infectionEndTime;
    }

    public void setInfectionEndTime(Timestamp infectionEndTime) {
        this.infectionEndTime = infectionEndTime;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public Integer getExpectedCpeTimeHours() {
        return expectedCpeTimeHours;
    }

    public void setExpectedCpeTimeHours(Integer expectedCpeTimeHours) {
        this.expectedCpeTimeHours = expectedCpeTimeHours;
    }

    public String getViralTiterTcid50Ml() {
        return viralTiterTcid50Ml;
    }

    public void setViralTiterTcid50Ml(String viralTiterTcid50Ml) {
        this.viralTiterTcid50Ml = viralTiterTcid50Ml;
    }

    public String getViralTiterPfuMl() {
        return viralTiterPfuMl;
    }

    public void setViralTiterPfuMl(String viralTiterPfuMl) {
        this.viralTiterPfuMl = viralTiterPfuMl;
    }

    public String getHarvestCriteria() {
        return harvestCriteria;
    }

    public void setHarvestCriteria(String harvestCriteria) {
        this.harvestCriteria = harvestCriteria;
    }

    public SystemUser getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(SystemUser performedBy) {
        this.performedBy = performedBy;
    }

    public Timestamp getInoculationDate() {
        return inoculationDate;
    }

    public void setInoculationDate(Timestamp inoculationDate) {
        this.inoculationDate = inoculationDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Utility methods
    public boolean isInfectionComplete() {
        return infectionEndTime != null;
    }

    public boolean isInfectionInProgress() {
        return infectionStartTime != null && infectionEndTime == null;
    }

    public boolean hasExpectedCpeTimeElapsed() {
        if (expectedCpeTimeHours == null || infectionStartTime == null) {
            return false;
        }

        long currentTimeMs = System.currentTimeMillis();
        long infectionDurationMs = currentTimeMs - infectionStartTime.getTime();
        int currentDurationHours = (int) (infectionDurationMs / (1000 * 60 * 60));

        return currentDurationHours >= expectedCpeTimeHours;
    }

    public void completeInfection() {
        if (infectionEndTime == null) {
            infectionEndTime = new Timestamp(System.currentTimeMillis());
        }
    }

    @Override
    public String toString() {
        return "VirusCultureVirusInoculation{" +
                "id=" + id +
                ", virusStrain='" + virusStrain + '\'' +
                ", virusStockId='" + virusStockId + '\'' +
                ", multiplicityOfInfection=" + multiplicityOfInfection +
                ", infectionTemperatureCelsius=" + infectionTemperatureCelsius +
                ", co2Percentage=" + co2Percentage +
                ", durationHours=" + durationHours +
                '}';
    }
}