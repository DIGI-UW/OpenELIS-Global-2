package org.openelisglobal.sampletyperequest.dto;

import java.util.List;
import org.openelisglobal.sampletyperequest.valueholder.SampleTypeRequest;
import org.openelisglobal.test.dto.TestSelectionDTO;

/**
 * DTO for SampleTypeRequest - used in REST API responses.
 */
public class SampleTypeRequestDTO {

    private String id;
    private String sampleId;
    private String typeOfSampleId;
    private String typeOfSampleName;
    private Integer sortOrder;
    private Double requestedQuantity;
    private String unitOfMeasureId;
    private String unitOfMeasureName;
    private String requestedTests;
    private String requestedTestNames;
    private List<TestSelectionDTO> requestedTestDetails;
    private String requestedPanels;
    private String requestedPanelNames;
    private String status;
    private String sampleItemId;
    private String createdDate;
    private Integer cultureSetNumber;
    private String container;
    private String bodySite;
    private String collectionDate;
    private String collectionTime;

    // Default constructor
    public SampleTypeRequestDTO() {
    }

    // Constructor from entity
    public SampleTypeRequestDTO(SampleTypeRequest entity) {
        this.id = entity.getId() != null ? entity.getId().toString() : null;
        this.sampleId = entity.getSample() != null ? entity.getSample().getId() : null;
        this.typeOfSampleId = entity.getTypeOfSample() != null ? entity.getTypeOfSample().getId() : null;
        this.typeOfSampleName = entity.getTypeOfSample() != null ? entity.getTypeOfSample().getLocalizedName() : null;
        this.sortOrder = entity.getSortOrder();
        this.cultureSetNumber = entity.getCultureSetNumber();
        this.container = entity.getContainer();
        this.bodySite = entity.getBodySite();
        this.collectionDate = entity.getCollectionDate() == null ? null : entity.getCollectionDate().toString();
        this.collectionTime = entity.getCollectionTime();
        this.requestedQuantity = entity.getRequestedQuantity();
        this.unitOfMeasureId = entity.getUnitOfMeasure() != null ? entity.getUnitOfMeasure().getId() : null;
        this.unitOfMeasureName = entity.getUnitOfMeasure() != null ? entity.getUnitOfMeasure().getUnitOfMeasureName()
                : null;
        this.requestedTests = entity.getRequestedTests();
        this.requestedPanels = entity.getRequestedPanels();
        this.status = entity.getStatus() != null ? entity.getStatus().name() : null;
        this.sampleItemId = entity.getSampleItem() != null ? entity.getSampleItem().getId() : null;
        this.createdDate = entity.getCreatedDate() != null ? entity.getCreatedDate().toString() : null;
    }

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public String getTypeOfSampleId() {
        return typeOfSampleId;
    }

    public void setTypeOfSampleId(String typeOfSampleId) {
        this.typeOfSampleId = typeOfSampleId;
    }

    public String getTypeOfSampleName() {
        return typeOfSampleName;
    }

    public void setTypeOfSampleName(String typeOfSampleName) {
        this.typeOfSampleName = typeOfSampleName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Double getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Double requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public String getUnitOfMeasureId() {
        return unitOfMeasureId;
    }

    public void setUnitOfMeasureId(String unitOfMeasureId) {
        this.unitOfMeasureId = unitOfMeasureId;
    }

    public String getUnitOfMeasureName() {
        return unitOfMeasureName;
    }

    public void setUnitOfMeasureName(String unitOfMeasureName) {
        this.unitOfMeasureName = unitOfMeasureName;
    }

    public String getRequestedTests() {
        return requestedTests;
    }

    public void setRequestedTests(String requestedTests) {
        this.requestedTests = requestedTests;
    }

    public String getRequestedPanels() {
        return requestedPanels;
    }

    public void setRequestedPanels(String requestedPanels) {
        this.requestedPanels = requestedPanels;
    }

    public String getRequestedTestNames() {
        return requestedTestNames;
    }

    public void setRequestedTestNames(String requestedTestNames) {
        this.requestedTestNames = requestedTestNames;
    }

    public List<TestSelectionDTO> getRequestedTestDetails() {
        return requestedTestDetails;
    }

    public void setRequestedTestDetails(List<TestSelectionDTO> requestedTestDetails) {
        this.requestedTestDetails = requestedTestDetails;
    }

    public String getRequestedPanelNames() {
        return requestedPanelNames;
    }

    public void setRequestedPanelNames(String requestedPanelNames) {
        this.requestedPanelNames = requestedPanelNames;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(String sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public String getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(String createdDate) {
        this.createdDate = createdDate;
    }

    public Integer getCultureSetNumber() {
        return cultureSetNumber;
    }

    public void setCultureSetNumber(Integer cultureSetNumber) {
        this.cultureSetNumber = cultureSetNumber;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String value) {
        container = value;
    }

    public String getBodySite() {
        return bodySite;
    }

    public void setBodySite(String value) {
        bodySite = value;
    }

    public String getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(String value) {
        collectionDate = value;
    }

    public String getCollectionTime() {
        return collectionTime;
    }

    public void setCollectionTime(String value) {
        collectionTime = value;
    }

}
