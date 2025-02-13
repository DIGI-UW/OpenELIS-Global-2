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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.common.provider.query;

import java.math.BigDecimal;
import org.openelisglobal.common.util.ConfigurationProperties;
import org.openelisglobal.common.util.ConfigurationProperties.Property;
import org.openelisglobal.common.util.DateUtil;

public class PatientSearchResults {

    private String id;
    private String GUID;
    private String firstName;
    private String lastName;
    private String gender;
    private String birthdate;
    private String nationalId;
    private String externalId;
    private String stNumber;
    private String referringSitePatientId;
    private String subjectNumber;
    private String mothersName;
    private String dataSourceName;
    private String dataSourceId;
    private String contactName;
    private String contactEmail;
    private String contactPhone;

    public PatientSearchResults() {
    }

    public PatientSearchResults(BigDecimal id, String first, String last, String gender, String dob, String nationalId,
            String externalId, String stNumber, String subjectNumber, String guid, String referringSitePatientId) {
        this.id = id.toPlainString();
        firstName = first;
        lastName = last;
        this.gender = gender;
        birthdate = dob;
        this.nationalId = nationalId;
        this.stNumber = stNumber;
        this.subjectNumber = subjectNumber;
        this.GUID = guid;
        this.externalId = externalId;
        this.referringSitePatientId = referringSitePatientId;
    }

    public String getPatientID() {
        return id;
    }

    public void setPatientID(String id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getGender() {
        return gender;
    }

    public String getDOB() {
        return getFormatedBirthDate();
    }

    public String getNationalId() {
        return nationalId;
    }

    public String getSTNumber() {
        return stNumber;
    }

    public String getBirthdate() {
        return getFormatedBirthDate();
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getStNumber() {
        return stNumber;
    }

    public void setStNumber(String stNumber) {
        this.stNumber = stNumber;
    }

    public String getSubjectNumber() {
        return subjectNumber;
    }

    public void setSubjectNumber(String subjectNumber) {
        this.subjectNumber = subjectNumber;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
    }

    public void setMothersName(String mothersName) {
        this.mothersName = mothersName;
    }

    public String getMothersName() {
        return mothersName;
    }

    public String getDataSourceName() {
        return dataSourceName;
    }

    public void setDataSourceName(String dataSourceName) {
        this.dataSourceName = dataSourceName;
    }

    public String getDataSourceId() {
        return dataSourceId;
    }

    public void setDataSourceId(String dataSourceId) {
        this.dataSourceId = dataSourceId;
    }

    public void setGUID(String gUID) {
        GUID = gUID;
    }

    public String getGUID() {
        return GUID;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public String getExternalId() {
        return externalId;
    }

    public String getReferringSitePatientId() {
        return referringSitePatientId;
    }

    public void setReferringSitePatientId(String referringSitePatientId) {
        this.referringSitePatientId = referringSitePatientId;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getFormatedBirthDate() {
        String format1 = "dd/MM/yyyy";
        String format2 = "MM/dd/yyyy";
        birthdate = ConfigurationProperties.getInstance().getPropertyValue(Property.DEFAULT_DATE_LOCALE).equals("fr-FR")
                ? DateUtil.formatStringDate(birthdate, format1)
                : DateUtil.formatStringDate(birthdate, format2);

        return birthdate;
    }
}
