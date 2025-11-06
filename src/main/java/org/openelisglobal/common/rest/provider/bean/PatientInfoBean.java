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
package org.openelisglobal.common.rest.provider.bean;

import java.io.Serializable;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.valueholder.PatientContact;

public class PatientInfoBean implements Serializable {
    private static final long serialVersionUID = 1L;
    private String currentDate;
    private String patientLastUpdated;
    private String personLastUpdated;
    private PatientUpdateStatus patientUpdateStatus;
    private String patientPK;
    private String STnumber;
    private String subjectNumber;
    private String nationalId;
    private String guid;
    private String lastName;
    private String firstName;
    private String aka;
    private String mothersName;
    private String mothersInitial;
    private String streetAddress;
    private String city;
    private String commune;
    private String addressDepartment;
    private String gender;
    private String birthDateForDisplay = "";
    private String insuranceNumber;
    private String occupation;
    private String primaryPhone;
    private String email;
    private String patientType = "";
    private String healthRegion;
    private String education;
    private String maritialStatus;
    private String nationality;
    private String healthDistrict;
    private String otherNationality;
    private PatientContact patientContact;
    private boolean readOnly = false;

    public void setPatientType(String patientType) {
        this.patientType = patientType;
    }

    public String getPatientType() {
        return patientType;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCurrentDate() {
        return currentDate;
    }

    public void setCurrentDate(String currentDate) {
        this.currentDate = currentDate;
    }

    public String getPatientLastUpdated() {
        return patientLastUpdated;
    }

    public void setPatientLastUpdated(String patientLastUpdated) {
        this.patientLastUpdated = patientLastUpdated;
    }

    public String getPersonLastUpdated() {
        return personLastUpdated;
    }

    public void setPersonLastUpdated(String personLastUpdated) {
        this.personLastUpdated = personLastUpdated;
    }

    public String getPatientPK() {
        return patientPK;
    }

    public void setPatientPK(String patientPK) {
        this.patientPK = patientPK;
    }

    public String getSTnumber() {
        return STnumber;
    }

    public void setSTnumber(String sTnumber) {
        STnumber = sTnumber;
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

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getAka() {
        return aka;
    }

    public void setAka(String aka) {
        this.aka = aka;
    }

    public String getMothersName() {
        return mothersName;
    }

    public void setMothersName(String mothersName) {
        this.mothersName = mothersName;
    }

    public String getMothersInitial() {
        return mothersInitial;
    }

    public void setMothersInitial(String mothersInitial) {
        this.mothersInitial = mothersInitial;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCommune() {
        return commune;
    }

    public void setCommune(String commune) {
        this.commune = commune;
    }

    public String getBirthDateForDisplay() {
        return birthDateForDisplay;
    }

    public void setBirthDateForDisplay(String birthDateForDisplay) {
        this.birthDateForDisplay = birthDateForDisplay;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getInsuranceNumber() {
        return insuranceNumber;
    }

    public void setInsuranceNumber(String insuranceNumber) {
        this.insuranceNumber = insuranceNumber;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public void setAddressDepartment(String addressDepartment) {
        this.addressDepartment = addressDepartment;
    }

    public String getAddressDepartment() {
        return addressDepartment;
    }

    public String getPrimaryPhone() {
        return primaryPhone;
    }

    public void setPrimaryPhone(String primaryPhone) {
        this.primaryPhone = primaryPhone;
    }

    public String getHealthRegion() {
        return healthRegion;
    }

    public void setHealthRegion(String healthRegion) {
        this.healthRegion = healthRegion;
    }

    public String getHealthDistrict() {
        return healthDistrict;
    }

    public void setHealthDistrict(String healthDistrict) {
        this.healthDistrict = healthDistrict;
    }

    public String getOtherNationality() {
        return otherNationality;
    }

    public void setOtherNationality(String otherNationality) {
        this.otherNationality = otherNationality;
    }

    public String getEducation() {
        return education;
    }

    public void setEducation(String education) {
        this.education = education;
    }

    public String getMaritialStatus() {
        return maritialStatus;
    }

    public void setMaritialStatus(String maritialStatus) {
        this.maritialStatus = maritialStatus;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String naionality) {
        nationality = naionality;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public PatientUpdateStatus getPatientUpdateStatus() {
        return patientUpdateStatus;
    }

    public void setPatientUpdateStatus(PatientUpdateStatus patientUpdateStatus) {
        this.patientUpdateStatus = patientUpdateStatus;
    }

    public PatientContact getPatientContact() {
        return patientContact;
    }

    public void setPatientContact(PatientContact patientContact) {
        this.patientContact = patientContact;
    }
}
