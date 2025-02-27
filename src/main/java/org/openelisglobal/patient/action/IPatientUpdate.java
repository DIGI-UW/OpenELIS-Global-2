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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.patient.action;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.springframework.validation.Errors;

public interface IPatientUpdate {
    public static enum PatientUpdateStatus {
        NO_ACTION, UPDATE, ADD
    }

    public abstract Errors preparePatientData(HttpServletRequest request, PatientManagementInfo patientInfo)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException;

    public abstract void setPatientUpdateStatus(PatientManagementInfo patientInfo);

    public abstract PatientUpdateStatus getPatientUpdateStatus();

    public abstract void persistPatientData(PatientManagementInfo patientInfo) throws LIMSRuntimeException;

    public abstract String getPatientId(SamplePatientEntryForm form);
}
