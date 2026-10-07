package org.openelisglobal.sampletyperequest.valueholder;

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
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.unitofmeasure.valueholder.UnitOfMeasure;

/**
 * SampleTypeRequest - Represents a requested sample type before physical
 * collection.
 *
 * <p>
 * Created during Step 1 (Enter Order) of the decoupled order workflow.
 * Fulfilled in Step 2 (Collect Sample) when actual sample_item records are
 * created.
 *
 * <p>
 * This separation allows:
 * <ul>
 * <li>Pre-registration of orders before sample collection</li>
 * <li>Partial collection scenarios (3 of 5 tubes collected today)</li>
 * <li>Clear distinction between "requested" vs "collected" samples</li>
 * </ul>
 */
@Entity
@Table(name = "sample_type_request", schema = "clinlims")
public class SampleTypeRequest extends BaseObject<Integer> {

    public enum Status {
        REQUESTED, // Initial state - sample type requested but not collected
        COLLECTED, // Sample has been physically collected (sample_item created)
        CANCELLED // Request was cancelled (e.g., patient refused)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sample_type_request_seq_gen")
    @SequenceGenerator(name = "sample_type_request_seq_gen", sequenceName = "sample_type_request_seq", schema = "clinlims", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false)
    private Sample sample;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_of_sample_id", nullable = false)
    private TypeOfSample typeOfSample;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "requested_quantity")
    private Double requestedQuantity;

    @Column(name = "culture_set_number")
    private Integer cultureSetNumber;

    @Column(name = "container", length = 255)
    private String container;
    @Column(name = "body_site", length = 40)
    private String bodySite;
    @Column(name = "collection_date")
    private java.sql.Date collectionDate;
    @Column(name = "collection_time", length = 8)
    private String collectionTime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_of_measure_id")
    private UnitOfMeasure unitOfMeasure;

    /** Comma-separated test IDs requested for this sample type */
    @Column(name = "requested_tests")
    private String requestedTests;

    /** Comma-separated panel IDs requested for this sample type */
    @Column(name = "requested_panels")
    private String requestedPanels;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.REQUESTED;

    /** Link to the fulfilled sample_item (set when collected in Step 2) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_item_id")
    private SampleItem sampleItem;

    @Column(name = "created_date", nullable = false, updatable = false)
    private Timestamp createdDate;

    // Getters and Setters

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public TypeOfSample getTypeOfSample() {
        return typeOfSample;
    }

    public void setTypeOfSample(TypeOfSample typeOfSample) {
        this.typeOfSample = typeOfSample;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Double getRequestedQuantity() {
        return requestedQuantity;
    }

    public void setRequestedQuantity(Double requestedQuantity) {
        this.requestedQuantity = requestedQuantity;
    }

    public UnitOfMeasure getUnitOfMeasure() {
        return unitOfMeasure;
    }

    public void setUnitOfMeasure(UnitOfMeasure unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }

    public String getRequestedTests() {
        return requestedTests;
    }

    public void setRequestedTests(String requestedTests) {
        this.requestedTests = requestedTests;
    }

    public String getRequestedPanels() {
        return requestedPanels;
    }

    public void setRequestedPanels(String requestedPanels) {
        this.requestedPanels = requestedPanels;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    /**
     * Checks if this request has been fulfilled (sample collected).
     */
    public boolean isFulfilled() {
        return status == Status.COLLECTED && sampleItem != null;
    }

    /**
     * Checks if this request is still pending collection.
     */
    public boolean isPending() {
        return status == Status.REQUESTED;
    }

    public Integer getCultureSetNumber() {
        return cultureSetNumber;
    }

    public void setCultureSetNumber(Integer cultureSetNumber) {
        if (cultureSetNumber != null && cultureSetNumber < 1) {
            throw new IllegalArgumentException("Culture set number must be positive");
        }
        this.cultureSetNumber = cultureSetNumber;
    }

    public String getContainer() {
        return container;
    }

    public void setContainer(String value) {
        container = value;
    }

    public String getBodySite() {
        return bodySite;
    }

    public void setBodySite(String value) {
        bodySite = value;
    }

    public java.sql.Date getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(java.sql.Date value) {
        collectionDate = value;
    }

    public String getCollectionTime() {
        return collectionTime;
    }

    public void setCollectionTime(String value) {
        collectionTime = value;
    }

}
