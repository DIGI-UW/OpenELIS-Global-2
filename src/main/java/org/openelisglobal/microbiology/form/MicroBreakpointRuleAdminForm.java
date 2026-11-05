package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;

public class MicroBreakpointRuleAdminForm {
    public String id;
    public String standardId;
    public String organismId;
    public String organismName;
    public String organismGroup;
    public String antibioticId;
    public String antibioticName;
    public String antibioticCode;
    public String method;
    public String specimenTypeId;
    public String clinicalContext;
    public String breakpointType;
    public BigDecimal susceptibleValue;
    public BigDecimal intermediateLowerValue;
    public BigDecimal intermediateUpperValue;
    public BigDecimal resistantValue;
    public boolean resistantInclusive = true;
    public boolean configurationManaged;
    public String units;
    public String notes;
    public boolean active = true;
    public boolean seeded;
    public boolean locallyCustomized;
}
