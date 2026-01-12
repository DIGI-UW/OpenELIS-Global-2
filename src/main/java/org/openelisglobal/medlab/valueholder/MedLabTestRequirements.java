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
import java.util.Objects;
import org.openelisglobal.common.valueholder.BaseObject;

/**
 * MedLabTestRequirements entity - configuration table linking tests to sample
 * requirements.
 *
 * <p>
 * This table drives the order-driven architecture (FR-006, FR-007):
 * <ul>
 * <li>Orders specify required container types, volumes, and handling
 * requirements
 * <li>Requirements are derived from this configuration based on ordered tests
 * <li>No modification to base Test or ElectronicOrder entities required
 * </ul>
 */
@Entity
@Table(name = "medlab_test_requirements")
public class MedLabTestRequirements extends BaseObject<Integer> {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "medlab_test_requirements_seq")
    @SequenceGenerator(name = "medlab_test_requirements_seq", sequenceName = "medlab_test_requirements_seq", allocationSize = 1)
    @Column(name = "id")
    private Integer id;

    @Column(name = "test_id", nullable = false)
    private Integer testId;

    @Column(name = "type_of_sample_id", nullable = false)
    private Integer typeOfSampleId;

    @Column(name = "container_type", nullable = false, length = 100)
    private String containerType;

    @Column(name = "volume_required_ml", nullable = false, precision = 5, scale = 2)
    private BigDecimal volumeRequiredMl;

    @Column(name = "handling_requirements", length = 500)
    private String handlingRequirements;

    @Column(name = "storage_temperature", length = 50)
    private String storageTemperature;

    @Column(name = "max_delay_minutes")
    private Integer maxDelayMinutes;

    @Column(name = "department_id")
    private Integer departmentId;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Note: lastUpdated is inherited from BaseObject

    public MedLabTestRequirements() {
        super();
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTestId() {
        return testId;
    }

    public void setTestId(Integer testId) {
        this.testId = testId;
    }

    public Integer getTypeOfSampleId() {
        return typeOfSampleId;
    }

    public void setTypeOfSampleId(Integer typeOfSampleId) {
        this.typeOfSampleId = typeOfSampleId;
    }

    public String getContainerType() {
        return containerType;
    }

    public void setContainerType(String containerType) {
        this.containerType = containerType;
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

    public String getStorageTemperature() {
        return storageTemperature;
    }

    public void setStorageTemperature(String storageTemperature) {
        this.storageTemperature = storageTemperature;
    }

    public Integer getMaxDelayMinutes() {
        return maxDelayMinutes;
    }

    public void setMaxDelayMinutes(Integer maxDelayMinutes) {
        this.maxDelayMinutes = maxDelayMinutes;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    // lastUpdated getter/setter inherited from BaseObject

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MedLabTestRequirements that = (MedLabTestRequirements) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
