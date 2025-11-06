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

import java.util.Date;
import java.util.List;

public class ResultReportXmit {
    public String version = "1";

    private Date transmissionDate;
    private String sendingSiteId;
    private String sendingSiteName;

    private List<TestResultsXmit> testResults;

    public Date getTransmissionDate() {
        return transmissionDate;
    }

    public void setTransmissionDate(Date transmissionDate) {
        this.transmissionDate = transmissionDate;
    }

    public List<TestResultsXmit> getTestResults() {
        return testResults;
    }

    public void setTestResults(List<TestResultsXmit> testResults) {
        this.testResults = testResults;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setSendingSiteId(String sendingSiteId) {
        this.sendingSiteId = sendingSiteId;
    }

    public String getSendingSiteId() {
        return sendingSiteId;
    }

    public String getSendingSiteName() {
        return sendingSiteName;
    }

    public void setSendingSiteName(String sendingSiteName) {
        this.sendingSiteName = sendingSiteName;
    }

    /** Following elements are for malaria case reports only */
    private String sendingSiteLanguage;

    public void setSendingSiteLanguage(String lang) {
        this.sendingSiteLanguage = lang;
    }

    public String getSendingSiteLanguage() {
        return sendingSiteLanguage;
    }
    /** End malaria case report elements */
}
