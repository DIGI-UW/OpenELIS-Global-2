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
package org.openelisglobal.reports.action.implementation.reportBeans;

import java.util.List;

public class ConfirmationData {

    private String organizationName;
    private String requesterAccession;
    private String labAccession;
    private String sampleType;
    private List<String> requesterTest;
    private List<String> requesterResult;
    private List<String> labTest;
    private List<String> labResult;
    private List<String> compleationDate;
    private String requesterName;
    private String requesterPhone;
    private String requesterFax;
    private String requesterEMail;
    private String note;
    private String receptionDate;

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getRequesterAccession() {
        return requesterAccession;
    }

    public void setRequesterAccession(String requesterAccession) {
        this.requesterAccession = requesterAccession;
    }

    public String getLabAccession() {
        return labAccession;
    }

    public void setLabAccession(String labAccession) {
        this.labAccession = labAccession;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public List<String> getRequesterTest() {
        return requesterTest;
    }

    public void setRequesterTest(List<String> requesterTest) {
        this.requesterTest = requesterTest;
    }

    public List<String> getRequesterResult() {
        return requesterResult;
    }

    public void setRequesterResult(List<String> requesterResult) {
        this.requesterResult = requesterResult;
    }

    public List<String> getLabTest() {
        return labTest;
    }

    public void setLabTest(List<String> labTest) {
        this.labTest = labTest;
    }

    public List<String> getLabResult() {
        return labResult;
    }

    public void setLabResult(List<String> labResult) {
        this.labResult = labResult;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public String getRequesterPhone() {
        return requesterPhone;
    }

    public void setRequesterPhone(String requesterPhone) {
        this.requesterPhone = requesterPhone;
    }

    public String getRequesterFax() {
        return requesterFax;
    }

    public void setRequesterFax(String requesterFax) {
        this.requesterFax = requesterFax;
    }

    public String getRequesterEMail() {
        return requesterEMail;
    }

    public void setRequesterEMail(String requesterEMail) {
        this.requesterEMail = requesterEMail;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Integer getMaxRequesterIndex() {
        return requesterTest == null ? 0 : requesterTest.size();
    }

    public void setReceptionDate(String receptionDate) {
        this.receptionDate = receptionDate;
    }

    public String getReceptionDate() {
        return receptionDate;
    }

    public void setCompleationDate(List<String> compleationDate) {
        this.compleationDate = compleationDate;
    }

    public List<String> getCompleationDate() {
        return compleationDate;
    }
}
