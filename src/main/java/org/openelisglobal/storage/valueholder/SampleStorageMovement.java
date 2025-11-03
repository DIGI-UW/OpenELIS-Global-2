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
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.Type;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * SampleStorageMovement entity - Immutable audit log of sample movements
 * Insert-only, no updates/deletes allowed
 */
@Entity
@Table(name = "SAMPLE_STORAGE_MOVEMENT")
@Immutable
public class SampleStorageMovement extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "sample_storage_movement_seq")
    @GenericGenerator(name = "sample_storage_movement_seq", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", parameters = {
            @org.hibernate.annotations.Parameter(name = "sequence_name", value = "sample_storage_movement_seq")
    })
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "ID", precision = 10, scale = 0)
    private String id;
    
    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "SAMPLE_ID", nullable = false)
    private Sample sample;
    
    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "PREVIOUS_POSITION_ID")
    private StoragePosition previousPosition;
    
    @ManyToOne(fetch = jakarta.persistence.FetchType.EAGER)
    @JoinColumn(name = "NEW_POSITION_ID")
    private StoragePosition newPosition;
    
    @Type(type = "org.openelisglobal.hibernate.resources.usertype.LIMSStringNumberUserType")
    @Column(name = "MOVED_BY_USER_ID", precision = 10, scale = 0, nullable = false)
    private String movedByUserId;
    
    @Column(name = "MOVEMENT_DATE", nullable = false)
    private Timestamp movementDate;
    
    @Column(name = "REASON")
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
