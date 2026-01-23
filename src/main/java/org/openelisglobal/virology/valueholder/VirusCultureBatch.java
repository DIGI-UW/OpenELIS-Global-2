package org.openelisglobal.virology.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.systemuser.valueholder.SystemUser;
import org.openelisglobal.validation.annotations.SafeHtml;

/**
 * Main entity that tracks a virus culture batch through all 9 process steps of
 * the virus culture growth workflow (Stage 2)
 */
@Entity
@Table(name = "virus_culture_batch")
@DynamicUpdate
@org.hibernate.annotations.OptimisticLocking(type = org.hibernate.annotations.OptimisticLockType.VERSION)
public class VirusCultureBatch extends BaseObject<Integer> {

    public enum BatchStatus {
        MEDIA_PREP_PENDING, MEDIA_PREP_IN_PROGRESS, MEDIA_PREP_COMPLETE, STERILIZATION_PENDING,
        STERILIZATION_IN_PROGRESS, STERILIZATION_COMPLETE, CELL_CULTURE_PENDING, CELL_CULTURE_IN_PROGRESS,
        CELL_CULTURE_COMPLETE, QUALITY_CONTROL_PENDING, QUALITY_CONTROL_IN_PROGRESS, QUALITY_CONTROL_COMPLETE,
        VIRUS_CULTURE_PENDING, VIRUS_CULTURE_IN_PROGRESS, VIRUS_CULTURE_COMPLETE, DARK_ROOM_IMAGING_PENDING,
        DARK_ROOM_IMAGING_IN_PROGRESS, DARK_ROOM_IMAGING_COMPLETE, FORMULATION_PENDING, FORMULATION_IN_PROGRESS,
        FORMULATION_COMPLETE, FEEDING_PENDING, FEEDING_IN_PROGRESS, FEEDING_COMPLETE, PACKAGING_PENDING,
        PACKAGING_IN_PROGRESS, PACKAGING_COMPLETE, WORKFLOW_COMPLETE, FAILED, CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "virus_culture_batch_seq")
    @SequenceGenerator(name = "virus_culture_batch_seq", sequenceName = "virus_culture_batch_seq", allocationSize = 1)
    private Integer id;

