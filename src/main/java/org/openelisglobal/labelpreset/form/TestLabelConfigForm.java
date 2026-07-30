package org.openelisglobal.labelpreset.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

/**
 * Spring form bean for PUT /api/tests/{id}/labelConfig (OGC-285 M4). Carries
 * the master allow-order-entry-override toggle and the list of per-preset link
 * overrides.
 */
public class TestLabelConfigForm {

    /** Master toggle — when false, Order Entry disables per-link Allow Override. */
    @NotNull
    private Boolean allowOrderEntryOverride = true;

    /** Per-preset link entries. */
    @Valid
    private List<LinkEntry> links = new ArrayList<>();

    public Boolean getAllowOrderEntryOverride() {
        return allowOrderEntryOverride;
    }

    public void setAllowOrderEntryOverride(Boolean allowOrderEntryOverride) {
        this.allowOrderEntryOverride = allowOrderEntryOverride;
    }

    public List<LinkEntry> getLinks() {
        return links;
    }

    public void setLinks(List<LinkEntry> links) {
        this.links = links;
    }

    /** Per-preset link override — sent as array element in the form payload. */
    public static class LinkEntry {

        /** FK to label_preset. */
        @NotNull
        private Integer presetId;

        @NotNull
        @Min(0)
        @Max(999)
        private Integer defaultQty;

        @NotNull
        @Min(0)
        @Max(999)
        private Integer maxQty;

        @NotNull
        private Boolean allowOverride = true;

        public Integer getPresetId() {
            return presetId;
        }

        public void setPresetId(Integer presetId) {
            this.presetId = presetId;
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
