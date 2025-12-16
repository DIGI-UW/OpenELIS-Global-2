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
import org.hibernate.annotations.Immutable;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * SampleStorageMovement entity - Immutable audit log of SampleItem movements
 * Insert-only, no updates/deletes allowed Uses flexible assignment model:
 * locationId + locationType (no StoragePosition references)
 */
@Entity
@Table(name = "SAMPLE_STORAGE_MOVEMENT")
@Immutable
public class SampleStorageMovement extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_storage_movement_seq")
    @SequenceGenerator(name = "sample_storage_movement_seq", sequenceName = "sample_storage_movement_seq", allocationSize = 1)
    @Column(name = "ID")
    private Integer id;

    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "SAMPLE_ITEM_ID", nullable = false)
    private SampleItem sampleItem;

    // Previous location (flexible assignment model)
    @Column(name = "PREVIOUS_LOCATION_ID")
    private Integer previousLocationId;

    @Column(name = "PREVIOUS_LOCATION_TYPE", length = 20)
    private String previousLocationType;

    @Column(name = "PREVIOUS_POSITION_COORDINATE", length = 50)
    private String previousPositionCoordinate;

    // New location (flexible assignment model)
    @Column(name = "NEW_LOCATION_ID")
    private Integer newLocationId;

    @Column(name = "NEW_LOCATION_TYPE", length = 20)
    private String newLocationType;

    @Column(name = "NEW_POSITION_COORDINATE", length = 50)
    private String newPositionCoordinate;

    @Column(name = "MOVED_BY_USER_ID", nullable = false)
    private Integer movedByUserId;

    @Column(name = "MOVEMENT_DATE", nullable = false)
    private Timestamp movementDate;

    @Column(name = "REASON")
    private String reason;

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

    public Integer getPreviousLocationId() {
        return previousLocationId;
    }

    public void setPreviousLocationId(Integer previousLocationId) {
        this.previousLocationId = previousLocationId;
    }

    public String getPreviousLocationType() {
        return previousLocationType;
    }

    public void setPreviousLocationType(String previousLocationType) {
        this.previousLocationType = previousLocationType;
    }

    public String getPreviousPositionCoordinate() {
        return previousPositionCoordinate;
    }

    public void setPreviousPositionCoordinate(String previousPositionCoordinate) {
        this.previousPositionCoordinate = previousPositionCoordinate;
    }

    public Integer getNewLocationId() {
        return newLocationId;
    }

    public void setNewLocationId(Integer newLocationId) {
        this.newLocationId = newLocationId;
    }

    public String getNewLocationType() {
        return newLocationType;
    }

    public void setNewLocationType(String newLocationType) {
        this.newLocationType = newLocationType;
    }

    public String getNewPositionCoordinate() {
        return newPositionCoordinate;
    }

    public void setNewPositionCoordinate(String newPositionCoordinate) {
        this.newPositionCoordinate = newPositionCoordinate;
    }

    public Integer getMovedByUserId() {
        return movedByUserId;
    }

    public void setMovedByUserId(Integer movedByUserId) {
        this.movedByUserId = movedByUserId;
    }

    public Timestamp getMovementDate() {
        return movementDate;
    }

    public void setMovementDate(Timestamp movementDate) {
        this.movementDate = movementDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @PrePersist
    protected void onCreate() {
        if (movementDate == null) {
            movementDate = new Timestamp(System.currentTimeMillis());
        }
    }
}
