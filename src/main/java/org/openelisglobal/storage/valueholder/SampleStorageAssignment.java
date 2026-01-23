package org.openelisglobal.storage.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.sql.Timestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * SampleStorageAssignment entity - Current storage location for a SampleItem
 * Represents one-to-one relationship: one SampleItem, one current location
 */
@Entity
@Table(name = "SAMPLE_STORAGE_ASSIGNMENT")
@DynamicUpdate
public class SampleStorageAssignment extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_storage_assignment_seq")
    @SequenceGenerator(name = "sample_storage_assignment_seq", sequenceName = "sample_storage_assignment_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    // Store sampleItemId directly instead of @ManyToOne to avoid cross-mapping
    // issues between JPA annotations and HBM XML mapping (SampleItem uses
    // LIMSStringNumberUserType which maps String in Java to numeric in DB)
    // Database column is numeric, so store as Integer here
    @Column(name = "SAMPLE_ITEM_ID", nullable = false, unique = true)
    private Integer sampleItemId;

    // Transient field for convenience - must be populated manually after loading
    @Transient
    private SampleItem sampleItem;

    // Simplified polymorphic location relationship
    // Nullable to support disposal (location cleared but assignment preserved for
    // audit/metrics)
    @Column(name = "LOCATION_ID", nullable = true)
    private Integer locationId; // Can reference device, shelf, or rack ID

    @Column(name = "LOCATION_TYPE", length = 20, nullable = true)
    private String locationType; // Enum: 'device', 'shelf', 'rack'

    @Column(name = "POSITION_COORDINATE", length = 50)
    private String positionCoordinate; // Optional text-based coordinate (position is just text, not an entity)

    @Column(name = "ASSIGNED_BY_USER_ID", nullable = false)
    private Integer assignedByUserId;

    @Column(name = "ASSIGNED_DATE", nullable = false)
    private Timestamp assignedDate;

    @Column(name = "NOTES")
    private String notes;

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

    // Convenience method to get sampleItemId as String (for compatibility with
    // SampleItem.id)
    public String getSampleItemIdAsString() {
        return sampleItemId != null ? sampleItemId.toString() : null;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
        // Also update sampleItemId when setting the transient sampleItem
        if (sampleItem != null && sampleItem.getId() != null) {
            this.sampleItemId = Integer.parseInt(sampleItem.getId());
        }
    }

    public Integer getLocationId() {
        return locationId;
    }

    public void setLocationId(Integer locationId) {
        this.locationId = locationId;
    }

    public String getLocationType() {
        return locationType;
    }

    public void setLocationType(String locationType) {
        this.locationType = locationType;
    }

    public String getPositionCoordinate() {
        return positionCoordinate;
    }

    public void setPositionCoordinate(String positionCoordinate) {
        this.positionCoordinate = positionCoordinate;
    }

    public Integer getAssignedByUserId() {
        return assignedByUserId;
    }

    public void setAssignedByUserId(Integer assignedByUserId) {
        this.assignedByUserId = assignedByUserId;
    }

    public Timestamp getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(Timestamp assignedDate) {
        this.assignedDate = assignedDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @PrePersist
    protected void onCreate() {
        if (assignedDate == null) {
            assignedDate = new Timestamp(System.currentTimeMillis());
        }
    }
}
