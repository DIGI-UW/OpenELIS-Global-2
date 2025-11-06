/*
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations under
 * the License.
 *
 * The Original Code is OpenELIS code.
 *
 * Copyright (C) ITECH, University of Washington, Seattle WA.  All Rights Reserved.
 */

package org.openelisglobal.testconfiguration.beans;

import org.openelisglobal.common.util.validator.GenericValidator;

public class ResultLimitBean {
    private String gender;
    private String ageRange;
    private String normalRange;
    private String validRange;
    private String reportingRange;
    private String criticalRange;

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = GenericValidator.isBlankOrNull(gender) ? "n/a" : gender;
    }

    public String getAgeRange() {
        return ageRange;
    }

    public void setAgeRange(String ageRange) {
        this.ageRange = ageRange;
    }

    public String getNormalRange() {
        return normalRange;
    }

    public void setNormalRange(String normalRange) {
        this.normalRange = normalRange;
    }

    public String getValidRange() {
        return validRange;
    }

    public void setValidRange(String validRange) {
        this.validRange = validRange;
    }

    public String getReportingRange() {
        return reportingRange;
    }

    public void setReportingRange(String reportingRange) {
        this.reportingRange = reportingRange;
    }

    public String getCriticalRange() {
        return criticalRange;
    }

    public void setCriticalRange(String criticalRange) {
        this.criticalRange = criticalRange;
    }
}
