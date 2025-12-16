package org.openelisglobal.notebook.form;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notebook.valueholder.SampleRouting.DestinationType;
import org.openelisglobal.validation.annotations.SafeHtml;

/** Form bean for sample routing operations. */
public class SampleRoutingForm {

    private Integer notebookId;
    private List<Integer> sampleItemIds;
    private DestinationType destinationType;

    // For INTERNAL_ANALYSIS
    private Integer boxId;
    private Map<Integer, String> wellAssignments;

    // For EXTERNAL_LAB
    @SafeHtml(level = SafeHtml.SafeListLevel.NONE)
    private String externalLabName;

    private LocalDate shipmentDate;

    // For STORAGE
    private Integer storageAssignmentId;

    public Integer getNotebookId() {
        return notebookId;
    }

    public void setNotebookId(Integer notebookId) {
        this.notebookId = notebookId;
    }

    public List<Integer> getSampleItemIds() {
        return sampleItemIds;
    }

    public void setSampleItemIds(List<Integer> sampleItemIds) {
        this.sampleItemIds = sampleItemIds;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        this.destinationType = destinationType;
    }

    public Integer getBoxId() {
        return boxId;
    }

    public void setBoxId(Integer boxId) {
        this.boxId = boxId;
    }

    public Map<Integer, String> getWellAssignments() {
        return wellAssignments;
    }

    public void setWellAssignments(Map<Integer, String> wellAssignments) {
        this.wellAssignments = wellAssignments;
    }

    public String getExternalLabName() {
        return externalLabName;
    }

    public void setExternalLabName(String externalLabName) {
        this.externalLabName = externalLabName;
    }

    public LocalDate getShipmentDate() {
        return shipmentDate;
    }

    public void setShipmentDate(LocalDate shipmentDate) {
        this.shipmentDate = shipmentDate;
    }

    public Integer getStorageAssignmentId() {
        return storageAssignmentId;
    }

    public void setStorageAssignmentId(Integer storageAssignmentId) {
        this.storageAssignmentId = storageAssignmentId;
    }
}
