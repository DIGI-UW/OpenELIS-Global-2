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
package org.openelisglobal.reports.send.sample.valueholder;

import java.util.ArrayList;

public class SampleXmit extends org.openelisglobal.sample.valueholder.Sample {

    private PatientXmit patient;

    private String externalId;

    private ProviderXmit provider;

    private FacilityXmit facility;

    private ArrayList tests;

    public SampleXmit() {
    }

    public PatientXmit getPatient() {
        return patient;
    }

    public ArrayList getTests() {
        return tests;
    }

    public void setTests(ArrayList tests) {
        this.tests = tests;
    }

    public void setPatient(PatientXmit patient) {
        this.patient = patient;
    }

    public ProviderXmit getProvider() {
        return provider;
    }

    public void setProvider(ProviderXmit provider) {
        this.provider = provider;
    }

    public String getExternalId() {
        return externalId;
    }

    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }

    public FacilityXmit getFacility() {
        return facility;
    }

    public void setFacility(FacilityXmit facility) {
        this.facility = facility;
    }
}
