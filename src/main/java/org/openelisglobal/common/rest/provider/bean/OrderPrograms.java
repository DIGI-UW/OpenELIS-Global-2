package org.openelisglobal.common.rest.provider.bean;

import java.sql.Date;
import java.util.UUID;

public class OrderPrograms {
    private String programSampleId;
    private String firstName;
    private String lastName;
    private String gender;
    private String patientPK;
    private String programName;
    private String programCode;
    private Date receivedDate;
    private String accessionNumber;
    private UUID questionnaireResponseUuid;

    public String getProgramSampleId() {
        return programSampleId;
    }

    public void setProgramSampleId(String programSampleId) {
        this.programSampleId = programSampleId;
    }

    public String getProgramName() {
        return programName;
    }

    public void setProgramName(String programName) {
        this.programName = programName;
    }

    public String getProgramCode() {
        return programCode;
    }

    public void setProgramCode(String programCode) {
        this.programCode = programCode;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public UUID getQuestionnaireResponseUuid() {
        return questionnaireResponseUuid;
    }

    public void setQuestionnaireResponseUuid(UUID questionnaireResponseUuid) {
        this.questionnaireResponseUuid = questionnaireResponseUuid;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getPatientPK() {
        return patientPK;
    }

    public void setPatientPK(String patientPK) {
        this.patientPK = patientPK;
    }

}
