package org.openelisglobal.storage.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
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

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "SAMPLE_ITEM_ID", nullable = false, unique = true)
    private SampleItem sampleItem;

    // Simplified polymorphic location relationship
    @Column(name = "LOCATION_ID", nullable = false)
    private Integer locationId; // Can reference device, shelf, or rack ID

    @Column(name = "LOCATION_TYPE", length = 20, nullable = false)
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

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
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
