package org.openelisglobal.labelpreset.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

/**
 * One aggregated cell in the Order Entry Labels section — the resolved
 * {@code default}/{@code max}/{@code locked}/{@code source} for a given
 * (sample, preset) or (order, preset) pair. Mirrors {@code LabelCell} in
 * {@code contracts/openapi.yaml} §8.1, computed per the FRS §4.4.1
 * conflict-resolution rules.
 *
 * <p>
 * {@code source_test_id} / {@code source_test_name} are populated only when
 * {@code source == TEST}; they are omitted from the JSON when null.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
public class LabelCell implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("preset_id")
    private Integer presetId;

    @JsonProperty("default")
    private int defaultQty;

    @JsonProperty("max")
    private int max;

    @JsonProperty("locked")
    private boolean locked;

    @JsonProperty("source")
    private SourceType source;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("source_test_id")
    private Long sourceTestId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("source_test_name")
    private String sourceTestName;

    public LabelCell() {
    }

    public Integer getPresetId() {
        return presetId;
    }

    public void setPresetId(Integer presetId) {
        this.presetId = presetId;
    }

    public int getDefaultQty() {
        return defaultQty;
    }

    public void setDefaultQty(int defaultQty) {
        this.defaultQty = defaultQty;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public boolean getLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public SourceType getSource() {
        return source;
    }

    public void setSource(SourceType source) {
        this.source = source;
    }

    public Long getSourceTestId() {
        return sourceTestId;
    }

    public void setSourceTestId(Long sourceTestId) {
        this.sourceTestId = sourceTestId;
    }

    public String getSourceTestName() {
        return sourceTestName;
    }

    public void setSourceTestName(String sourceTestName) {
        this.sourceTestName = sourceTestName;
    }
}
