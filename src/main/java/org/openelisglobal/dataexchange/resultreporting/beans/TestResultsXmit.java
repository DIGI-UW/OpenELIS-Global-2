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
package org.openelisglobal.dataexchange.resultreporting.beans;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.openelisglobal.dataexchange.orderresult.OrderResponseWorker.Event;

public class TestResultsXmit {

    /** A String representing the patientId whom the analysis has been done for. */
    private String patientGUID;

    private CodedValueXmit test;

    private CodedValueXmit sampleType;

    private TestRangeXmit normalRange;

    private String status;

    private String unitOfMeasure;

    public CodedValueXmit getTest() {
        return test;
    }

    public void setTest(CodedValueXmit test) {
        this.test = test;
    }

    public CodedValueXmit getSampleType() {
        return sampleType;
    }

    public void setSampleType(CodedValueXmit sampleType) {
        this.sampleType = sampleType;
    }

    /** The date representing when the test has been done */
    private Date testDate;

    private String accessionNumber;

    private String referringOrderNumber;

    /** A List representing all results for the given test and analysis. */
    private List<ResultXmit> results;

    public TestResultsXmit() {
        results = new ArrayList<ResultXmit>();
    }

    /**
     * @return the results
     */
    public List<ResultXmit> getResults() {
        return results;
    }

    /**
     * @param results the results to set
     */
    public void setResults(List<ResultXmit> results) {
        this.results = results;
    }

    public void setPatientGUID(String patientGUID) {
        this.patientGUID = patientGUID;
    }

    public String getPatientGUID() {
        return patientGUID;
    }

    public void setTestDate(Date testDate) {
        this.testDate = testDate;
    }

    public Date getTestDate() {
        return testDate;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getReferringOrderNumber() {
        return referringOrderNumber;
    }

    public void setReferringOrderNumber(String referringOrderNumber) {
        this.referringOrderNumber = referringOrderNumber;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setNormalRange(TestRangeXmit normalRange) {
        this.normalRange = normalRange;
    }

    public TestRangeXmit getNormalRange() {
        return normalRange;
    }

    /** The section for the test */
    private String testSection;

    public void setTestSection(String section) {
        this.testSection = section;
    }

    public String getTestSection() {
        return testSection;
    }

    /** Valid range min/max */
    private TestRangeXmit validRange;

    public void setValidRange(TestRangeXmit validRange) {
        this.validRange = validRange;
    }

    public TestRangeXmit getValidRange() {
        return validRange;
    }

    /** The notes for the test, concatenated into one string */
    private String testNotes;

    public void setTestNotes(String notes) {
        this.testNotes = notes;
    }

    public String getTestNotes() {
        return testNotes;
    }

    /** Following elements are for malaria case reports only */
    private String patientFirstName;

    public void setPatientFirstName(String fname) {
        this.patientFirstName = fname;
    }

    public String getPatientFirstName() {
        return patientFirstName;
    }

    private String patientLastName;

    public void setPatientLastName(String lname) {
        this.patientLastName = lname;
    }

    public String getPatientLastName() {
        return patientLastName;
    }

    private String patientSTID;

    public void setPatientSTID(String patientSTID) {
        this.patientSTID = patientSTID;
    }

    public String getPatientSTID() {
        return patientSTID;
    }

    private String patientGender;

    public void setPatientGender(String gender) {
        this.patientGender = gender;
    }

    public String getPatientGender() {
        return patientGender;
    }

    private String patientStreetAddress;

    public void setPatientStreetAddress(String addr) {
        this.patientStreetAddress = addr;
    }

    public String getPatientStreetAddress() {
        return patientStreetAddress;
    }

    private String patientCity;

    public void setPatientCity(String city) {
        this.patientCity = city;
    }

    public String getPatientCity() {
        return patientCity;
    }

    private String patientState;

    public void setPatientState(String state) {
        this.patientState = state;
    }

    public String getPatientState() {
        return patientState;
    }

    private String patientZipCode;

    public void setPatientZipCode(String zip) {
        this.patientZipCode = zip;
    }

    public String getPatientZipCode() {
        return patientZipCode;
    }

    private String patientCountry;

    public void setPatientCountry(String country) {
        this.patientCountry = country;
    }

    public String getPatientCountry() {
        return patientCountry;
    }

    private String patientBirthdate;

    public void setPatientBirthdate(String birthdate) {
        this.patientBirthdate = birthdate;
    }

    public String getPatientBirthdate() {
        return patientBirthdate;
    }

    private String patientTelephone;

    public void setPatientTelephone(String telephone) {
        this.patientTelephone = telephone;
    }

    public String getatientTelephone() {
        return patientTelephone;
    }

    /** End malaria case report elements */
    private Event resultsEvent;

    public void setResultsEvent(Event resultsEvent) {
        this.resultsEvent = resultsEvent;
    }

    public Event getResultsEvent() {
        return resultsEvent;
    }

    public String getUnits() {
        return unitOfMeasure;
    }

    public void setUnits(String unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
}
