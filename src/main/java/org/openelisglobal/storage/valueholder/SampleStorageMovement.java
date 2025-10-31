package org.openelisglobal.storage.valueholder;

import java.sql.Timestamp;
import jakarta.persistence.PrePersist;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * SampleStorageMovement entity - Immutable audit log of sample movements
 * Insert-only, no updates/deletes allowed
 */
public class SampleStorageMovement extends BaseObject<String> {

    private String id;
    private Sample sample;
    private StoragePosition previousPosition;
    private StoragePosition newPosition;
    private SystemUser movedByUser;
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

    public SystemUser getMovedByUser() {
        return movedByUser;
    }

    public void setMovedByUser(SystemUser movedByUser) {
        this.movedByUser = movedByUser;
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
