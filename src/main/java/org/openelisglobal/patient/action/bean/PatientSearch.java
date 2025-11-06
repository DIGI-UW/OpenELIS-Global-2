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

package org.openelisglobal.patient.action.bean;

import java.util.List;
import org.openelisglobal.common.services.DisplayListService;
import org.openelisglobal.common.util.IdValuePair;

/** */
public class PatientSearch {
    private boolean loadFromServerWithPatient = false;
    private boolean defaultHeader = true;
    private String selectedPatientActionButtonText;

    public boolean isLoadFromServerWithPatient() {
        return loadFromServerWithPatient;
    }

    public void setLoadFromServerWithPatient(boolean loadFromServerWithPatient) {
        this.loadFromServerWithPatient = loadFromServerWithPatient;
    }

    public List<IdValuePair> getSearchCriteria() {
        return DisplayListService.getInstance().getList(DisplayListService.ListType.PATIENT_SEARCH_CRITERIA);
    }

    public List<IdValuePair> getGenders() {
        return DisplayListService.getInstance().getList(DisplayListService.ListType.GENDERS);
    }

    public String getSelectedPatientActionButtonText() {
        return selectedPatientActionButtonText;
    }

    public void setSelectedPatientActionButtonText(String selectedPatientActionButtonText) {
        this.selectedPatientActionButtonText = selectedPatientActionButtonText;
    }

    public boolean isDefaultHeader() {
        return defaultHeader;
    }

    public void setDefaultHeader(boolean defaultHeader) {
        this.defaultHeader = defaultHeader;
    }
}
