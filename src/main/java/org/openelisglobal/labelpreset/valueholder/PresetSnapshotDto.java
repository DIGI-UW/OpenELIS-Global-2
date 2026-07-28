package org.openelisglobal.labelpreset.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Type-safe binding for the order_label_request.preset_snapshot JSONB column
 * (FRS §7.3.1). The snapshot is frozen at order-save time and is the
 * authoritative source for reprint. Reads tolerate older shapes (forward
 * compat) via {@link JsonIgnoreProperties}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PresetSnapshotDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("preset")
    private PresetSnapshotPreset preset;

    @JsonProperty("fields")
    private List<PresetSnapshotField> fields = new ArrayList<>();

    /** Null if the cell was driven by a system default rather than a test link. */
    @JsonProperty("test_link")
    private PresetSnapshotTestLink testLink;

    public PresetSnapshotDto() {
    }

    public PresetSnapshotPreset getPreset() {
        return preset;
    }

    public void setPreset(PresetSnapshotPreset preset) {
        this.preset = preset;
    }

    public List<PresetSnapshotField> getFields() {
        return fields;
    }

    public void setFields(List<PresetSnapshotField> fields) {
        this.fields = fields;
    }

    public PresetSnapshotTestLink getTestLink() {
        return testLink;
    }

    public void setTestLink(PresetSnapshotTestLink testLink) {
        this.testLink = testLink;
    }

    /** Everything needed to render the label frame + barcode. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PresetSnapshotPreset implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("id")
        private Integer id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("height_mm")
        private Integer heightMm;

        @JsonProperty("width_mm")
        private Integer widthMm;

        @JsonProperty("barcode_type")
        private String barcodeType;

        public PresetSnapshotPreset() {
        }

        public Integer getId() {
            return id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

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

        public String getBarcodeType() {
            return barcodeType;
        }

        public void setBarcodeType(String barcodeType) {
            this.barcodeType = barcodeType;
        }
    }

    /**
     * Ordered content field; field_label decouples the rendered label from the
     * evolving field set.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PresetSnapshotField implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("field_key")
        private String fieldKey;

        @JsonProperty("field_label")
        private String fieldLabel;

        @JsonProperty("is_required")
        private Boolean isRequired;

        @JsonProperty("display_order")
        private Integer displayOrder;

        public PresetSnapshotField() {
        }

        public String getFieldKey() {
            return fieldKey;
        }

        public void setFieldKey(String fieldKey) {
            this.fieldKey = fieldKey;
        }

        public String getFieldLabel() {
            return fieldLabel;
        }

        public void setFieldLabel(String fieldLabel) {
            this.fieldLabel = fieldLabel;
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

    /** Linked-test settings captured at save time. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PresetSnapshotTestLink implements Serializable {

        private static final long serialVersionUID = 1L;

        @JsonProperty("test_id")
        private Integer testId;

        @JsonProperty("default_qty")
        private Integer defaultQty;

        @JsonProperty("max_qty")
        private Integer maxQty;

        @JsonProperty("allow_override")
        private Boolean allowOverride;

        public PresetSnapshotTestLink() {
        }

        public Integer getTestId() {
            return testId;
        }

        public void setTestId(Integer testId) {
            this.testId = testId;
        }

        public Integer getDefaultQty() {
            return defaultQty;
        }

        public void setDefaultQty(Integer defaultQty) {
            this.defaultQty = defaultQty;
        }

        public Integer getMaxQty() {
            return maxQty;
        }

        public void setMaxQty(Integer maxQty) {
            this.maxQty = maxQty;
        }

        public Boolean getAllowOverride() {
            return allowOverride;
        }

        public void setAllowOverride(Boolean allowOverride) {
            this.allowOverride = allowOverride;
        }
    }
}
