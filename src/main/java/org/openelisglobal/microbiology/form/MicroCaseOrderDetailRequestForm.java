package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroCaseOrderDetailRequestForm {

    public String patientOrigin;
    public Integer numberOfSets;
    public String clinicalHistory;
    public String antibioticExposure;
    public String criticalNotificationPreference;
}
