/*
cd /ucd u	* The contents of this file are subject to the Mozilla Public License
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

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.internationalization.MessageUtil;
import org.openelisglobal.patient.form.PatientEntryByProjectForm;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

/**
 * @author Paul A. Hill (pahill@uw.edu)
 * @since Jul 29, 2010
 */
@Service
@Scope("prototype")
public class PatientEditUpdate extends PatientEntry implements IPatientEditUpdate {
    /**
     * @param form
     * @param sysUserId
     * @param request
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws LIMSRuntimeException
     */
    public PatientEditUpdate(PatientEntryByProjectForm form, String sysUserId, HttpServletRequest request) {
        this();
        super.setFieldsFromForm(form);
        super.setSysUserId(sysUserId);
        super.setRequest(request);
        // we are not updating the record status in either case
        newPatientStatus = null;
        newSampleStatus = null;
    }

    public PatientEditUpdate() {
        super();
        // we are not updating the record status in either case
        newPatientStatus = null;
        newSampleStatus = null;
    }

    /**
     * @see org.openelisglobal.patient.saving.PatientEntry#canAccession()
     */
    @Override
    public boolean canAccession() {
        String type = request.getParameter("type");
        if (RequestType.valueOfAsUpperCase(type) == RequestType.READWRITE) {
            return true;
        }
        return false;
    }

    @Override
    protected String getActionLabel() {
        return MessageUtil.getMessage("banner.menu.editPatient.ReadWrite");
    }
}
