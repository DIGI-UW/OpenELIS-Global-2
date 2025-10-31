package org.openelisglobal.storage.valueholder;

import java.sql.Timestamp;
import jakarta.persistence.PrePersist;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * SampleStorageAssignment entity - Current storage location for a sample
 * Represents one-to-one relationship: one sample, one current location
 */
public class SampleStorageAssignment extends BaseObject<String> {

    private String id;
    private Sample sample;
    private StoragePosition storagePosition;
    private SystemUser assignedByUser;
    private Timestamp assignedDate;
    private String notes;

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

    public StoragePosition getStoragePosition() {
        return storagePosition;
    }

    public void setStoragePosition(StoragePosition storagePosition) {
        this.storagePosition = storagePosition;
    }

    public SystemUser getAssignedByUser() {
        return assignedByUser;
    }

    public void setAssignedByUser(SystemUser assignedByUser) {
        this.assignedByUser = assignedByUser;
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
