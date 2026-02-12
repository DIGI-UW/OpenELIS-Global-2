package org.openelisglobal.common.management.valueholder;

/**
 * Display object for Sample Type management UI
 */
public class SampleTypeDisplay {
    private String id;
    private String description;
    private String localAbbreviation;
    private String sortOrder;
    private boolean isActive;
    private String domain;

    public SampleTypeDisplay() {
    }

    public SampleTypeDisplay(String id, String description, String localAbbreviation, String sortOrder, boolean isActive, String domain) {
        this.id = id;
        this.description = description;
        this.localAbbreviation = localAbbreviation;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
        this.domain = domain;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocalAbbreviation() {
        return localAbbreviation;
    }

    public void setLocalAbbreviation(String localAbbreviation) {
        this.localAbbreviation = localAbbreviation;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}