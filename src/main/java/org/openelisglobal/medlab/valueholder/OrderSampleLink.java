/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) CIRG, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.medlab.valueholder;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.UUID;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * OrderSampleLink entity - links samples to their fulfilling orders.
 *
 * <p>
 * This table enables order-driven validation (FR-021, FR-025):
 * <ul>
 * <li>Samples without corresponding orders are rejected at QC
 * <li>Stores requirements snapshot at link time for audit trail
 * <li>Tracks validation status for QC workflow
 * </ul>
 */
@Entity
@Table(name = "order_sample_link")
public class OrderSampleLink extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_sample_link_seq")
    @SequenceGenerator(name = "order_sample_link_seq", sequenceName = "order_sample_link_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "electronic_order_id", nullable = false)
    private Integer electronicOrderId;

    @Column(name = "sample_id", nullable = false)
    private Integer sampleId;

    @Column(name = "sample_item_id")
    private Integer sampleItemId;

    @Column(name = "test_id")
    private Integer testId;

    // Requirements snapshot at link time (audit trail)
    @Column(name = "container_type_required", length = 100)
    private String containerTypeRequired;

    @Column(name = "volume_required_ml", precision = 5, scale = 2)
    private BigDecimal volumeRequiredMl;

    @Column(name = "handling_requirements", length = 500)
    private String handlingRequirements;

    // Validation status
    @Column(name = "validated", nullable = false)
    private Boolean validated = false;

    @Column(name = "validated_by")
    private Integer validatedBy;

    @Column(name = "validated_at")
    private Timestamp validatedAt;

    // Audit fields
    @Column(name = "created_by")
    private Integer createdBy;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "fhir_uuid", unique = true)
    private UUID fhirUuid;

    public OrderSampleLink() {
        super();
        this.createdAt = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getElectronicOrderId() {
        return electronicOrderId;
    }

    public void setElectronicOrderId(Integer electronicOrderId) {
        this.electronicOrderId = electronicOrderId;
    }

    public Integer getSampleId() {
        return sampleId;
    }

    public void setSampleId(Integer sampleId) {
        this.sampleId = sampleId;
    }

    public Integer getSampleItemId() {
        return sampleItemId;
    }

    public void setSampleItemId(Integer sampleItemId) {
        this.sampleItemId = sampleItemId;
    }

    public Integer getTestId() {
        return testId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
    }

    public String getContainerTypeRequired() {
        return containerTypeRequired;
    }

    public void setContainerTypeRequired(String containerTypeRequired) {
        this.containerTypeRequired = containerTypeRequired;
    }

    public BigDecimal getVolumeRequiredMl() {
        return volumeRequiredMl;
    }

    public void setVolumeRequiredMl(BigDecimal volumeRequiredMl) {
        this.volumeRequiredMl = volumeRequiredMl;
    }

    public String getHandlingRequirements() {
        return handlingRequirements;
    }

    public void setHandlingRequirements(String handlingRequirements) {
        this.handlingRequirements = handlingRequirements;
    }

    public Boolean getValidated() {
        return validated;
    }

    public void setValidated(Boolean validated) {
        this.validated = validated;
    }

    public Integer getValidatedBy() {
        return validatedBy;
    }

    public void setValidatedBy(Integer validatedBy) {
        this.validatedBy = validatedBy;
    }

    public Timestamp getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Timestamp validatedAt) {
        this.validatedAt = validatedAt;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    /**
     * Checks if this link has been validated at QC.
     *
     * @return true if validated, false otherwise
     */
    public boolean isValidated() {
        return Boolean.TRUE.equals(validated);
    }

    /**
     * Marks this link as validated.
     *
     * @param validatorId the user who validated
     */
    public void markValidated(Integer validatorId) {
        this.validated = true;
        this.validatedBy = validatorId;
        this.validatedAt = new Timestamp(System.currentTimeMillis());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        OrderSampleLink that = (OrderSampleLink) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
