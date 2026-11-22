package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class MicroIsolateForm {

    public String id;
    public String caseId;
    public String isolateLabel;
    public String organismId;
    public String preliminaryOrganismText;
    public String gramStain;
    public String colonyMorphology;
    public String identificationMethod;
    public BigDecimal identificationConfidence;
    public String significance;
    public String identificationStatus;
    public Timestamp createdAt;
}
