package org.openelisglobal.shipment.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.systemuser.valueholder.SystemUser;

/**
 * Represents a physical box containing samples for shipment
 */
@Entity
@Table(name = "shipping_box")
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class ShippingBox extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "shipping_box_seq")
    @SequenceGenerator(name = "shipping_box_seq", sequenceName = "shipping_box_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "box_id", nullable = false, unique = true, length = 50)
    private String boxId;

    @Column(name = "fhir_uuid", nullable = false, unique = true)
    private UUID fhirUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_facility_id", nullable = false)
    private Organization destinationFacility;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 50)
    private BoxState state;

    @Column(name = "temperature_requirement", length = 50)
    private String temperatureRequirement;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_date", nullable = false)
    private Timestamp createdDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private SystemUser createdBy;

    @Column(name = "sent_date")
    private Timestamp sentDate;

    @Column(name = "received_date")
    private Timestamp receivedDate;

    @Column(name = "reconciled_date")
    private Timestamp reconciledDate;

    @Column(name = "archived", nullable = false)
    private Boolean archived = false;

    @Column(name = "archived_date")
    private Timestamp archivedDate;

    // Relationships
    @OneToMany(mappedBy = "shippingBox", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BoxSample> boxSamples = new ArrayList<>();

    @OneToOne(mappedBy = "shippingBox", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Shipment shipment;

    public ShippingBox() {
        this.fhirUuid = UUID.randomUUID();
        this.state = BoxState.DRAFT;
        this.archived = false;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public String getBoxId() {
        return boxId;
    }

    public void setBoxId(String boxId) {
        this.boxId = boxId;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public Organization getDestinationFacility() {
        return destinationFacility;
    }

    public void setDestinationFacility(Organization destinationFacility) {
        this.destinationFacility = destinationFacility;
    }

    public BoxState getState() {
        return state;
    }

    public void setState(BoxState state) {
        this.state = state;
    }

    public String getTemperatureRequirement() {
        return temperatureRequirement;
    }

    public void setTemperatureRequirement(String temperatureRequirement) {
        this.temperatureRequirement = temperatureRequirement;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public SystemUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(SystemUser createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getSentDate() {
        return sentDate;
    }

    public void setSentDate(Timestamp sentDate) {
        this.sentDate = sentDate;
    }

    public Timestamp getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Timestamp receivedDate) {
        this.receivedDate = receivedDate;
    }

    public Timestamp getReconciledDate() {
        return reconciledDate;
    }

    public void setReconciledDate(Timestamp reconciledDate) {
        this.reconciledDate = reconciledDate;
    }

    public Boolean getArchived() {
        return archived;
    }

    public void setArchived(Boolean archived) {
        this.archived = archived;
    }

    public Timestamp getArchivedDate() {
        return archivedDate;
    }

    public void setArchivedDate(Timestamp archivedDate) {
        this.archivedDate = archivedDate;
    }

    public List<BoxSample> getBoxSamples() {
        return boxSamples;
    }

    public void setBoxSamples(List<BoxSample> boxSamples) {
        this.boxSamples = boxSamples;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    // Helper methods
    public void addBoxSample(BoxSample boxSample) {
        boxSamples.add(boxSample);
        boxSample.setShippingBox(this);
    }

    public void removeBoxSample(BoxSample boxSample) {
        boxSamples.remove(boxSample);
        boxSample.setShippingBox(null);
    }

    public int getSampleCount() {
        return boxSamples != null ? boxSamples.size() : 0;
    }
}
