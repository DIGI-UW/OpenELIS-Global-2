package org.openelisglobal.notebook.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.time.LocalDate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.storage.valueholder.SampleStorageAssignment;
import org.openelisglobal.storage.valueholder.StorageBox;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Tracks sample destination routing for branching workflows. Links samples to
 * their routing destination (internal analysis, external lab, storage).
 */
@Entity
@Table(name = "sample_routing")
public class SampleRouting extends BaseObject<Integer> {

    /**
     * Destination types for sample routing.
     */
    public enum DestinationType {
        /** Sample goes to analysis box with well assignment */
        INTERNAL_ANALYSIS,
        /** Sample shipped to external laboratory */
        EXTERNAL_LAB,
        /** Sample goes to storage location */
        STORAGE,
        /** Sample transferred to biorepository for permanent archival */
        BIOREPOSITORY
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_routing_generator")
    @SequenceGenerator(name = "sample_routing_generator", sequenceName = "sample_routing_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    // Store sampleItemId directly (same pattern as SampleStorageAssignment)
    // SampleItem uses LIMSStringNumberUserType which maps String in Java to numeric
    // in DB
    @Column(name = "sample_item_id", nullable = false)
    private Integer sampleItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notebook_id", nullable = false)
    private NoteBook notebook;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type", nullable = false, length = 30)
    private DestinationType destinationType;

    @Column(name = "external_lab_name", length = 255)
    private String externalLabName;

    @Column(name = "shipment_date")
    private LocalDate shipmentDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "storage_assignment_id")
    private SampleStorageAssignment storageAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "box_id")
    private StorageBox box;

    @Column(name = "well_coordinate", length = 10)
    private String wellCoordinate;

    /**
     * Name/identifier for assay plates (temporary plates for Internal Analysis).
     * Used when routing to a temporary plate that is NOT in the storage hierarchy.
     * When this is set, box should be null.
     */
    @Column(name = "assay_plate_name", length = 100)
    private String assayPlateName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routed_by")
    private SystemUser routedBy;

    @Column(name = "routed_at")
    private Timestamp routedAt;

    // Getters and setters
    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(Integer sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public NoteBook getNotebook() {
        return notebook;
    }

    public void setNotebook(NoteBook notebook) {
        this.notebook = notebook;
    }

    public Integer getNotebookId() {
        return notebook != null ? notebook.getId() : null;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        this.destinationType = destinationType;
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

    public SampleStorageAssignment getStorageAssignment() {
        return storageAssignment;
    }

    public void setStorageAssignment(SampleStorageAssignment storageAssignment) {
        this.storageAssignment = storageAssignment;
    }

    public StorageBox getBox() {
        return box;
    }

    public void setBox(StorageBox box) {
        this.box = box;
    }

    public String getWellCoordinate() {
        return wellCoordinate;
    }

    public void setWellCoordinate(String wellCoordinate) {
        this.wellCoordinate = wellCoordinate;
    }

    public String getAssayPlateName() {
        return assayPlateName;
    }

    public void setAssayPlateName(String assayPlateName) {
        this.assayPlateName = assayPlateName;
    }

    public SystemUser getRoutedBy() {
        return routedBy;
    }

    public void setRoutedBy(SystemUser routedBy) {
        this.routedBy = routedBy;
    }

    public Timestamp getRoutedAt() {
        return routedAt;
    }

    public void setRoutedAt(Timestamp routedAt) {
        this.routedAt = routedAt;
    }

    /**
     * Validates routing constraints based on destination type.
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        if (destinationType == null) {
            return false;
        }

        switch (destinationType) {
        case INTERNAL_ANALYSIS:
            // Internal analysis requires either a storage box OR an assay plate name
            // Assay plates are temporary plates not stored in the storage hierarchy
            boolean hasBox = box != null && wellCoordinate != null && !wellCoordinate.isBlank();
            boolean hasAssayPlate = assayPlateName != null && !assayPlateName.isBlank() && wellCoordinate != null
                    && !wellCoordinate.isBlank();
            return hasBox || hasAssayPlate;
        case EXTERNAL_LAB:
            return externalLabName != null && !externalLabName.isBlank();
        case STORAGE:
            return storageAssignment != null;
        case BIOREPOSITORY:
            // Biorepository routing requires storage assignment for permanent archival
            return storageAssignment != null;
        default:
            return false;
        }
    }
}
