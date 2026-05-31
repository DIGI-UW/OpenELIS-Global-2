package org.openelisglobal.biorepository.controller.rest.dto;

import java.util.ArrayList;
import java.util.List;

public class SampleLifecycleResponseDTO {

    private Integer sampleItemId;
    private Integer bioSampleId;
    private String sampleExternalId;
    private String accessionNumber;
    private SampleLifecycleStateDTO currentState;
    private SampleTransferSummaryDTO transferSummary;
    private List<SampleLifecycleEventDTO> events = new ArrayList<>();

    public Integer getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(Integer sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public Integer getBioSampleId() {
        return bioSampleId;
    }

    public void setBioSampleId(Integer bioSampleId) {
        this.bioSampleId = bioSampleId;
    }

    public String getSampleExternalId() {
        return sampleExternalId;
    }

    public void setSampleExternalId(String sampleExternalId) {
        this.sampleExternalId = sampleExternalId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public SampleLifecycleStateDTO getCurrentState() {
        return currentState;
    }

    public void setCurrentState(SampleLifecycleStateDTO currentState) {
        this.currentState = currentState;
    }

    public SampleTransferSummaryDTO getTransferSummary() {
        return transferSummary;
    }

    public void setTransferSummary(SampleTransferSummaryDTO transferSummary) {
        this.transferSummary = transferSummary;
    }

    public List<SampleLifecycleEventDTO> getEvents() {
        return events;
    }

    public void setEvents(List<SampleLifecycleEventDTO> events) {
        this.events = events;
    }
}
