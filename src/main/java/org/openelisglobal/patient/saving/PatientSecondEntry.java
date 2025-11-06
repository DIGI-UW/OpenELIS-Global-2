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
package org.openelisglobal.patient.saving;

import static org.openelisglobal.common.services.StatusService.RecordStatus.InitialRegistration;
import static org.openelisglobal.common.services.StatusService.RecordStatus.ValidationRegistration;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import org.openelisglobal.common.action.IActionConstants;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.form.PatientEntryByProjectForm;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author Paul A. Hill (pahill@uw.edu)
 * @since Jul 6, 2010
 */
@Service
@Scope("prototype")
public class PatientSecondEntry extends PatientEntry implements IPatientSecondEntry, IActionConstants {

    /**
     * @param form
     * @param sysUserId
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws LIMSRuntimeException
     */
    public PatientSecondEntry(PatientEntryByProjectForm form, String sysUserId, HttpServletRequest request) {
        this();
        super.setFieldsFromForm(form);
        super.setSysUserId(sysUserId);
        super.setRequest(request);
    }

    public PatientSecondEntry() {
        super();
        newPatientStatus = ValidationRegistration;
        newSampleStatus = null;
    }

    /**
     * @see org.openelisglobal.patient.saving.PatientEntry#canAccession()
     */
    @Override
    public boolean canAccession() {
        return projectFormMapper.isSecondEntry(request) && InitialRegistration == statusSet.getPatientRecordStatus();
    }

    @Override
    protected String getActionLabel() {
        return MessageUtil.getMessage("banner.menu.createPatient.Verify");
    }
}
