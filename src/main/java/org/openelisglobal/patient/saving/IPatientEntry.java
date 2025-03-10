package org.openelisglobal.patient.saving;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.patient.form.PatientEntryByProjectForm;

public interface IPatientEntry extends IAccessioner {

    void setRequest(HttpServletRequest request);

    void setFieldsFromForm(PatientEntryByProjectForm form) throws LIMSRuntimeException;

    void setSysUserId(String sysUserId);

    boolean canAccession();
}
