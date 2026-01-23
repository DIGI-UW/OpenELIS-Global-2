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
 * Process Step 2: Sterilization Record sterilization parameters (temp, time,
 * pressure)
 */
@Entity
@Table(name = "virus_culture_sterilization")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class VirusCultureSterilization extends BaseObject<Integer> {

    public enum SterilizationMethod {
        AUTOCLAVING, FILTRATION, DRY_HEAT, GAMMA_IRRADIATION, UV_STERILIZATION
    }

    public enum ValidationResult {
        PENDING, PASSED, FAILED, REQUIRES_RETEST
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "virus_culture_sterilization_seq")
    @SequenceGenerator(name = "virus_culture_sterilization_seq", sequenceName = "virus_culture_sterilization_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "culture_batch_id", nullable = false)
    @JsonIgnore
    private VirusCultureBatch cultureBatch;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sterilization_method", nullable = false, length = 50)
    private SterilizationMethod sterilizationMethod;

    @Column(name = "temperature_celsius", precision = 5, scale = 2)
    private BigDecimal temperatureCelsius;

    @Column(name = "time_minutes")
    private Integer timeMinutes;

    @Column(name = "pressure_psi", precision = 6, scale = 2)
    private BigDecimal pressurePsi;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "equipment_id", length = 100)
    private String equipmentId;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "cycle_number", length = 50)
    private String cycleNumber;

    @Column(name = "start_time")
    private Timestamp startTime;

    @Column(name = "end_time")
    private Timestamp endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_result")
    private ValidationResult validationResult = ValidationResult.PENDING;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "spore_test_result", length = 20)
    private String sporeTestResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by")
    @JsonIgnore
    private SystemUser performedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validated_by")
    @JsonIgnore
    private SystemUser validatedBy;

    @Column(name = "sterilization_date")
    private Timestamp sterilizationDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    public VirusCultureSterilization() {
        super();
    }

    public VirusCultureSterilization(VirusCultureBatch cultureBatch, SterilizationMethod sterilizationMethod) {
        this();
        this.cultureBatch = cultureBatch;
        this.sterilizationMethod = sterilizationMethod;
        this.sterilizationDate = new Timestamp(System.currentTimeMillis());
    }

    @PrePersist
    protected void onCreate() {
        if (sterilizationDate == null) {
            sterilizationDate = new Timestamp(System.currentTimeMillis());
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

    public SterilizationMethod getSterilizationMethod() {
        return sterilizationMethod;
    }

    public void setSterilizationMethod(SterilizationMethod sterilizationMethod) {
        this.sterilizationMethod = sterilizationMethod;
    }

    public BigDecimal getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(BigDecimal temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public Integer getTimeMinutes() {
        return timeMinutes;
    }

    public void setTimeMinutes(Integer timeMinutes) {
        this.timeMinutes = timeMinutes;
    }

    public BigDecimal getPressurePsi() {
        return pressurePsi;
    }

    public void setPressurePsi(BigDecimal pressurePsi) {
        this.pressurePsi = pressurePsi;
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public void setEquipmentId(String equipmentId) {
        this.equipmentId = equipmentId;
    }

    public String getCycleNumber() {
        return cycleNumber;
    }

    public void setCycleNumber(String cycleNumber) {
        this.cycleNumber = cycleNumber;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(ValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    public String getSporeTestResult() {
        return sporeTestResult;
    }

    public void setSporeTestResult(String sporeTestResult) {
        this.sporeTestResult = sporeTestResult;
    }

    public SystemUser getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(SystemUser performedBy) {
        this.performedBy = performedBy;
    }

    public SystemUser getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(SystemUser validatedBy) {
        this.validatedBy = validatedBy;
    }

    public Timestamp getSterilizationDate() {
        return sterilizationDate;
    }

    public void setSterilizationDate(Timestamp sterilizationDate) {
        this.sterilizationDate = sterilizationDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isValidated() {
        return validationResult == ValidationResult.PASSED;
    }

    public boolean hasFailed() {
        return validationResult == ValidationResult.FAILED;
    }
}