    @NotNull
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "batch_id", unique = true, nullable = false)
    private String batchId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_page_sample_id", nullable = false)
    @JsonIgnore
    private NotebookPageSample notebookPageSample;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BatchStatus status = BatchStatus.MEDIA_PREP_PENDING;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "virus_strain")
    private String virusStrain;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "cell_line_used")
    private String cellLineUsed;

    @Column(name = "passage_number")
    private Integer passageNumber;

    @Column(name = "culture_start_date")
    private Timestamp cultureStartDate;

    @Column(name = "culture_end_date")
    private Timestamp cultureEndDate;

    @Column(name = "temperature_celsius", precision = 5, scale = 2)
    private Double temperatureCelsius;

    @Column(name = "co2_percentage", precision = 5, scale = 2)
    private Double co2Percentage;

    @Column(name = "duration_hours")
    private Integer durationHours;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "titer_value")
    private String titerValue;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "titer_unit")
    private String titerUnit;

    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    @Column(name = "final_batch_id")
    private String finalBatchId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @JsonIgnore
    private SystemUser createdBy;

    @Column(name = "created_date")
    private Timestamp createdDate;

    // UUID for FHIR integration
    @Column(name = "fhir_uuid", columnDefinition = "uuid")
    private UUID fhirUuid;

    // Child entities
    @OneToMany(mappedBy = "cultureBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VirusCultureMediaPreparation> mediaPreparations;

    @OneToMany(mappedBy = "cultureBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VirusCultureSterilization> sterilizations;

    @OneToMany(mappedBy = "cultureBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VirusCultureCellCulture> cellCultures;

    @OneToMany(mappedBy = "cultureBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VirusCultureQualityControl> qualityControls;

    @OneToMany(mappedBy = "cultureBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VirusCultureVirusInoculation> virusInoculations;

    @OneToMany(mappedBy = "cultureBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VirusCultureImaging> imagings;

    @OneToMany(mappedBy = "cultureBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VirusCultureFormulation> formulations;

    @OneToMany(mappedBy = "cultureBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VirusCultureFeeding> feedings;

    @OneToMany(mappedBy = "cultureBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VirusCulturePackaging> packagings;

    @OneToMany(mappedBy = "cultureBatch", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<VirusCultureWorkflowStatus> workflowStatuses;

    // Constructors
    public VirusCultureBatch() {
        super();
    }

    public VirusCultureBatch(String batchId, NotebookPageSample notebookPageSample) {
        this();
        this.batchId = batchId;
        this.notebookPageSample = notebookPageSample;
        this.createdDate = new Timestamp(System.currentTimeMillis());
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (fhirUuid == null) {
            fhirUuid = UUID.randomUUID();
        }
        if (createdDate == null) {
            createdDate = new Timestamp(System.currentTimeMillis());
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

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public NotebookPageSample getNotebookPageSample() {
        return notebookPageSample;
    }

    public void setNotebookPageSample(NotebookPageSample notebookPageSample) {
        this.notebookPageSample = notebookPageSample;
    }

    // Convenience method to get notebook page sample ID
    @Transient
    public Integer getNotebookPageSampleId() {
        return notebookPageSample != null ? notebookPageSample.getId() : null;
    }

    public BatchStatus getStatus() {
        return status;
    }

    public void setStatus(BatchStatus status) {
        this.status = status;
    }

    public String getVirusStrain() {
        return virusStrain;
    }

    public void setVirusStrain(String virusStrain) {
        this.virusStrain = virusStrain;
    }

    public String getCellLineUsed() {
        return cellLineUsed;
    }

    public void setCellLineUsed(String cellLineUsed) {
        this.cellLineUsed = cellLineUsed;
    }

    public Integer getPassageNumber() {
        return passageNumber;
    }

    public void setPassageNumber(Integer passageNumber) {
        this.passageNumber = passageNumber;
    }

    public Timestamp getCultureStartDate() {
        return cultureStartDate;
    }

    public void setCultureStartDate(Timestamp cultureStartDate) {
        this.cultureStartDate = cultureStartDate;
    }

    public Timestamp getCultureEndDate() {
        return cultureEndDate;
    }

    public void setCultureEndDate(Timestamp cultureEndDate) {
        this.cultureEndDate = cultureEndDate;
    }

    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(Double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public Double getCo2Percentage() {
        return co2Percentage;
    }

    public void setCo2Percentage(Double co2Percentage) {
        this.co2Percentage = co2Percentage;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public String getTiterValue() {
        return titerValue;
    }

    public void setTiterValue(String titerValue) {
        this.titerValue = titerValue;
    }

    public String getTiterUnit() {
        return titerUnit;
    }

    public void setTiterUnit(String titerUnit) {
        this.titerUnit = titerUnit;
    }

    public String getFinalBatchId() {
        return finalBatchId;
    }

    public void setFinalBatchId(String finalBatchId) {
        this.finalBatchId = finalBatchId;
    }

    public SystemUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(SystemUser createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getFhirUuidAsString() {
        return fhirUuid != null ? fhirUuid.toString() : null;
    }

    // Child entity getters with null safety
    public List<VirusCultureMediaPreparation> getMediaPreparations() {
        if (mediaPreparations == null) {
            mediaPreparations = new ArrayList<>();
        }
        return mediaPreparations;
    }

    public void setMediaPreparations(List<VirusCultureMediaPreparation> mediaPreparations) {
        this.mediaPreparations = mediaPreparations;
    }

    public List<VirusCultureSterilization> getSterilizations() {
        if (sterilizations == null) {
            sterilizations = new ArrayList<>();
        }
        return sterilizations;
    }

    public void setSterilizations(List<VirusCultureSterilization> sterilizations) {
        this.sterilizations = sterilizations;
    }

    public List<VirusCultureCellCulture> getCellCultures() {
        if (cellCultures == null) {
            cellCultures = new ArrayList<>();
        }
        return cellCultures;
    }

    public void setCellCultures(List<VirusCultureCellCulture> cellCultures) {
        this.cellCultures = cellCultures;
    }

    public List<VirusCultureQualityControl> getQualityControls() {
        if (qualityControls == null) {
            qualityControls = new ArrayList<>();
        }
        return qualityControls;
    }

    public void setQualityControls(List<VirusCultureQualityControl> qualityControls) {
        this.qualityControls = qualityControls;
    }

    public List<VirusCultureVirusInoculation> getVirusInoculations() {
        if (virusInoculations == null) {
            virusInoculations = new ArrayList<>();
        }
        return virusInoculations;
    }

    public void setVirusInoculations(List<VirusCultureVirusInoculation> virusInoculations) {
        this.virusInoculations = virusInoculations;
    }

    public List<VirusCultureImaging> getImagings() {
        if (imagings == null) {
            imagings = new ArrayList<>();
        }
        return imagings;
    }

    public void setImagings(List<VirusCultureImaging> imagings) {
        this.imagings = imagings;
    }

    public List<VirusCultureFormulation> getFormulations() {
        if (formulations == null) {
            formulations = new ArrayList<>();
        }
        return formulations;
    }

    public void setFormulations(List<VirusCultureFormulation> formulations) {
        this.formulations = formulations;
    }

    public List<VirusCultureFeeding> getFeedings() {
        if (feedings == null) {
            feedings = new ArrayList<>();
        }
        return feedings;
    }

    public void setFeedings(List<VirusCultureFeeding> feedings) {
        this.feedings = feedings;
    }

    public List<VirusCulturePackaging> getPackagings() {
        if (packagings == null) {
            packagings = new ArrayList<>();
        }
        return packagings;
    }

    public void setPackagings(List<VirusCulturePackaging> packagings) {
        this.packagings = packagings;
    }

    public List<VirusCultureWorkflowStatus> getWorkflowStatuses() {
        if (workflowStatuses == null) {
            workflowStatuses = new ArrayList<>();
        }
        return workflowStatuses;
    }

    public void setWorkflowStatuses(List<VirusCultureWorkflowStatus> workflowStatuses) {
        this.workflowStatuses = workflowStatuses;
    }

    // Utility methods
    public boolean isWorkflowComplete() {
        return status == BatchStatus.WORKFLOW_COMPLETE;
    }

    public boolean isFailed() {
        return status == BatchStatus.FAILED;
    }

    public boolean isCancelled() {
        return status == BatchStatus.CANCELLED;
    }

    public boolean isActive() {
        return !isFailed() && !isCancelled() && !isWorkflowComplete();
    }

    @Override
    public String toString() {
        return "VirusCultureBatch{" + "id=" + id + ", batchId='" + batchId + '\'' + ", status=" + status
                + ", virusStrain='" + virusStrain + '\'' + ", cellLineUsed='" + cellLineUsed + '\'' + ", passageNumber="
                + passageNumber + '}';
    }
}