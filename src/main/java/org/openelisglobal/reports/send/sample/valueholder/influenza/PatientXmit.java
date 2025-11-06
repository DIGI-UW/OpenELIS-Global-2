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
package org.openelisglobal.reports.send.sample.valueholder.influenza;

import org.openelisglobal.common.util.StringUtil;

/**
 * @author diane benz bugzilla 2393
 */
public class PatientXmit extends org.openelisglobal.patient.valueholder.Patient {

    private String lastName;

    private String firstName;

    private String middleName;

    private String streetAddress;

    private String multipleUnit;

    private String city;

    private String state;

    private String zipCode;

    private String county;

    public PatientXmit() {
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getCity() {
        if (!StringUtil.isNullorNill(city)) {
            return city.trim();
        }
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        if (!StringUtil.isNullorNill(state)) {
            return state.trim();
        }
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

    public String getZipCode() {
        if (!StringUtil.isNullorNill(zipCode)) {
            return zipCode.trim();
        }
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
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
}
