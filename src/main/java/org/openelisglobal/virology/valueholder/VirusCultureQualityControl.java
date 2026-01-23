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
 * Process Step 4: Quality Control Log QC results (viability %, sterility
 * pass/fail)
 */
@Entity
@Table(name = "virus_culture_quality_control")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class VirusCultureQualityControl extends BaseObject<Integer> {

    public enum QcType {
        VIABILITY_ASSAY, STERILITY_TEST, MYCOPLASMA_TEST, ENDOTOXIN_TEST, PH_MEASUREMENT, GLUCOSE_TEST, LACTATE_TEST,
        FULL_QC_PANEL
    }

    public enum SterilityResult {
        PASS, FAIL, PENDING, CONTAMINATED, INCONCLUSIVE
    }

    public enum ResultInterpretation {
        PENDING, ACCEPTABLE, OUT_OF_SPECIFICATION, MARGINAL, FAILED, REQUIRES_RETEST
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "virus_culture_qc_seq")
    @SequenceGenerator(name = "virus_culture_qc_seq", sequenceName = "virus_culture_qc_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "culture_batch_id", nullable = false)
    @JsonIgnore
    private VirusCultureBatch cultureBatch;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "qc_type", nullable = false, length = 50)
    private QcType qcType;

    @Column(name = "viability_percentage", precision = 5, scale = 2)
    private BigDecimal viabilityPercentage;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "sterility_result", nullable = false)
    private SterilityResult sterilityResult;

    @Enumerated(EnumType.STRING)
    @Column(name = "mycoplasma_test_result")
    private SterilityResult mycoplasmaTestResult;

    @Column(name = "endotoxin_level_eu_ml", precision = 10, scale = 4)
    private BigDecimal endotoxinLevelEuMl;

    @Column(name = "ph_measurement", precision = 4, scale = 2)
    private BigDecimal phMeasurement;

    @Column(name = "glucose_concentration_mg_dl", precision = 8, scale = 2)
    private BigDecimal glucoseConcentrationMgDl;

    @Column(name = "lactate_concentration_mmol_l", precision = 8, scale = 2)
    private BigDecimal lactateConcentrationMmolL;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "test_method", length = 100)
    private String testMethod;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "acceptance_criteria", columnDefinition = "TEXT")
    private String acceptanceCriteria;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_interpretation")
    private ResultInterpretation resultInterpretation = ResultInterpretation.PENDING;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "deviation_notes", columnDefinition = "TEXT")
    private String deviationNotes;

    @Column(name = "test_date")
    private Timestamp testDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tested_by")
    @JsonIgnore
    private SystemUser testedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    @JsonIgnore
    private SystemUser reviewedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    @JsonIgnore
    private SystemUser approvedBy;

    @Column(name = "created_date")
    private Timestamp createdDate;

    // Constructors
    public VirusCultureQualityControl() {
        super();
    }

    public VirusCultureQualityControl(VirusCultureBatch cultureBatch, QcType qcType) {
        this();
        this.cultureBatch = cultureBatch;
        this.qcType = qcType;
        this.createdDate = new Timestamp(System.currentTimeMillis());
        this.sterilityResult = SterilityResult.PENDING;
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = new Timestamp(System.currentTimeMillis());
        }
        if (testDate == null) {
            testDate = new Timestamp(System.currentTimeMillis());
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

    public QcType getQcType() {
        return qcType;
    }

    public void setQcType(QcType qcType) {
        this.qcType = qcType;
    }

    public BigDecimal getViabilityPercentage() {
        return viabilityPercentage;
    }

    public void setViabilityPercentage(BigDecimal viabilityPercentage) {
        this.viabilityPercentage = viabilityPercentage;
    }

    public SterilityResult getSterilityResult() {
        return sterilityResult;
    }

    public void setSterilityResult(SterilityResult sterilityResult) {
        this.sterilityResult = sterilityResult;
    }

    public SterilityResult getMycoplasmaTestResult() {
        return mycoplasmaTestResult;
    }

    public void setMycoplasmaTestResult(SterilityResult mycoplasmaTestResult) {
        this.mycoplasmaTestResult = mycoplasmaTestResult;
    }

    public BigDecimal getEndotoxinLevelEuMl() {
        return endotoxinLevelEuMl;
    }

    public void setEndotoxinLevelEuMl(BigDecimal endotoxinLevelEuMl) {
        this.endotoxinLevelEuMl = endotoxinLevelEuMl;
    }

    public BigDecimal getPhMeasurement() {
        return phMeasurement;
    }

    public void setPhMeasurement(BigDecimal phMeasurement) {
        this.phMeasurement = phMeasurement;
    }

    public BigDecimal getGlucoseConcentrationMgDl() {
        return glucoseConcentrationMgDl;
    }

    public void setGlucoseConcentrationMgDl(BigDecimal glucoseConcentrationMgDl) {
        this.glucoseConcentrationMgDl = glucoseConcentrationMgDl;
    }

    public BigDecimal getLactateConcentrationMmolL() {
        return lactateConcentrationMmolL;
    }

    public void setLactateConcentrationMmolL(BigDecimal lactateConcentrationMmolL) {
        this.lactateConcentrationMmolL = lactateConcentrationMmolL;
    }

    public String getTestMethod() {
        return testMethod;
    }

    public void setTestMethod(String testMethod) {
        this.testMethod = testMethod;
    }

    public String getAcceptanceCriteria() {
        return acceptanceCriteria;
    }

    public void setAcceptanceCriteria(String acceptanceCriteria) {
        this.acceptanceCriteria = acceptanceCriteria;
    }

    public ResultInterpretation getResultInterpretation() {
        return resultInterpretation;
    }

    public void setResultInterpretation(ResultInterpretation resultInterpretation) {
        this.resultInterpretation = resultInterpretation;
    }

    public String getDeviationNotes() {
        return deviationNotes;
    }

    public void setDeviationNotes(String deviationNotes) {
        this.deviationNotes = deviationNotes;
    }

    public Timestamp getTestDate() {
        return testDate;
    }

    public void setTestDate(Timestamp testDate) {
        this.testDate = testDate;
    }

    public SystemUser getTestedBy() {
        return testedBy;
    }

    public void setTestedBy(SystemUser testedBy) {
        this.testedBy = testedBy;
    }

    public SystemUser getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(SystemUser reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public SystemUser getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(SystemUser approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    // Utility methods
    public boolean isPassed() {
        return resultInterpretation == ResultInterpretation.ACCEPTABLE;
    }

    public boolean isFailed() {
        return resultInterpretation == ResultInterpretation.FAILED
                || resultInterpretation == ResultInterpretation.OUT_OF_SPECIFICATION;
    }

    public boolean isPending() {
        return resultInterpretation == ResultInterpretation.PENDING;
    }

    public boolean requiresRetest() {
        return resultInterpretation == ResultInterpretation.REQUIRES_RETEST;
    }

    public boolean isSterile() {
        return sterilityResult == SterilityResult.PASS
                && (mycoplasmaTestResult == null || mycoplasmaTestResult == SterilityResult.PASS);
    }

    public boolean isViable() {
        return viabilityPercentage != null && viabilityPercentage.compareTo(new BigDecimal("70")) >= 0;
    }

    public boolean hasAcceptableEndotoxinLevel() {
        return endotoxinLevelEuMl == null || endotoxinLevelEuMl.compareTo(new BigDecimal("5.0")) <= 0;
    }

    @Override
    public String toString() {
        return "VirusCultureQualityControl{" + "id=" + id + ", qcType=" + qcType + ", viabilityPercentage="
                + viabilityPercentage + ", sterilityResult=" + sterilityResult + ", resultInterpretation="
                + resultInterpretation + '}';
    }
}