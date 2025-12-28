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
 * Process Step 7: Formulation
 * Document formulation details (stabilizers, preservatives, concentrations)
 */
@Entity
@Table(name = "virus_culture_formulation")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class VirusCultureFormulation extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "virus_culture_formulation_seq")
    @SequenceGenerator(name = "virus_culture_formulation_seq", sequenceName = "virus_culture_formulation_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "culture_batch_id", nullable = false)
    @JsonIgnore
    private VirusCultureBatch cultureBatch;

    @NotNull
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "formulation_type", nullable = false, length = 100)
    private String formulationType;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "base_buffer")
    private String baseBuffer;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "stabilizer_name")
    private String stabilizerName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "stabilizer_concentration", length = 100)
    private String stabilizerConcentration;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "preservative_name")
    private String preservativeName;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "preservative_concentration", length = 100)
    private String preservativeConcentration;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "cryoprotectant")
    private String cryoprotectant;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "cryoprotectant_concentration", length = 100)
    private String cryoprotectantConcentration;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "final_concentration", length = 100)
    private String finalConcentration;

    @Column(name = "final_volume_ml", precision = 10, scale = 2)
    private BigDecimal finalVolumeMl;

    @Column(name = "ph_target", precision = 4, scale = 2)
    private BigDecimal phTarget;

    @Column(name = "ph_actual", precision = 4, scale = 2)
    private BigDecimal phActual;

    @Column(name = "osmolality_mosm_kg")
    private Integer osmolalityMosmKg;

    @Column(name = "protein_concentration_mg_ml", precision = 8, scale = 3)
    private BigDecimal proteinConcentrationMgMl;

    @Column(name = "virus_recovery_percentage", precision = 5, scale = 2)
    private BigDecimal virusRecoveryPercentage;

    @Column(name = "storage_temperature_celsius", precision = 6, scale = 2)
    private BigDecimal storageTemperatureCelsius;

    @Column(name = "expected_stability_days")
    private Integer expectedStabilityDays;

    @Column(name = "formulation_date")
    private Timestamp formulationDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "formulated_by")
    @JsonIgnore
    private SystemUser formulatedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qc_tested_by")
    @JsonIgnore
    private SystemUser qcTestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    @JsonIgnore
    private SystemUser approvedBy;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @SafeHtml(level = SafeHtml.SafeListLevel.RELAXED)
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    // Constructors and basic methods
    public VirusCultureFormulation() {
        super();
    }

    public VirusCultureFormulation(VirusCultureBatch cultureBatch, String formulationType) {
        this();
        this.cultureBatch = cultureBatch;
        this.formulationType = formulationType;
        this.formulationDate = new Timestamp(System.currentTimeMillis());
        this.createdDate = new Timestamp(System.currentTimeMillis());
    }

    @PrePersist
    protected void onCreate() {
        if (createdDate == null) {
            createdDate = new Timestamp(System.currentTimeMillis());
        }
        if (formulationDate == null) {
            formulationDate = new Timestamp(System.currentTimeMillis());
        }
    }

    @Override
    public Integer getId() { return id; }
    @Override
    public void setId(Integer id) { this.id = id; }

    // Getters and setters (abbreviated for brevity)
    public VirusCultureBatch getCultureBatch() { return cultureBatch; }
    public void setCultureBatch(VirusCultureBatch cultureBatch) { this.cultureBatch = cultureBatch; }

    public String getFormulationType() { return formulationType; }
    public void setFormulationType(String formulationType) { this.formulationType = formulationType; }

    public String getStabilizerName() { return stabilizerName; }
    public void setStabilizerName(String stabilizerName) { this.stabilizerName = stabilizerName; }

    public String getPreservativeName() { return preservativeName; }
    public void setPreservativeName(String preservativeName) { this.preservativeName = preservativeName; }

    public BigDecimal getFinalVolumeMl() { return finalVolumeMl; }
    public void setFinalVolumeMl(BigDecimal finalVolumeMl) { this.finalVolumeMl = finalVolumeMl; }

    public Timestamp getFormulationDate() { return formulationDate; }
    public void setFormulationDate(Timestamp formulationDate) { this.formulationDate = formulationDate; }

    public SystemUser getFormulatedBy() { return formulatedBy; }
    public void setFormulatedBy(SystemUser formulatedBy) { this.formulatedBy = formulatedBy; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}