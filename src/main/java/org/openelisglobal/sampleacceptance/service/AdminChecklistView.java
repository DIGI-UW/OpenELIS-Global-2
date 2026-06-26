package org.openelisglobal.sampleacceptance.service;

import java.util.List;
import org.openelisglobal.dictionary.valueholder.Dictionary;

/**
 * Admin-side view of the Sample Acceptance Checklist for one navigation target
 * (a domain, or {@code ALL} for the lab-wide list). Unlike the runtime
 * {@link SampleAcceptanceEvaluation}, this carries the configuration as the
 * admin needs to see it: the domain's own items <em>including inactive
 * ones</em> (so the "Active" toggle column has something to toggle), the
 * lab-wide items, the current enforcement mode, and whether the domain
 * currently overrides the lab-wide list.
 *
 * <p>
 * Precedence rendering: when {@code domainOverrides} is true the lab-wide items
 * are shown read-only ("superseded"); when false they are the active fallback.
 * For {@code ALL} only {@code ownItems} (the lab-wide list) is populated.
 */
public class AdminChecklistView {

    private String domain;
    private String enforcement;
    private boolean domainOverrides;
    private List<Dictionary> ownItems;
    private List<Dictionary> labWideItems;

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

    public boolean isDomainOverrides() {
        return domainOverrides;
    }

    public void setDomainOverrides(boolean domainOverrides) {
        this.domainOverrides = domainOverrides;
    }

    public List<Dictionary> getOwnItems() {
        return ownItems;
    }

    public void setOwnItems(List<Dictionary> ownItems) {
        this.ownItems = ownItems;
    }

    public List<Dictionary> getLabWideItems() {
        return labWideItems;
    }

    public void setLabWideItems(List<Dictionary> labWideItems) {
        this.labWideItems = labWideItems;
    }
}
