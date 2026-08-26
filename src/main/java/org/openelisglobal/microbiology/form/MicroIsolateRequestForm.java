package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroIsolateRequestForm {
    public String caseId;
    public String isolateLabel;
    public String isolateId;
    public String organismId;
    public String preliminaryOrganismText;
    public String significance;
    public String identificationStatus;
}
