package org.openelisglobal.virology.form;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Form for handling virology workflow stage progression requests.
 * Used when advancing samples between workflow stages.
 */
public class VirologyProgressionForm {

    @NotNull(message = "Entry ID is required")
    private Long entryId;

    @NotEmpty(message = "From stage is required")
    @Pattern(regexp = "^(stage1_reception|stage2_culture|stage3_vaccine)$",
             message = "Invalid from stage. Must be: stage1_reception, stage2_culture, or stage3_vaccine")
    private String fromStage;

    @NotEmpty(message = "To stage is required")
    @Pattern(regexp = "^(stage2_culture|stage3_vaccine)$",
             message = "Invalid to stage. Must be: stage2_culture or stage3_vaccine")
    private String toStage;

    @NotEmpty(message = "Sample IDs are required")
    private List<Long> sampleIds;

    // Default constructor
    public VirologyProgressionForm() {}

    // Constructor with parameters
    public VirologyProgressionForm(Long entryId, String fromStage, String toStage, List<Long> sampleIds) {
        this.entryId = entryId;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.sampleIds = sampleIds;
    }

    // Getters and setters
    public Long getEntryId() {
        return entryId;
    }

    public void setEntryId(Long entryId) {
        this.entryId = entryId;
    }

    public String getFromStage() {
        return fromStage;
    }

    public void setFromStage(String fromStage) {
        this.fromStage = fromStage;
    }

    public String getToStage() {
        return toStage;
    }

    public void setToStage(String toStage) {
        this.toStage = toStage;
    }

    public List<Long> getSampleIds() {
        return sampleIds;
    }

    public void setSampleIds(List<Long> sampleIds) {
        this.sampleIds = sampleIds;
    }

    @Override
    public String toString() {
        return "VirologyProgressionForm{" +
                "entryId=" + entryId +
                ", fromStage='" + fromStage + '\'' +
                ", toStage='" + toStage + '\'' +
                ", sampleIds=" + sampleIds +
                '}';
    }
}