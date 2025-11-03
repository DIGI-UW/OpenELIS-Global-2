package org.openelisglobal.storage.valueholder;

import java.sql.Timestamp;
import jakarta.persistence.*;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.valueholder.Sample;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "sample_storage_movement")
@Immutable
public class SampleStorageMovement extends BaseObject<String> {

    @Id
    @GeneratedValue(generator = "sample_storage_movement_generator")
    @GenericGenerator(name = "sample_storage_movement_generator", strategy = "org.openelisglobal.hibernate.resources.StringSequenceGenerator", 
        parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "sample_storage_movement_seq"))
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "sample_id", nullable = false)
    private Sample sample;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "previous_position_id")
    private StoragePosition previousPosition;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "new_position_id")
    private StoragePosition newPosition;

    @Column(name = "moved_by_user_id", nullable = false)
    private String movedByUserId;

    @Column(name = "movement_date", nullable = false)
    private Timestamp movementDate;

    @Column(name = "reason")
    private String reason;

    @PrePersist
    protected void onCreate() {
        if (movementDate == null) {
            movementDate = new Timestamp(System.currentTimeMillis());
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

    public StoragePosition getPreviousPosition() {
        return previousPosition;
    }

    public void setPreviousPosition(StoragePosition previousPosition) {
        this.previousPosition = previousPosition;
    }

    public String getPreviousPositionId() {
        return previousPosition != null ? previousPosition.getId() : null;
    }

    public StoragePosition getNewPosition() {
        return newPosition;
    }

    public void setNewPosition(StoragePosition newPosition) {
        this.newPosition = newPosition;
    }

    public String getNewPositionId() {
        return newPosition != null ? newPosition.getId() : null;
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
}

