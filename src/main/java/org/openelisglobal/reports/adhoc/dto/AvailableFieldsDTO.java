package org.openelisglobal.reports.adhoc.dto;

import java.util.ArrayList;
import java.util.List;

public class AvailableFieldsDTO {

    private List<ReportFieldDTO> patientFields = new ArrayList<>();
    private List<ReportFieldDTO> sampleFields = new ArrayList<>();

    public AvailableFieldsDTO() {
    }

    public AvailableFieldsDTO(List<ReportFieldDTO> patientFields, List<ReportFieldDTO> sampleFields) {
        this.patientFields = patientFields != null ? patientFields : new ArrayList<>();
        this.sampleFields = sampleFields != null ? sampleFields : new ArrayList<>();
    }

    public List<ReportFieldDTO> getPatientFields() {
        return patientFields;
    }

    public void setPatientFields(List<ReportFieldDTO> patientFields) {
        this.patientFields = patientFields != null ? patientFields : new ArrayList<>();
    }

    public List<ReportFieldDTO> getSampleFields() {
        return sampleFields;
    }

    public void setSampleFields(List<ReportFieldDTO> sampleFields) {
        this.sampleFields = sampleFields != null ? sampleFields : new ArrayList<>();
    }

    public List<ReportFieldDTO> getAllFields() {
        List<ReportFieldDTO> all = new ArrayList<>();
        all.addAll(patientFields);
        all.addAll(sampleFields);
        return all;
    }

    public ReportFieldDTO findFieldById(String fieldId) {
        return getAllFields().stream().filter(f -> f.getFieldId().equals(fieldId)).findFirst().orElse(null);
    }
}
