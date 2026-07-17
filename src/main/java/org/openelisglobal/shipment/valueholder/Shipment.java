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
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * Represents shipping/tracking details for a box in transit
 */
@Entity
@Table(name = "shipment")
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class Shipment extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shipment_seq")
    @SequenceGenerator(name = "shipment_seq", sequenceName = "shipment_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_box_id", nullable = false)
    private ShippingBox shippingBox;

    @Column(name = "courier", length = 100)
    private String courier;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "shipped_date")
    private Timestamp shippedDate;

    @Column(name = "estimated_delivery_date")
    private Timestamp estimatedDeliveryDate;

    @Column(name = "actual_delivery_date")
    private Timestamp actualDeliveryDate;

    @Column(name = "sender_notes", columnDefinition = "TEXT")
    private String senderNotes;

    @Column(name = "receiver_notes", columnDefinition = "TEXT")
    private String receiverNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ShipmentStatus status;

    public Shipment() {
        this.status = ShipmentStatus.PENDING;
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

    public String getCourier() {
        return courier;
    }

    public void setCourier(String courier) {
        this.courier = courier;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public Timestamp getShippedDate() {
        return shippedDate;
    }

    public void setShippedDate(Timestamp shippedDate) {
        this.shippedDate = shippedDate;
    }

    public Timestamp getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(Timestamp estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public Timestamp getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    public void setActualDeliveryDate(Timestamp actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }

    public String getSenderNotes() {
        return senderNotes;
    }

    public void setSenderNotes(String senderNotes) {
        this.senderNotes = senderNotes;
    }

    public String getReceiverNotes() {
        return receiverNotes;
    }

    public void setReceiverNotes(String receiverNotes) {
        this.receiverNotes = receiverNotes;
    }

    public ShipmentStatus getStatus() {
        return status;
    }

    public void setStatus(ShipmentStatus status) {
        this.status = status;
    }
}
