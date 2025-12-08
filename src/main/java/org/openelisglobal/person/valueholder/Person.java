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
package org.openelisglobal.person.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.AssociationInverseSide;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.ObjectPath;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.PropertyValue;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.common.valueholder.BaseObject;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

public class Person extends BaseObject<String> {

    @Pattern(regexp = ValidationHelper.ID_REGEX)
    private String id;

    @ValidName(nameType = NameType.LAST_NAME)
    @KeywordField(normalizer = "lowercase")
    private String lastName;

    @ValidName(nameType = NameType.FIRST_NAME)
    @KeywordField(normalizer = "lowercase")
    private String firstName;

    private String middleName;

    private String multipleUnit;

    private String streetAddress;

    private String city;

    private String state;

    private String zipCode;

    private String country;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX)
    private String workPhone;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX)
    private String homePhone;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX)
    private String cellPhone;

    @Pattern(regexp = ValidationHelper.PHONE_REGEX)
    private String primaryPhone;

    private String fax;
    @Email
    private String email;

    @JsonIgnore
    @AssociationInverseSide(inversePath = @ObjectPath(@PropertyValue(propertyName = "person")))
    private Set<Patient> patients = new HashSet<>(0);

    public Person() {
        super();
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    public String getCellPhone() {
        return cellPhone;
    }

    public void setCellPhone(String cellPhone) {
        this.cellPhone = cellPhone;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getHomePhone() {
        return homePhone;
    }

    public void setHomePhone(String homePhone) {
        this.homePhone = homePhone;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public String getMultipleUnit() {
        return multipleUnit;
    }

    public void setMultipleUnit(String multipleUnit) {
        this.multipleUnit = multipleUnit;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getWorkPhone() {
        return workPhone;
    }

    public void setWorkPhone(String workPhone) {
        this.workPhone = workPhone;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public Set getPatients() {
        return this.patients;
    }

    public void setPatients(Set patients) {
        this.patients = patients;
    }

    public void addPatient(Patient patient) {
        patients.add(patient);
        patient.setPerson(this);
    }

    public String getPrimaryPhone() {
        return primaryPhone;
    }

    public void setPrimaryPhone(String primaryPhone) {
        this.primaryPhone = primaryPhone;
    }
}
