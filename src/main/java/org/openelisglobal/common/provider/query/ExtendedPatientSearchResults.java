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

import org.apache.commons.lang3.StringUtils;

public class ExtendedPatientSearchResults extends PatientSearchResults {

    private String streetName;

    private String flatNumberApartmentName;

    private String postalCode;

    private String campCommune;

    private String town;

    private String county;

    public String getStreetName() {
        return streetName;
    }

    public void setStreetName(String streetName) {
        this.streetName = streetName;
    }

    public String getStreetAddress() {
        return StringUtils.trimToNull(
                (StringUtils.defaultString(flatNumberApartmentName) + " " + StringUtils.defaultString(streetName)));
    }

    public String getFlatNumberApartmentName() {
        return flatNumberApartmentName;
    }

    public void setFlatNumberApartmentName(String flatNumberApartmentName) {
        this.flatNumberApartmentName = flatNumberApartmentName;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCampCommune() {
        return campCommune;
    }

    public void setCampCommune(String campCommune) {
        this.campCommune = campCommune;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }
}
