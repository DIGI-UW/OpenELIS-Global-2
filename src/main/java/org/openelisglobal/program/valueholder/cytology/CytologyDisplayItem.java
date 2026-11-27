package org.openelisglobal.program.valueholder.cytology;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import org.openelisglobal.program.valueholder.cytology.CytologySample.CytologyStatus;

public class CytologyDisplayItem {
    /**
     * The day the examination was asked for, held without a zone so that rendering
     * it cannot move it onto a neighbouring day.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate requestDate;

    private CytologyStatus status;
    private String lastName;
    private String firstName;
    private String assignedTechnician;
    private String assignedCytoPathologist;
    private String labNumber;

    private Integer pathologySampleId;

    private String patientPK;

    public LocalDate getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDate requestDate) {
        this.requestDate = requestDate;
    }

    public CytologyStatus getStatus() {
        return status;
    }

    public void setStatus(CytologyStatus status) {
        this.status = status;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getAssignedTechnician() {
        return assignedTechnician;
    }

    public void setAssignedTechnician(String assignedTechnician) {
        this.assignedTechnician = assignedTechnician;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public Integer getPathologySampleId() {
        return pathologySampleId;
    }

    public void setPathologySampleId(Integer pathologySampleId) {
        this.pathologySampleId = pathologySampleId;
    }

    public String getAssignedCytoPathologist() {
        return assignedCytoPathologist;
    }

    public void setAssignedCytoPathologist(String assignedCytoPathologist) {
        this.assignedCytoPathologist = assignedCytoPathologist;
    }

    public String getPatientPK() {
        return patientPK;
    }

    public void setPatientPK(String patientPK) {
        this.patientPK = patientPK;
    }
}
