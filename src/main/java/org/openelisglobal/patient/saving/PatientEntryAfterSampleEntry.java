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
 * Copyright (C) The Minnesota Department of Health.  All Rights Reserved.
 *
 * Contributor(s): CIRG, University of Washington, Seattle WA.
 */

/**
 * Cote d'Ivoire
 *
 * @author pahill
 * @since 2010-06-15
 */
package org.openelisglobal.patient.saving;

import static org.openelisglobal.common.services.StatusService.RecordStatus.NotRegistered;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.services.StatusService.RecordStatus;
import org.openelisglobal.patient.form.PatientEntryByProjectForm;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
public class PatientEntryAfterSampleEntry extends PatientEntry implements IPatientEntryAfterSampleEntry {

    public PatientEntryAfterSampleEntry(PatientEntryByProjectForm form, String sysUserId, HttpServletRequest request) {
        this();
        super.setFieldsFromForm(form);
        super.setSysUserId(sysUserId);
        super.setRequest(request);
    }

    public PatientEntryAfterSampleEntry() {
        newPatientStatus = RecordStatus.InitialRegistration;
        newSampleStatus = null; // leave it be
    }

    /** An existing not registered patient with the sample already somewhere else */
    @Override
    public boolean canAccession() {
        return (NotRegistered == statusSet.getPatientRecordStatus()
                && NotRegistered != statusSet.getSampleRecordStatus());
    }
}
