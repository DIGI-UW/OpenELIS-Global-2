package org.openelisglobal.shipment.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * Represents the association between a shipping box and a sample
 */
@Entity
@Table(name = "box_sample")
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class BoxSample extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "box_sample_seq")
    @SequenceGenerator(name = "box_sample_seq", sequenceName = "box_sample_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_box_id", nullable = false)
    private ShippingBox shippingBox;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false)
    private Sample sample;

    @Column(name = "added_date", nullable = false)
    private Timestamp addedDate;

    @Column(name = "position_in_box")
    private Integer positionInBox;

    @Enumerated(EnumType.STRING)
    @Column(name = "reception_status", length = 50)
    private ReceptionStatus receptionStatus;

    @Column(name = "reception_notes", columnDefinition = "TEXT")
    private String receptionNotes;

    public BoxSample() {
        this.receptionStatus = ReceptionStatus.PENDING;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public ShippingBox getShippingBox() {
        return shippingBox;
    }

    public void setShippingBox(ShippingBox shippingBox) {
        this.shippingBox = shippingBox;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public Timestamp getAddedDate() {
        return addedDate;
    }

    public void setAddedDate(Timestamp addedDate) {
        this.addedDate = addedDate;
    }

    public Integer getPositionInBox() {
        return positionInBox;
    }

    public void setPositionInBox(Integer positionInBox) {
        this.positionInBox = positionInBox;
    }

    public ReceptionStatus getReceptionStatus() {
        return receptionStatus;
    }

    public void setReceptionStatus(ReceptionStatus receptionStatus) {
        this.receptionStatus = receptionStatus;
    }

    public String getReceptionNotes() {
        return receptionNotes;
    }

    public void setReceptionNotes(String receptionNotes) {
        this.receptionNotes = receptionNotes;
    }
}
