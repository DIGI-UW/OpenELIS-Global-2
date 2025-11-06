package org.openelisglobal.program.valueholder.immunohistochemistry;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Date;
import org.openelisglobal.program.valueholder.immunohistochemistry.ImmunohistochemistrySample.ImmunohistochemistryStatus;

public class ImmunohistochemistryDisplayItem {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date requestDate;

    private ImmunohistochemistryStatus status;
    private String lastName;
    private String firstName;
    private String assignedTechnician;
    private String assignedPathologist;
    private String labNumber;

    private Integer immunohistochemistrySampleId;

    public Date getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(Date requestDate) {
        this.requestDate = requestDate;
    }

    public ImmunohistochemistryStatus getStatus() {
        return status;
    }

    public void setStatus(ImmunohistochemistryStatus status) {
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

    public String getAssignedPathologist() {
        return assignedPathologist;
    }

    public void setAssignedPathologist(String assignedPathologist) {
        this.assignedPathologist = assignedPathologist;
    }

    public String getLabNumber() {
        return labNumber;
    }

    public void setLabNumber(String labNumber) {
        this.labNumber = labNumber;
    }

    public Integer getImmunohistochemistrySampleId() {
        return immunohistochemistrySampleId;
    }

    public void setImmunohistochemistrySampleId(Integer immunohistochemistrySampleId) {
        this.immunohistochemistrySampleId = immunohistochemistrySampleId;
    }
}
