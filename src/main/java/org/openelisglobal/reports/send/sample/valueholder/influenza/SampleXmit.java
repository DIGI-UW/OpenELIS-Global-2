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

import java.util.ArrayList;
import org.openelisglobal.sample.valueholder.Sample;

/**
 * @author diane benz bugzilla 2393 bugzilla 2437
 */
public class SampleXmit extends org.openelisglobal.sample.valueholder.Sample {

    // bugzilla 2437 this is not used for xml but is simply information that is
    // useful
    private transient Sample sample;

    private PatientXmit patient;

    private String sourceOfSample;

    private String typeOfSampleCode;

    private String typeOfSample;

    private ProviderXmit provider;

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

    public String getSourceOfSample() {
        return sourceOfSample;
    }

    public void setSourceOfSample(String sourceOfSample) {
        this.sourceOfSample = sourceOfSample;
    }

    public String getTypeOfSample() {
        return typeOfSample;
    }

    public void setTypeOfSample(String typeOfSample) {
        this.typeOfSample = typeOfSample;
    }

    public String getTypeOfSampleCode() {
        return typeOfSampleCode;
    }

    public void setTypeOfSampleCode(String typeOfSampleCode) {
        this.typeOfSampleCode = typeOfSampleCode;
    }

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }
}
