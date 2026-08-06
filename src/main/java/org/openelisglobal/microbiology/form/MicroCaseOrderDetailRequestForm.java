package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroCaseOrderDetailRequestForm {

    @Size(max = 20)
    public String cultureMethodId;

    @Size(max = 255)
    public String patientOrigin;

    @Min(1)
    @Max(99)
    public Integer numberOfSets;

    @Size(max = 4000)
    public String clinicalHistory;

    @Size(max = 4000)
    public String antibioticExposure;

    @Size(max = 255)
    public String criticalNotificationPreference;
}
