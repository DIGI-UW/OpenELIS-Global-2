package org.openelisglobal.storage.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * SampleStorageAssignment entity - Current storage location for a sample
 * Represents one-to-one relationship: one sample, one current location
 */
@Entity
@Table(name = "SAMPLE_STORAGE_ASSIGNMENT")
@DynamicUpdate
public class SampleStorageAssignment extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "sample_storage_assignment_seq")
    @GenericGenerator(name = "sample_storage_assignment_seq", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = {
            @org.hibernate.annotations.Parameter(name = "sequence_name", value = "sample_storage_assignment_seq")
    })
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;
    
    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "SAMPLE_ID", nullable = false, unique = true)
    private Sample sample;
    
    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "STORAGE_POSITION_ID", nullable = false)
    private StoragePosition storagePosition;
    
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ASSIGNED_BY_USER_ID", precision = 10, scale = 0, nullable = false)
    private String assignedByUserId;
    
    @Column(name = "ASSIGNED_DATE", nullable = false)
    private Timestamp assignedDate;
    
    @Column(name = "NOTES")
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

    @PrePersist
    protected void onCreate() {
        if (assignedDate == null) {
            assignedDate = new Timestamp(System.currentTimeMillis());
        }
    }
}
