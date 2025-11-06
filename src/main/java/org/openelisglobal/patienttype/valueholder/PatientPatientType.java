package org.openelisglobal.patienttype.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class PatientPatientType extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;

    private String patientId;

    private String patientTypeId;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientTypeId() {
        return patientTypeId;
    }

    public void setPatientTypeId(String patientTypeId) {
        this.patientTypeId = patientTypeId;
    }
}
