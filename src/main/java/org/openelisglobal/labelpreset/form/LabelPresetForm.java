package org.openelisglobal.labelpreset.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.labelpreset.valueholder.BarcodeType;

/**
 * Spring form bean for creating/updating a LabelPreset. Carries @Valid
 * annotations for: name required/120-char, heightMm/widthMm 5-200, barcodeType
 * not-null, at least one scope flag (order or sample), and max >= default per
 * scope.
 */
public class LabelPresetForm {

    @NotBlank(message = "{error.labelpreset.name.required}")
    @Size(max = 120, message = "{error.labelpreset.name.toolong}")
    private String name;

    @NotNull(message = "{error.labelpreset.heightMm.required}")
    @Min(value = 5, message = "{error.labelpreset.heightMm.range}")
    @Max(value = 200, message = "{error.labelpreset.heightMm.range}")
    private Integer heightMm;

    @NotNull(message = "{error.labelpreset.widthMm.required}")
    @Min(value = 5, message = "{error.labelpreset.widthMm.range}")
    @Max(value = 200, message = "{error.labelpreset.widthMm.range}")
    private Integer widthMm;

    @NotNull(message = "{error.labelpreset.barcodeType.required}")
    private BarcodeType barcodeType;

    private Boolean printsPerOrder = false;

    private Boolean printsPerSample = true;

    @Min(value = 0, message = "{error.labelpreset.defaultPerOrder.range}")
    private Integer defaultPerOrder = 0;

    @Min(value = 0, message = "{error.labelpreset.maxPerOrder.range}")
    private Integer maxPerOrder = 10;

    @Min(value = 0, message = "{error.labelpreset.defaultPerSample.range}")
    private Integer defaultPerSample = 0;

    @Min(value = 0, message = "{error.labelpreset.maxPerSample.range}")
    private Integer maxPerSample = 10;

    private Boolean isActive = true;

    @Valid
    private List<FieldEntry> fields = new ArrayList<>();

    /** At least one scope flag must be true. */
    @AssertTrue(message = "{error.labelpreset.scope.required}")
    public boolean isAtLeastOneScopeSelected() {
        return Boolean.TRUE.equals(printsPerOrder) || Boolean.TRUE.equals(printsPerSample);
    }

    /** For order scope: maxPerOrder >= defaultPerOrder. */
    @AssertTrue(message = "{error.labelpreset.maxPerOrder.lte}")
    public boolean isMaxPerOrderGteDefault() {
        if (!Boolean.TRUE.equals(printsPerOrder)) {
            return true; // validation only applies when order scope is enabled
        }
        if (maxPerOrder == null || defaultPerOrder == null) {
            return true;
        }
        return maxPerOrder >= defaultPerOrder;
    }

    /** For sample scope: maxPerSample >= defaultPerSample. */
    @AssertTrue(message = "{error.labelpreset.maxPerSample.lte}")
    public boolean isMaxPerSampleGteDefault() {
        if (!Boolean.TRUE.equals(printsPerSample)) {
            return true; // validation only applies when sample scope is enabled
        }
        if (maxPerSample == null || defaultPerSample == null) {
            return true;
        }
        return maxPerSample >= defaultPerSample;
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getHeightMm() {
        return heightMm;
    }

    public void setHeightMm(Integer heightMm) {
        this.heightMm = heightMm;
    }

    public Integer getWidthMm() {
        return widthMm;
    }

    public void setWidthMm(Integer widthMm) {
        this.widthMm = widthMm;
    }

    public BarcodeType getBarcodeType() {
        return barcodeType;
    }

    public void setBarcodeType(BarcodeType barcodeType) {
        this.barcodeType = barcodeType;
    }

    public Boolean getPrintsPerOrder() {
        return printsPerOrder;
    }

    public void setPrintsPerOrder(Boolean printsPerOrder) {
        this.printsPerOrder = printsPerOrder;
    }

    public Boolean getPrintsPerSample() {
        return printsPerSample;
    }

    public void setPrintsPerSample(Boolean printsPerSample) {
        this.printsPerSample = printsPerSample;
    }

    public Integer getDefaultPerOrder() {
        return defaultPerOrder;
    }

    public void setDefaultPerOrder(Integer defaultPerOrder) {
        this.defaultPerOrder = defaultPerOrder;
    }

    public Integer getMaxPerOrder() {
        return maxPerOrder;
    }

    public void setMaxPerOrder(Integer maxPerOrder) {
        this.maxPerOrder = maxPerOrder;
    }

    public Integer getDefaultPerSample() {
        return defaultPerSample;
    }

    public void setDefaultPerSample(Integer defaultPerSample) {
        this.defaultPerSample = defaultPerSample;
    }

    public Integer getMaxPerSample() {
        return maxPerSample;
    }

    public void setMaxPerSample(Integer maxPerSample) {
        this.maxPerSample = maxPerSample;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<FieldEntry> getFields() {
        return fields;
    }

    public void setFields(List<FieldEntry> fields) {
        this.fields = fields;
    }

    /**
     * Nested DTO for content field entries within a label preset write request.
     */
    public static class FieldEntry {

        @NotBlank(message = "{error.labelpreset.field.key.required}")
        @Size(max = 60, message = "{error.labelpreset.field.key.toolong}")
        private String fieldKey;

        private Boolean isRequired = false;

        @NotNull(message = "{error.labelpreset.field.displayOrder.required}")
        @Min(value = 0, message = "{error.labelpreset.field.displayOrder.range}")
        private Integer displayOrder;

        public String getFieldKey() {
            return fieldKey;
        }

        public void setFieldKey(String fieldKey) {
            this.fieldKey = fieldKey;
        }

        public Boolean getIsRequired() {
            return isRequired;
        }

        public void setIsRequired(Boolean isRequired) {
            this.isRequired = isRequired;
        }

        public Integer getDisplayOrder() {
            return displayOrder;
        }

        public void setDisplayOrder(Integer displayOrder) {
            this.displayOrder = displayOrder;
        }
    }
}
