package org.openelisglobal.patient.saving;

import jakarta.servlet.http.HttpServletRequest;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.patient.saving.form.IAccessionerForm;

public interface ISampleEntry extends IAccessioner {

    void setFieldsFromForm(IAccessionerForm form) throws LIMSRuntimeException;

    void setSysUserId(String sysUserId);

    void setRequest(HttpServletRequest request);

    boolean canAccession();
}
