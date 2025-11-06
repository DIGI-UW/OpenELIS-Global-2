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
 * <p>Copyright (C) ITECH, University of Washington, Seattle WA. All Rights Reserved.
 */
package org.openelisglobal.testreflex.action.util;

import java.util.List;
import java.util.Map;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.sample.valueholder.Sample;

public class TestReflexBean {
    private Sample sample;
    private Result result;
    private Patient patient;
    private Map<String, List<String>> triggersToSelectedReflexesMap;

    public Sample getSample() {
        return sample;
    }

    public void setSample(Sample sample) {
        this.sample = sample;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Map<String, List<String>> getTriggersToSelectedReflexesMap() {
        return triggersToSelectedReflexesMap;
    }

    public void setTriggersToSelectedReflexesMap(Map<String, List<String>> triggersToSelectedReflexesMap) {
        this.triggersToSelectedReflexesMap = triggersToSelectedReflexesMap;
    }
}
