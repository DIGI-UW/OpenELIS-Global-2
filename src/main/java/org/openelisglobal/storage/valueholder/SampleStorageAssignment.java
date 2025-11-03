package org.openelisglobal.storage.valueholder;

import java.sql.Timestamp;
import jakarta.persistence.*;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.valueholder.Sample;
import org.hibernate.annotations.GenericGenerator;

@Entity
@Table(name = "sample_storage_assignment")
public class SampleStorageAssignment extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "sample_storage_assignment_generator")
    @GenericGenerator(name = "sample_storage_assignment_generator", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", 
        parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "sample_storage_assignment_seq"))
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sample_id", nullable = false, unique = true)
    private Sample sample;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "storage_position_id", nullable = false)
    private StoragePosition storagePosition;

    @Column(name = "assigned_by_user_id", nullable = false)
    private String assignedByUserId;

    @Column(name = "assigned_date", nullable = false)
    private Timestamp assignedDate;

    @Column(name = "notes")
    private String notes;

    @PrePersist
    protected void onCreate() {
        if (assignedDate == null) {
            assignedDate = new Timestamp(System.currentTimeMillis());
        }
    }

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

    public String getSampleId() {
        return sample != null ? sample.getId() : null;
    }

    public StoragePosition getStoragePosition() {
        return storagePosition;
    }

    public void setStoragePosition(StoragePosition storagePosition) {
        this.storagePosition = storagePosition;
    }

    public String getStoragePositionId() {
        return storagePosition != null ? storagePosition.getId() : null;
    }

    public String getAssignedByUserId() {
        return assignedByUserId;
    }

    public void setAssignedByUserId(String assignedByUserId) {
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
}

