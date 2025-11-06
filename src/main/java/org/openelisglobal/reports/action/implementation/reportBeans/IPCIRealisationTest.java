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

/**
 * This file is the result of the Capstone project five for the Cote d'Ivoire OpenElis software
 * developer course made by Kone Constant
 */
package org.openelisglobal.reports.action.implementation.reportBeans;

public class IPCIRealisationTest {

    public String sectionName;
    public String testName;
    public int required;
    public int performed;
    public int noPerformed;

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public int getRequired() {
        return required;
    }

    public void setRequired(int required) {
        this.required = required;
    }

    public int getPerformed() {
        return performed;
    }

    public void setPerformed(int performed) {
        this.performed = performed;
    }

    public int getNoPerformed() {
        return noPerformed;
    }

    public void setNoPerformed(int noPerformed) {
        this.noPerformed = noPerformed;
    }
}
