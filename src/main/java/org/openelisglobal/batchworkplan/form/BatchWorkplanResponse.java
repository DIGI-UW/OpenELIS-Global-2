package org.openelisglobal.batchworkplan.form;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.batchworkplan.valueholder.BatchWorkplanStatus;

public class BatchWorkplanResponse {

    private Long id;
    private String name;
    private BatchWorkplanStatus status;
    private String testSectionId;
    private String notes;
    private Timestamp createdAt;
    private Timestamp activatedAt;
    private Timestamp completedAt;
    private Timestamp archivedAt;
    private Integer itemCount;
    private List<BatchWorkplanItemResponse> items = new ArrayList<>();
    private Long reagentId;
    private String reagentName;
    private Long reagentLotId;
    private String reagentLotNumber;
    private Timestamp reagentLotExpiration;
    private String instrumentId;
    private String instrumentName;
    private Timestamp generatedAt;
    private String generatedBy;
    private BatchQcStatusResponse qc;
    private List<BatchReleaseResponse> releases;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BatchWorkplanStatus getStatus() {
        return status;
    }

    public void setStatus(BatchWorkplanStatus status) {
        this.status = status;
    }

    public String getTestSectionId() {
        return testSectionId;
    }

    public void setTestSectionId(String testSectionId) {
        this.testSectionId = testSectionId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Timestamp activatedAt) {
        this.activatedAt = activatedAt;
    }

    public Timestamp getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Timestamp completedAt) {
        this.completedAt = completedAt;
    }

    public Timestamp getArchivedAt() {
        return archivedAt;
    }

    public void setArchivedAt(Timestamp archivedAt) {
        this.archivedAt = archivedAt;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public List<BatchWorkplanItemResponse> getItems() {
        return items;
    }

    public void setItems(List<BatchWorkplanItemResponse> items) {
        this.items = items;
    }

    public Long getReagentId() {
        return reagentId;
    }

    public void setReagentId(Long reagentId) {
        this.reagentId = reagentId;
    }

    public String getReagentName() {
        return reagentName;
    }

    public void setReagentName(String reagentName) {
        this.reagentName = reagentName;
    }

    public Long getReagentLotId() {
        return reagentLotId;
    }

    public void setReagentLotId(Long reagentLotId) {
        this.reagentLotId = reagentLotId;
    }

    public String getReagentLotNumber() {
        return reagentLotNumber;
    }

    public void setReagentLotNumber(String reagentLotNumber) {
        this.reagentLotNumber = reagentLotNumber;
    }

    public Timestamp getReagentLotExpiration() {
        return reagentLotExpiration;
    }

    public void setReagentLotExpiration(Timestamp reagentLotExpiration) {
        this.reagentLotExpiration = reagentLotExpiration;
    }

    public String getInstrumentId() {
        return instrumentId;
    }

    public void setInstrumentId(String instrumentId) {
        this.instrumentId = instrumentId;
    }

    public String getInstrumentName() {
        return instrumentName;
    }

    public void setInstrumentName(String instrumentName) {
        this.instrumentName = instrumentName;
    }

    public Timestamp getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Timestamp generatedAt) {
        this.generatedAt = generatedAt;
    }

    public String getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(String generatedBy) {
        this.generatedBy = generatedBy;
    }

    public BatchQcStatusResponse getQc() {
        return qc;
    }

    public void setQc(BatchQcStatusResponse qc) {
        this.qc = qc;
    }

    public List<BatchReleaseResponse> getReleases() {
        return releases;
    }

    public void setReleases(List<BatchReleaseResponse> releases) {
        this.releases = releases;
    }
}
