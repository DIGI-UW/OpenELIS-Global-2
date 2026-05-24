package org.openelisglobal.biorepository.controller.rest.dto;

import java.util.List;

/**
 * Request DTO for manifest import operations (validation and bulk
 * registration).
 */
public class ManifestImportRequest {

    private List<SampleRegistrationDTO> samples;
    private Integer shipmentId;
    private Integer departmentTestSectionId;

    public List<SampleRegistrationDTO> getSamples() {
        return samples;
    }

    public void setSamples(List<SampleRegistrationDTO> samples) {
        this.samples = samples;
    }

    public Integer getShipmentId() {
        return shipmentId;
    }

    public void setShipmentId(Integer shipmentId) {
        this.shipmentId = shipmentId;
    }

    public Integer getDepartmentTestSectionId() {
        return departmentTestSectionId;
    }

    public void setDepartmentTestSectionId(Integer departmentTestSectionId) {
        this.departmentTestSectionId = departmentTestSectionId;
    }
}
