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
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * Represents the association between a shipping box and a sample item (physical
 * specimen).
 *
 * Note: This replaces the old BoxSample entity. A SampleItem is the correct
 * granularity for shipment because: - A Sample can have multiple SampleItems
 * (e.g., blood → serum + plasma) - Referrals/Analyses are linked to
 * SampleItems, not Samples - Physical specimens in boxes are SampleItems with
 * specific TypeOfSample (serum, urine, etc.)
 */
@Entity
@Table(name = "box_sample_item")
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class BoxSampleItem extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "box_sample_item_seq")
    @SequenceGenerator(name = "box_sample_item_seq", sequenceName = "box_sample_item_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_box_id", nullable = false)
    private ShippingBox shippingBox;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id", nullable = false)
    private SampleItem sampleItem;

    @Column(name = "added_date", nullable = false)
    private Timestamp addedDate;

    @Column(name = "position_in_box")
    private Integer positionInBox;

    @Enumerated(EnumType.STRING)
    @Column(name = "reception_status", length = 50)
    private ReceptionStatus receptionStatus;

    @Column(name = "reception_notes", columnDefinition = "TEXT")
    private String receptionNotes;

    @Column(name = "sys_user_id", nullable = false)
    private Integer systemUserId;

    public BoxSampleItem() {
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

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
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

    public Integer getSystemUserId() {
        return systemUserId;
    }

    public void setSystemUserId(Integer systemUserId) {
        this.systemUserId = systemUserId;
    }
}
