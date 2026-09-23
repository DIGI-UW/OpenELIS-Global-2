package org.openelisglobal.microbiology.form;

import java.sql.Timestamp;

public class MicroReagentLotForm {
    public Long id;
    public String lotNumber;
    public Timestamp effectiveExpirationDate;
    public Timestamp openedDate;
    public Double currentQuantity;
    public String status;
    public String qcStatus;
    public boolean available;
    public String unavailableReason;
    public boolean fefoRecommended;
}
