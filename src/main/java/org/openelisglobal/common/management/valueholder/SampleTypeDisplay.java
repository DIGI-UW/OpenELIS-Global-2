package org.openelisglobal.common.management.valueholder;

import com.fasterxml.jackson.annotation.JsonProperty;

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
    private String whonetCode;
    private int testCount;
    private String storageDefaults;

    public SampleTypeDisplay() {
    }

    public SampleTypeDisplay(String id, String description, String localAbbreviation, String sortOrder, boolean isActive, String domain, String whonetCode, int testCount, String storageDefaults) {
        this.id = id;
        this.description = description;
        this.localAbbreviation = localAbbreviation;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
        this.domain = domain;
        this.whonetCode = whonetCode;
        this.testCount = testCount;
        this.storageDefaults = storageDefaults;
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

    @JsonProperty("isActive")
    public boolean isActive() {
        return isActive;
    }

    @JsonProperty("isActive")
    public void setActive(boolean active) {
        isActive = active;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getWhonetCode() {
        return whonetCode;
    }

    public void setWhonetCode(String whonetCode) {
        this.whonetCode = whonetCode;
    }

    public int getTestCount() {
        return testCount;
    }

    public void setTestCount(int testCount) {
        this.testCount = testCount;
    }

    public String getStorageDefaults() {
        return storageDefaults;
    }

    public void setStorageDefaults(String storageDefaults) {
        this.storageDefaults = storageDefaults;
    }
}