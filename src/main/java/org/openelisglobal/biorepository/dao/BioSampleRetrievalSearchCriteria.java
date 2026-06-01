package org.openelisglobal.biorepository.dao;

import java.sql.Timestamp;
import java.util.Set;
import org.openelisglobal.biorepository.valueholder.BioSample.WorkflowStatus;

/**
 * Criteria for discovery-first biorepository sample retrieval search.
 */
public class BioSampleRetrievalSearchCriteria {

    private WorkflowStatus workflowStatus;
    private String identityPattern;
    private String barcodePattern;
    private String accessionPattern;
    private Set<String> sampleTypeIds;
    private String sampleTypeDescriptionPattern;
    private String originLabPattern;
    private String projectIdPattern;
    private Timestamp collectionDateFrom;
    private Timestamp collectionDateTo;
    private int limit = 50;

    public WorkflowStatus getWorkflowStatus() {
        return workflowStatus;
    }

    public void setWorkflowStatus(WorkflowStatus workflowStatus) {
        this.workflowStatus = workflowStatus;
    }

    public String getIdentityPattern() {
        return identityPattern;
    }

    public void setIdentityPattern(String identityPattern) {
        this.identityPattern = identityPattern;
    }

    public String getBarcodePattern() {
        return barcodePattern;
    }

    public void setBarcodePattern(String barcodePattern) {
        this.barcodePattern = barcodePattern;
    }

    public String getAccessionPattern() {
        return accessionPattern;
    }

    public void setAccessionPattern(String accessionPattern) {
        this.accessionPattern = accessionPattern;
    }

    public Set<String> getSampleTypeIds() {
        return sampleTypeIds;
    }

    public void setSampleTypeIds(Set<String> sampleTypeIds) {
        this.sampleTypeIds = sampleTypeIds;
    }

    public String getSampleTypeDescriptionPattern() {
        return sampleTypeDescriptionPattern;
    }

    public void setSampleTypeDescriptionPattern(String sampleTypeDescriptionPattern) {
        this.sampleTypeDescriptionPattern = sampleTypeDescriptionPattern;
    }

    public String getOriginLabPattern() {
        return originLabPattern;
    }

    public void setOriginLabPattern(String originLabPattern) {
        this.originLabPattern = originLabPattern;
    }

    public String getProjectIdPattern() {
        return projectIdPattern;
    }

    public void setProjectIdPattern(String projectIdPattern) {
        this.projectIdPattern = projectIdPattern;
    }

    public Timestamp getCollectionDateFrom() {
        return collectionDateFrom;
    }

    public void setCollectionDateFrom(Timestamp collectionDateFrom) {
        this.collectionDateFrom = collectionDateFrom;
    }

    public Timestamp getCollectionDateTo() {
        return collectionDateTo;
    }

    public void setCollectionDateTo(Timestamp collectionDateTo) {
        this.collectionDateTo = collectionDateTo;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
