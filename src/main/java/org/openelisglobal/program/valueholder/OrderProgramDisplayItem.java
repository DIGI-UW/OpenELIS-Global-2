package org.openelisglobal.program.valueholder;

import java.util.Date;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

public class OrderProgramDisplayItem {

    private Integer programSampleId;
    private String programName;
    private String programCode;

    private String accessionNumber;
    private Date receivedDate;

    private String firstName;
    private String lastName;
    private String age;
    private String gender;
    private String patientPK;
    private String referringFacility;
    private String department;
    private String requester;

    private Questionnaire programQuestionnaire;
    private QuestionnaireResponse programQuestionnaireResponse;
    private String questionnaireStatus;

    public Integer getProgramSampleId() {
        return programSampleId;
    }

    public void setProgramSampleId(Integer programSampleId) {
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

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
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

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Questionnaire getProgramQuestionnaire() {
        return programQuestionnaire;
    }

    public void setProgramQuestionnaire(Questionnaire programQuestionnaire) {
        this.programQuestionnaire = programQuestionnaire;
    }

    public QuestionnaireResponse getProgramQuestionnaireResponse() {
        return programQuestionnaireResponse;
    }

    public void setProgramQuestionnaireResponse(QuestionnaireResponse programQuestionnaireResponse) {
        this.programQuestionnaireResponse = programQuestionnaireResponse;
    }

    public String getQuestionnaireStatus() {
        return questionnaireStatus;
    }

    public void setQuestionnaireStatus(String questionnaireStatus) {
        this.questionnaireStatus = questionnaireStatus;
    }

    public String getPatientPK() {
        return patientPK;
    }

    public void setPatientPK(String patientPK) {
        this.patientPK = patientPK;
    }

    public String getReferringFacility() {
        return referringFacility;
    }

    public void setReferringFacility(String referringFacility) {
        this.referringFacility = referringFacility;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

}
