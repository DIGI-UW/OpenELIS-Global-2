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
package org.openelisglobal.result.action.util;

import java.util.List;
import java.util.Map;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.result.valueholder.Result;
import org.openelisglobal.result.valueholder.ResultInventory;
import org.openelisglobal.result.valueholder.ResultSignature;
import org.openelisglobal.sample.valueholder.Sample;

public class ResultSet {
    public final Result result;
    public final ResultSignature signature;
    public final ResultInventory testKit;
    public final Patient patient;
    public final Sample sample;
    public final Map<String, List<String>> triggersToSelectedReflexesMap;
    public final boolean alwaysInsertSignature;
    public final boolean multipleResultsForAnalysis;

    public ResultSet(Result result, ResultSignature signature, ResultInventory testKit, Patient patient, Sample sample,
            Map<String, List<String>> triggersToSelectedReflexesMap, boolean multipleResultsForAnalysis) {
        this.result = result;
        this.signature = signature;
        this.testKit = testKit;
        this.patient = patient;
        this.sample = sample;
        this.triggersToSelectedReflexesMap = triggersToSelectedReflexesMap;
        this.multipleResultsForAnalysis = multipleResultsForAnalysis;
        alwaysInsertSignature = signature != null && signature.getId() == null;
    }
}
