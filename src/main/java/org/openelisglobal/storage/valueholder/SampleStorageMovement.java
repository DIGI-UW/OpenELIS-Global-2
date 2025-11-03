package org.openelisglobal.storage.valueholder;

import java.sql.Timestamp;
import jakarta.persistence.PrePersist;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * SampleStorageMovement entity - Immutable audit log of sample movements
 * Insert-only, no updates/deletes allowed
 */
public class SampleStorageMovement extends BaseObject<String> {

    private String id;
    private Sample sample;
    private StoragePosition previousPosition;
    private StoragePosition newPosition;
    private String movedByUserId;
    private Timestamp movementDate;
    private String reason;

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public StoragePosition getPreviousPosition() {
        return previousPosition;
    }

    public void setPreviousPosition(StoragePosition previousPosition) {
        this.previousPosition = previousPosition;
    }

    public StoragePosition getNewPosition() {
        return newPosition;
    }

    public void setNewPosition(StoragePosition newPosition) {
        this.newPosition = newPosition;
    }

    public String getMovedByUserId() {
        return movedByUserId;
    }

    public void setMovedByUserId(String movedByUserId) {
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
