package org.openelisglobal.result.controller.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.test.beanItems.TestResultItem;

/**
 * JSON payload for {@code GET /rest/accession-results}, aligned with legacy
 * {@link org.openelisglobal.result.controller.AccessionResultsController}
 * loading rules.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AccessionResultsRestResponse {

    private String accessionNumber;
    private Boolean searchFinished;
    private List<TestResultItem> testResult = new ArrayList<>();
    /**
     * Non-null when the accession could not be resolved (mirrors legacy validation
     * errors).
     */
    private String error;

    private String firstName;
    private String lastName;
    private String dob;
    private String gender;
    private String st;
    private String subjectNumber;
    private String nationalId;

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public Boolean getSearchFinished() {
        return searchFinished;
    }

    public void setSearchFinished(Boolean searchFinished) {
        this.searchFinished = searchFinished;
    }

    public List<TestResultItem> getTestResult() {
        return testResult;
    }

    public void setTestResult(List<TestResultItem> testResult) {
        this.testResult = testResult;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
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

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getSt() {
        return st;
    }

    public void setSt(String st) {
        this.st = st;
    }

    public String getSubjectNumber() {
        return subjectNumber;
    }

    public void setSubjectNumber(String subjectNumber) {
        this.subjectNumber = subjectNumber;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }
}
