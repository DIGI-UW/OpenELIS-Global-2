package org.openelisglobal.microbiology.form;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class MicroReagentRequirementForm {
    public String analysisId;
    public String testId;
    public String testName;
    public String linkId;
    public Long reagentId;
    public String reagentName;
    public String usageType;
    public BigDecimal quantityPerTest;
    public String quantityUnit;
    public List<MicroReagentLotForm> lots = new ArrayList<>();
}
