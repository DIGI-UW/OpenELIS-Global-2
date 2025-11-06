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

import java.util.Comparator;
import org.openelisglobal.common.util.StringUtil;

/**
 * @author pahill (pahill@uw.edu)
 * @since Jul 13, 2011
 */
public class FollowupRequiredData {
    private String orgname;
    private String subjectNumber;
    private String siteSubjectNumber;
    private String collectiondate;
    private String receivedDate;
    private String doctor;
    private String labNo;
    private String status;
    private String nonConformityNotes;
    private String underInvestigationNotes;

    public String getOrgname() {
        return orgname;
    }

    public void setOrgname(String orgname) {
        this.orgname = orgname;
    }

    public String getSubjectNumber() {
        return subjectNumber;
    }

    public void setSubjectNumber(String subjectNumber) {
        this.subjectNumber = subjectNumber;
    }

    public String getCollectiondate() {
        return collectiondate;
    }

    public void setCollectiondate(String collectiondate) {
        this.collectiondate = collectiondate;
    }

    public String getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(String receivedDate) {
        this.receivedDate = receivedDate;
    }

    public String getDoctor() {
        return doctor;
    }

    public void setDoctor(String doctor) {
        this.doctor = doctor;
    }

    public String getLabNo() {
        return labNo;
    }

    public void setLabNo(String labNo) {
        this.labNo = labNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setSiteSubjectNumber(String siteSubjectNumber) {
        this.siteSubjectNumber = siteSubjectNumber;
    }

    public String getSiteSubjectNumber() {
        return siteSubjectNumber;
    }

    public String getNonConformityNotes() {
        return nonConformityNotes;
    }

    public void setNonConformityNotes(String nonConformityNotes) {
        this.nonConformityNotes = nonConformityNotes;
    }

    public String getUnderInvestigationNotes() {
        return underInvestigationNotes;
    }

    public void setUnderInvestigationNotes(String underInvestigationNotes) {
        this.underInvestigationNotes = underInvestigationNotes;
    }

    public static class OrderByOrgName implements Comparator<FollowupRequiredData> {
        /**
         * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
         */
        @Override
        public int compare(FollowupRequiredData o1, FollowupRequiredData o2) {
            int orgCompare = StringUtil.compareWithNulls(o1.getOrgname(), o2.getOrgname());
            if (orgCompare != 0) {
                return orgCompare;
            }
            return StringUtil.compareWithNulls(o1.getLabNo(), o2.getLabNo());
        }
    }
}
