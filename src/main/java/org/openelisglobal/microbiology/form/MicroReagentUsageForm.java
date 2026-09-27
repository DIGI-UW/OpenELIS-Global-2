package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroReagentUsageForm {
    public String id;
    public String usageContext;
    public String actionId;
    public String reagentName;
    public String lotNumber;
    public Timestamp effectiveExpirationDate;
    public Double quantityUsed;
    public String quantityUnit;
    public Timestamp usageDate;
    public Integer performedByUser;
    public String currentLotStatus;
    public String currentQcStatus;
}
