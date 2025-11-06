package org.openelisglobal.patientidentitytype.valueholder;

import org.openelisglobal.common.valueholder.BaseObject;

public class PatientIdentityType extends BaseObject<String> {

    private static final long serialVersionUID = 1L;

    private String id;
    private String identityType;
    private String description;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdentityType() {
        return identityType;
    }

    public void setIdentityType(String identityType) {
        this.identityType = identityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
