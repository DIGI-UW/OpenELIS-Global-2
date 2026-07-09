package org.openelisglobal.labelpreset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * A column in the Order Entry Labels section (one per applicable preset).
 * Mirrors {@code LabelColumn} in {@code contracts/openapi.yaml} §8.1. Columns
 * are sorted system-presets-first (by id), then custom presets alphabetically
 * (data-model.md §6.1 step 5).
 */
public class LabelColumn implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("preset_id")
    private Integer presetId;

    @JsonProperty("name")
    private String name;

    @JsonProperty("is_system")
    private boolean isSystem;

    @JsonProperty("max")
    private int max;

    public LabelColumn() {
    }

    public LabelColumn(Integer presetId, String name, boolean isSystem, int max) {
        this.presetId = presetId;
        this.name = name;
        this.isSystem = isSystem;
        this.max = max;
    }

    public Integer getPresetId() {
        return presetId;
    }

    public void setPresetId(Integer presetId) {
        this.presetId = presetId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean getIsSystem() {
        return isSystem;
    }

    public void setIsSystem(boolean isSystem) {
        this.isSystem = isSystem;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }
}
