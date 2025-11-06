package org.openelisglobal.patientidentity.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class PatientIdentity extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;
    private String identityTypeId;
    private String patientId;
    private String identityData;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentityTypeId() {
        return identityTypeId;
    }

    public void setIdentityTypeId(String identityTypeId) {
        this.identityTypeId = identityTypeId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getIdentityData() {
        return identityData;
    }

    public void setIdentityData(String identityData) {
        this.identityData = identityData;
    }
}
