package org.openelisglobal.shipment.valueholder;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.test.valueholder.Test;

/**
 * Represents a sample with referral tests that needs to be assigned to a
 * shipment box
 */
@Entity
@Table(name = "unassigned_sample")
@AttributeOverride(name = "lastupdated", column = @Column(name = "lastupdated"))
public class UnassignedSample extends BaseObject<Integer> {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "unassigned_sample_seq")
    @SequenceGenerator(name = "unassigned_sample_seq", sequenceName = "unassigned_sample_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false, unique = true)
    private Sample sample;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "referral_test_id", nullable = false)
    private Test referralTest;

    @Column(name = "created_date", nullable = false)
    private Timestamp createdDate;

    @Column(name = "assigned_date")
    private Timestamp assignedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_facility_id")
    private Organization destinationFacility;

    @Column(name = "priority", length = 20)
    private String priority;

    public UnassignedSample() {
    }

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

    public Test getReferralTest() {
        return referralTest;
    }

    public void setReferralTest(Test referralTest) {
        this.referralTest = referralTest;
    }

    public Timestamp getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Timestamp createdDate) {
        this.createdDate = createdDate;
    }

    public Timestamp getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(Timestamp assignedDate) {
        this.assignedDate = assignedDate;
    }

    public Organization getDestinationFacility() {
        return destinationFacility;
    }

    public void setDestinationFacility(Organization destinationFacility) {
        this.destinationFacility = destinationFacility;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    // Helper methods
    public boolean isAssigned() {
        return assignedDate != null;
    }
}
