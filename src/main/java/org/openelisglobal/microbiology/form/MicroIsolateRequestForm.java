package org.openelisglobal.microbiology.form;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MicroIsolateRequestForm {
    public String caseId;
    public String isolateLabel;
    public String isolateId;
    public String organismId;
    public String preliminaryOrganismText;
    public String gramStain;
    public String colonyMorphology;
    public String identificationMethod;
    public BigDecimal identificationConfidence;
    public String significance;
    public String identificationStatus;
    public String identificationReason;
}
