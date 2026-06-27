package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class MicroAstReadingForm {

    public String id;
    public String astRunId;
    public String antibioticId;
    public String method;
    public BigDecimal rawValue;
    public String rawText;
    public String interpretation;
    public String breakpointRuleId;
    public String overrideInterpretation;
    public String overrideReason;
    public Timestamp createdAt;
    public String createdBy;
}
