package org.openelisglobal.sampleacceptance.service;

import java.util.List;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.sampleacceptance.valueholder.SampleAcceptanceRecord;

/**
 * Runtime view of a specimen's acceptance state: the resolved domain, the live
 * enforcement mode, the currently-resolved checklist, the latest recorded
 * decision (if any), and whether the specimen is blocked from proceeding.
 */
public class SampleAcceptanceEvaluation {

    private String sampleItemId;
    private String domain;
    private String enforcement;
    private String overallStatus;
    private boolean blocked;
    private List<Dictionary> items;
    private SampleAcceptanceRecord latest;

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getEnforcement() {
        return enforcement;
    }

    public void setEnforcement(String enforcement) {
        this.enforcement = enforcement;
    }

    public String getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(String overallStatus) {
        this.overallStatus = overallStatus;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public List<Dictionary> getItems() {
        return items;
    }

    public void setItems(List<Dictionary> items) {
        this.items = items;
    }

    public SampleAcceptanceRecord getLatest() {
        return latest;
    }

    public void setLatest(SampleAcceptanceRecord latest) {
        this.latest = latest;
    }
}
