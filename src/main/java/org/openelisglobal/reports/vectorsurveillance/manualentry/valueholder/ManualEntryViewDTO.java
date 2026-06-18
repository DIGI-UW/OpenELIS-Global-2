package org.openelisglobal.reports.vectorsurveillance.manualentry.valueholder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The reporting period's surveillance numbers in field-map order (US4). Each
 * row pairs a configured field-map entry (metricKey, label, portalTag) with the
 * value derived from the surveillance indices. The sporozoite row is
 * {@code gated=true} (value null) when {@code positiveResolutionPct < 95%};
 * otherwise its computed rate is shown.
 */
public class ManualEntryViewDTO {

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer siteId;
    private List<Row> rows = new ArrayList<>();

    public ManualEntryViewDTO() {
    }

    public ManualEntryViewDTO(LocalDate periodStart, LocalDate periodEnd, Integer siteId) {
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.siteId = siteId;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Integer getSiteId() {
        return siteId;
    }

    public void setSiteId(Integer siteId) {
        this.siteId = siteId;
    }

    public List<Row> getRows() {
        return rows;
    }

    public void setRows(List<Row> rows) {
        this.rows = rows;
    }

    public static class Row {
        private String metricKey;
        private String label;
        private String portalTag;
        private String value;
        private boolean gated;

        public Row() {
        }

        public Row(String metricKey, String label, String portalTag, String value, boolean gated) {
            this.metricKey = metricKey;
            this.label = label;
            this.portalTag = portalTag;
            this.value = value;
            this.gated = gated;
        }

        public String getMetricKey() {
            return metricKey;
        }

        public void setMetricKey(String metricKey) {
            this.metricKey = metricKey;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getPortalTag() {
            return portalTag;
        }

        public void setPortalTag(String portalTag) {
            this.portalTag = portalTag;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public boolean isGated() {
            return gated;
        }

        public void setGated(boolean gated) {
            this.gated = gated;
        }
    }
}
