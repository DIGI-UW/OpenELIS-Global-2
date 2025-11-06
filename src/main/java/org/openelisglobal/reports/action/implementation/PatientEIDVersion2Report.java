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
package org.openelisglobal.reports.action.implementation;

public class PatientEIDVersion2Report extends PatientEIDReport implements IReportCreator {
    @Override
    protected String reportFileName() {
        return "RetroCI_Patient_EID_Version2";
    }

    @Override
    protected void createReportParameters() {
        super.createReportParameters();
        reportParameters.put("contact",
                "CHU de Treichville, 01 BP 1712 Tel : 21-21-42-50/21-25-4189 Fax : 21-24-29-69/" + " 21-25-10-63");
    }
}
