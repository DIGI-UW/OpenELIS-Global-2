/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.reports.action.implementation.reportBeans;

/**
 * @author pahill (pahill@uw.edu)
 * @since Jun 15, 2011
 */
public class NonConformityReportData {
    private String accessionNumber;
    private String subjectNumber;
    private String siteSubjectNumber;
    private String study;
    private String service;
    private String receivedDate;
    private String receivedHour;
    private String nonConformityDate;
    private String section;
    private String reason;
    private String sampleType;
    private String biologist;
    private String qaNote;
    private String sampleNote;
    private String doctor;

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public void setSubjectNumber(String subjectNumber) {
        this.subjectNumber = subjectNumber;
    }

    public String getSubjectNumber() {
        return subjectNumber;
    }

    public void setSiteSubjectNumber(String siteSubjectNumber) {
        this.siteSubjectNumber = siteSubjectNumber;
    }

    public String getSiteSubjectNumber() {
        return siteSubjectNumber;
    }

    public String getStudy() {
        return study;
    }

    public void setStudy(String study) {
        this.study = study;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getService() {
        return service;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getNonConformityDate() {
        return nonConformityDate;
    }

    public void setNonConformityDate(String nonConformityDate) {
        this.nonConformityDate = nonConformityDate;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getNonConformityReason() {
        return reason;
    }

    public void setNonConformityReason(String reason) {
        this.reason = reason;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getBiologist() {
        return biologist;
    }

    public void setBiologist(String biologist) {
        this.biologist = biologist;
    }

    public void setQaNote(String qaNote) {
        this.qaNote = qaNote;
    }

    public String getQaNote() {
        return qaNote;
    }

    public void setSampleNote(String sampleNote) {
        this.sampleNote = sampleNote;
    }

    public String getSampleNote() {
        return sampleNote;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }

    public String getDoctor() {
        return doctor;
    }

    public void setReceivedHour(String receivedHour) {
        this.receivedHour = receivedHour;
    }

    public String getReceivedHour() {
        return receivedHour;
    }
}
