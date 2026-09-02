package org.openelisglobal.reports.vectorsurveillance.manualentry.controller.rest;

import java.time.LocalDate;
import java.util.Map;

/**
 * Request body for POST /rest/reports/vector-surveillance/manual-entry/submit
 * (US4). {@code valueSnapshot} is the visible figures keyed by metricKey.
 */
public class ManualEntrySubmitRequest {

    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer siteId;
    private Map<String, String> valueSnapshot;

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

    public Map<String, String> getValueSnapshot() {
        return valueSnapshot;
    }

    public void setValueSnapshot(Map<String, String> valueSnapshot) {
        this.valueSnapshot = valueSnapshot;
    }
}
