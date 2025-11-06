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
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.reports.valueholder.resultsreport;

import java.util.List;
import org.openelisglobal.reports.valueholder.common.JRHibernateDataSource;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;

/**
 * @author benzd1 bugzilla 2264
 */
/**
 * @author benzd1
 */
public class ResultsReportSample {

    private String accessionNumber;

    private Sample sample;

    private SampleItem sampleItem;

    private String organizationId;

    private String organizationName;

    private String organizationStreetAddress;

    private String organizationCityStateZip;

    private String sampleCollectionDate;

    private String sampleReceivedDate;

    private String sampleClientReferenceNumber;

    private String sampleHasTestRevisions;

    // bugzilla 1900
    private String sampleIsForPreview;

    private String clinicianName;

    private JRHibernateDataSource resultsReportTests;

    private List tests;

    private JRHibernateDataSource resultsReportProjects;

    // patient stuff
    private String patientName;
    private String patientStreetAddress;
    // bugzilla 1852
    private String patientCity;
    private String patientState;
    private String patientZip;
    private String patientCountry;

    private String patientExternalId;
    private String patientGender;
    private String patientDateOfBirth;

    // source type, source of sample
    private String typeOfSample;
    private String sourceOfSample;

    public List getTests() {
        return tests;
    }

    public void setTests(List tests) {
        this.tests = tests;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public SampleItem getSampleItem() {
        return sampleItem;
    }

    public void setSampleItem(SampleItem sampleItem) {
        this.sampleItem = sampleItem;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getOrganizationStreetAddress() {
        return organizationStreetAddress;
    }

    public void setOrganizationStreetAddress(String organizationStreetAddress) {
        this.organizationStreetAddress = organizationStreetAddress;
    }

    public String getOrganizationCityStateZip() {
        return organizationCityStateZip;
    }

    public void setOrganizationCityStateZip(String organizationCityStateZip) {
        this.organizationCityStateZip = organizationCityStateZip;
    }

    public String getSampleCollectionDate() {
        return sampleCollectionDate;
    }

    public void setSampleCollectionDate(String sampleCollectionDate) {
        this.sampleCollectionDate = sampleCollectionDate;
    }

    public String getSampleReceivedDate() {
        return sampleReceivedDate;
    }

    public void setSampleReceivedDate(String sampleReceivedDate) {
        this.sampleReceivedDate = sampleReceivedDate;
    }

    public String getSampleClientReferenceNumber() {
        return sampleClientReferenceNumber;
    }

    public void setSampleClientReferenceNumber(String sampleClientReferenceNumber) {
        this.sampleClientReferenceNumber = sampleClientReferenceNumber;
    }

    public String getSampleHasTestRevisions() {
        return sampleHasTestRevisions;
    }

    public void setSampleHasTestRevisions(String sampleHasTestRevisions) {
        this.sampleHasTestRevisions = sampleHasTestRevisions;
    }

    public String getClinicianName() {
        return clinicianName;
    }

    public void setClinicianName(String clinicianName) {
        this.clinicianName = clinicianName;
    }

    public JRHibernateDataSource getResultsReportProjects() {
        return resultsReportProjects;
    }

    public void setResultsReportProjects(JRHibernateDataSource resultsReportProjects) {
        this.resultsReportProjects = resultsReportProjects;
    }

    public JRHibernateDataSource getResultsReportTests() {
        return resultsReportTests;
    }

    public void setResultsReportTests(JRHibernateDataSource resultsReportTests) {
        this.resultsReportTests = resultsReportTests;
    }

    public String getPatientCity() {
        return patientCity;
    }

    public void setPatientCity(String patientCity) {
        this.patientCity = patientCity;
    }

    public String getPatientCountry() {
        return patientCountry;
    }

    public void setPatientCountry(String patientCountry) {
        this.patientCountry = patientCountry;
    }

    public String getPatientState() {
        return patientState;
    }

    public void setPatientState(String patientState) {
        this.patientState = patientState;
    }

    public String getPatientZip() {
        return patientZip;
    }

    public void setPatientZip(String patientZip) {
        this.patientZip = patientZip;
    }

    public String getPatientDateOfBirth() {
        return patientDateOfBirth;
    }

    public void setPatientDateOfBirth(String patientDateOfBirth) {
        this.patientDateOfBirth = patientDateOfBirth;
    }

    public String getPatientExternalId() {
        return patientExternalId;
    }

    public void setPatientExternalId(String patientExternalId) {
        this.patientExternalId = patientExternalId;
    }

    public String getPatientGender() {
        return patientGender;
    }

    public void setPatientGender(String patientGender) {
        this.patientGender = patientGender;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientStreetAddress() {
        return patientStreetAddress;
    }

    public void setPatientStreetAddress(String patientStreetAddress) {
        this.patientStreetAddress = patientStreetAddress;
    }

    public String getSourceOfSample() {
        return sourceOfSample;
    }

    public void setSourceOfSample(String sourceOfSample) {
        this.sourceOfSample = sourceOfSample;
    }

    public String getTypeOfSample() {
        return typeOfSample;
    }

    public void setTypeOfSample(String typeOfSample) {
        this.typeOfSample = typeOfSample;
    }

    public String getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(String organizationId) {
        this.organizationId = organizationId;
    }

    public String getSampleIsForPreview() {
        return sampleIsForPreview;
    }

    public void setSampleIsForPreview(String sampleIsForPreview) {
        this.sampleIsForPreview = sampleIsForPreview;
    }
}
