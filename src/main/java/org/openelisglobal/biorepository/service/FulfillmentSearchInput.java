package org.openelisglobal.biorepository.service;

import java.util.Set;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;

/**
 * Input parameters for phased fulfillment sample search.
 */
public class FulfillmentSearchInput {

    private WorkflowStatus filterStatus;
    private Set<String> matchingTypeIds;
    private String identity;
    private String barcode;
    private String accessionNumber;
    private String sampleType;
    private String originLab;
    private String projectId;
    private String requesterLabUnit;
    private String collectionDateFrom;
    private String collectionDateTo;
    private int limit = 50;

    public WorkflowStatus getFilterStatus() {
        return filterStatus;
    }

    public void setFilterStatus(WorkflowStatus filterStatus) {
        this.filterStatus = filterStatus;
    }

    public Set<String> getMatchingTypeIds() {
        return matchingTypeIds;
    }

    public void setMatchingTypeIds(Set<String> matchingTypeIds) {
        this.matchingTypeIds = matchingTypeIds;
    }

    public String getIdentity() {
        return identity;
    }

    public void setIdentity(String identity) {
        this.identity = identity;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getOriginLab() {
        return originLab;
    }

    public void setOriginLab(String originLab) {
        this.originLab = originLab;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getRequesterLabUnit() {
        return requesterLabUnit;
    }

    public void setRequesterLabUnit(String requesterLabUnit) {
        this.requesterLabUnit = requesterLabUnit;
    }

    public String getCollectionDateFrom() {
        return collectionDateFrom;
    }

    public void setCollectionDateFrom(String collectionDateFrom) {
        this.collectionDateFrom = collectionDateFrom;
    }

    public String getCollectionDateTo() {
        return collectionDateTo;
    }

    public void setCollectionDateTo(String collectionDateTo) {
        this.collectionDateTo = collectionDateTo;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
